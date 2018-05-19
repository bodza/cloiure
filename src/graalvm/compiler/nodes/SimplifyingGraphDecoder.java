package graalvm.compiler.nodes;

import static graalvm.compiler.nodeinfo.InputType.Guard;
import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_IGNORED;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_IGNORED;

import java.util.List;

import graalvm.compiler.core.common.spi.ConstantFieldProvider;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.debug.DebugCloseable;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.calc.FloatingNode;
import graalvm.compiler.nodes.extended.GuardingNode;
import graalvm.compiler.nodes.extended.IntegerSwitchNode;
import graalvm.compiler.nodes.java.ArrayLengthNode;
import graalvm.compiler.nodes.java.LoadFieldNode;
import graalvm.compiler.nodes.java.LoadIndexedNode;
import graalvm.compiler.nodes.spi.StampProvider;
import graalvm.compiler.nodes.util.GraphUtil;
import graalvm.compiler.options.OptionValues;

import jdk.vm.ci.code.Architecture;
import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;

/**
 * Graph decoder that simplifies nodes during decoding. The standard
 * {@link Canonicalizable#canonical node canonicalization} interface is used to canonicalize nodes
 * during decoding. Additionally, {@link IfNode branches} and {@link IntegerSwitchNode switches}
 * with constant conditions are simplified.
 */
public class SimplifyingGraphDecoder extends GraphDecoder
{
    protected final MetaAccessProvider metaAccess;
    protected final ConstantReflectionProvider constantReflection;
    protected final ConstantFieldProvider constantFieldProvider;
    protected final StampProvider stampProvider;
    protected final boolean canonicalizeReads;
    protected final CanonicalizerTool canonicalizerTool;

    protected class PECanonicalizerTool implements CanonicalizerTool
    {
        private final Assumptions assumptions;
        private final OptionValues options;

        public PECanonicalizerTool(Assumptions assumptions, OptionValues options)
        {
            this.assumptions = assumptions;
            this.options = options;
        }

        @Override
        public OptionValues getOptions()
        {
            return options;
        }

        @Override
        public MetaAccessProvider getMetaAccess()
        {
            return metaAccess;
        }

        @Override
        public ConstantReflectionProvider getConstantReflection()
        {
            return constantReflection;
        }

        @Override
        public ConstantFieldProvider getConstantFieldProvider()
        {
            return constantFieldProvider;
        }

        @Override
        public boolean canonicalizeReads()
        {
            return canonicalizeReads;
        }

        @Override
        public boolean allUsagesAvailable()
        {
            return false;
        }

        @Override
        public Assumptions getAssumptions()
        {
            return assumptions;
        }

        @Override
        public Integer smallestCompareWidth()
        {
            // to be safe, just report null here
            // there will be more opportunities for this optimization later
            return null;
        }
    }

    @NodeInfo(cycles = CYCLES_IGNORED, size = SIZE_IGNORED, allowedUsageTypes = {Guard})
    static class CanonicalizeToNullNode extends FloatingNode implements Canonicalizable, GuardingNode
    {
        public static final NodeClass<CanonicalizeToNullNode> TYPE = NodeClass.create(CanonicalizeToNullNode.class);

        protected CanonicalizeToNullNode(Stamp stamp)
        {
            super(TYPE, stamp);
        }

        @Override
        public Node canonical(CanonicalizerTool tool)
        {
            return null;
        }
    }

    public SimplifyingGraphDecoder(Architecture architecture, StructuredGraph graph, MetaAccessProvider metaAccess, ConstantReflectionProvider constantReflection, ConstantFieldProvider constantFieldProvider, StampProvider stampProvider, boolean canonicalizeReads)
    {
        super(architecture, graph);
        this.metaAccess = metaAccess;
        this.constantReflection = constantReflection;
        this.constantFieldProvider = constantFieldProvider;
        this.stampProvider = stampProvider;
        this.canonicalizeReads = canonicalizeReads;
        this.canonicalizerTool = new PECanonicalizerTool(graph.getAssumptions(), graph.getOptions());
    }

    @Override
    protected void cleanupGraph(MethodScope methodScope)
    {
        GraphUtil.normalizeLoops(graph);
        super.cleanupGraph(methodScope);

        for (Node node : graph.getNewNodes(methodScope.methodStartMark))
        {
            if (node instanceof MergeNode)
            {
                MergeNode mergeNode = (MergeNode) node;
                if (mergeNode.forwardEndCount() == 1)
                {
                    graph.reduceTrivialMerge(mergeNode);
                }
            }
            else if (node instanceof BeginNode || node instanceof KillingBeginNode)
            {
                if (!(node.predecessor() instanceof ControlSplitNode) && node.hasNoUsages())
                {
                    GraphUtil.unlinkFixedNode((AbstractBeginNode) node);
                    node.safeDelete();
                }
            }
        }

        for (Node node : graph.getNewNodes(methodScope.methodStartMark))
        {
            GraphUtil.tryKillUnused(node);
        }
    }

    @Override
    protected boolean allowLazyPhis()
    {
        /*
         * We do not need to exactly reproduce the encoded graph, so we want to avoid unnecessary
         * phi functions.
         */
        return true;
    }

    @Override
    protected void handleMergeNode(MergeNode merge)
    {
        /*
         * All inputs of non-loop phi nodes are known by now. We can infer the stamp for the phi, so
         * that parsing continues with more precise type information.
         */
        for (ValuePhiNode phi : merge.valuePhis())
        {
            phi.inferStamp();
        }
    }

    @Override
    protected void handleFixedNode(MethodScope methodScope, LoopScope loopScope, int nodeOrderId, FixedNode node)
    {
        Node canonical = canonicalizeFixedNode(methodScope, node);
        if (canonical != node)
        {
            handleCanonicalization(loopScope, nodeOrderId, node, canonical);
        }
    }

    /**
     * Canonicalizes the provided node, which was originally a {@link FixedNode} but can already be
     * canonicalized (and therefore be a non-fixed node).
     *
     * @param methodScope The current method.
     * @param node The node to be canonicalized.
     */
    protected Node canonicalizeFixedNode(MethodScope methodScope, Node node)
    {
        if (node instanceof LoadFieldNode)
        {
            LoadFieldNode loadFieldNode = (LoadFieldNode) node;
            return loadFieldNode.canonical(canonicalizerTool);
        }
        else if (node instanceof FixedGuardNode)
        {
            FixedGuardNode guard = (FixedGuardNode) node;
            if (guard.getCondition() instanceof LogicConstantNode)
            {
                LogicConstantNode condition = (LogicConstantNode) guard.getCondition();
                if (condition.getValue() == guard.isNegated())
                {
                    DeoptimizeNode deopt = new DeoptimizeNode(guard.getAction(), guard.getReason(), guard.getSpeculation());
                    if (guard.stateBefore() != null)
                    {
                        deopt.setStateBefore(guard.stateBefore());
                    }
                    return deopt;
                }
                else
                {
                    return null;
                }
            }
            return node;
        }
        else if (node instanceof IfNode)
        {
            IfNode ifNode = (IfNode) node;
            if (ifNode.condition() instanceof LogicNegationNode)
            {
                ifNode.eliminateNegation();
            }
            if (ifNode.condition() instanceof LogicConstantNode)
            {
                boolean condition = ((LogicConstantNode) ifNode.condition()).getValue();
                AbstractBeginNode survivingSuccessor = ifNode.getSuccessor(condition);
                AbstractBeginNode deadSuccessor = ifNode.getSuccessor(!condition);

                graph.removeSplit(ifNode, survivingSuccessor);
                deadSuccessor.safeDelete();
            }
            return node;
        }
        else if (node instanceof LoadIndexedNode)
        {
            LoadIndexedNode loadIndexedNode = (LoadIndexedNode) node;
            return loadIndexedNode.canonical(canonicalizerTool);
        }
        else if (node instanceof ArrayLengthNode)
        {
            ArrayLengthNode arrayLengthNode = (ArrayLengthNode) node;
            return arrayLengthNode.canonical(canonicalizerTool);
        }
        else if (node instanceof IntegerSwitchNode && ((IntegerSwitchNode) node).value().isConstant())
        {
            IntegerSwitchNode switchNode = (IntegerSwitchNode) node;
            int value = switchNode.value().asJavaConstant().asInt();
            AbstractBeginNode survivingSuccessor = switchNode.successorAtKey(value);
            List<Node> allSuccessors = switchNode.successors().snapshot();

            graph.removeSplit(switchNode, survivingSuccessor);
            for (Node successor : allSuccessors)
            {
                if (successor != survivingSuccessor)
                {
                    successor.safeDelete();
                }
            }
            return node;
        }
        else if (node instanceof Canonicalizable)
        {
            return ((Canonicalizable) node).canonical(canonicalizerTool);
        }
        else
        {
            return node;
        }
    }

    private static Node canonicalizeFixedNodeToNull(FixedNode node)
    {
        /*
         * When a node is unnecessary, we must not remove it right away because there might be nodes
         * that use it as a guard input. Therefore, we replace it with a more lightweight node
         * (which is floating and has no inputs).
         */
        return new CanonicalizeToNullNode(node.stamp);
    }

    @SuppressWarnings("try")
    private void handleCanonicalization(LoopScope loopScope, int nodeOrderId, FixedNode node, Node c)
    {
        try (DebugCloseable position = graph.withNodeSourcePosition(node))
        {
            Node canonical = c == null ? canonicalizeFixedNodeToNull(node) : c;
            if (!canonical.isAlive())
            {
                canonical = graph.addOrUniqueWithInputs(canonical);
                if (canonical instanceof FixedWithNextNode)
                {
                    graph.addBeforeFixed(node, (FixedWithNextNode) canonical);
                }
                else if (canonical instanceof ControlSinkNode)
                {
                    FixedWithNextNode predecessor = (FixedWithNextNode) node.predecessor();
                    predecessor.setNext((ControlSinkNode) canonical);
                    List<Node> successorSnapshot = node.successors().snapshot();
                    node.safeDelete();
                    for (Node successor : successorSnapshot)
                    {
                        successor.safeDelete();
                    }
                }
            }
            if (!node.isDeleted())
            {
                GraphUtil.unlinkFixedNode((FixedWithNextNode) node);
                node.replaceAtUsagesAndDelete(canonical);
            }
            registerNode(loopScope, nodeOrderId, canonical, true, false);
        }
    }

    @Override
    @SuppressWarnings("try")
    protected Node handleFloatingNodeBeforeAdd(MethodScope methodScope, LoopScope loopScope, Node node)
    {
        if (node instanceof ValueNode)
        {
            ((ValueNode) node).inferStamp();
        }
        if (node instanceof Canonicalizable)
        {
            try (DebugCloseable context = graph.withNodeSourcePosition(node))
            {
                Node canonical = ((Canonicalizable) node).canonical(canonicalizerTool);
                if (canonical == null)
                {
                    /*
                     * This is a possible return value of canonicalization. However, we might need
                     * to add additional usages later on for which we need a node. Therefore, we
                     * just do nothing and leave the node in place.
                     */
                }
                else if (canonical != node)
                {
                    if (!canonical.isAlive())
                    {
                        canonical = graph.addOrUniqueWithInputs(canonical);
                    }
                    return canonical;
                }
            }
        }
        return node;
    }

    @Override
    protected Node addFloatingNode(MethodScope methodScope, Node node)
    {
        /*
         * In contrast to the base class implementation, we do not need to exactly reproduce the
         * encoded graph. Since we do canonicalization, we also want nodes to be unique.
         */
        return graph.addOrUnique(node);
    }
}
