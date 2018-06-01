package giraaff.phases.common;

import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;

import giraaff.core.common.spi.ConstantFieldProvider;
import giraaff.core.common.type.Stamp;
import giraaff.graph.Graph;
import giraaff.graph.Graph.Mark;
import giraaff.graph.Graph.NodeEventListener;
import giraaff.graph.Graph.NodeEventScope;
import giraaff.graph.Node;
import giraaff.graph.Node.IndirectCanonicalization;
import giraaff.graph.NodeClass;
import giraaff.graph.NodeWorkList;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.Canonicalizable.BinaryCommutative;
import giraaff.graph.spi.SimplifierTool;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.ControlSinkNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.util.GraphUtil;
import giraaff.phases.BasePhase;
import giraaff.phases.Phase;
import giraaff.phases.tiers.PhaseContext;
import giraaff.util.GraalError;

// @class CanonicalizerPhase
public final class CanonicalizerPhase extends BasePhase<PhaseContext>
{
    private static final int MAX_ITERATION_PER_NODE = 10;

    private boolean globalValueNumber = true;
    private boolean canonicalizeReads = true;
    private boolean simplify = true;
    private final CustomCanonicalizer customCanonicalizer;

    // @class CanonicalizerPhase.CustomCanonicalizer
    public abstract static class CustomCanonicalizer
    {
        public Node canonicalize(Node node)
        {
            return node;
        }

        @SuppressWarnings("unused")
        public void simplify(Node node, SimplifierTool tool)
        {
        }
    }

    // @cons
    public CanonicalizerPhase()
    {
        this(null);
    }

    // @cons
    public CanonicalizerPhase(CustomCanonicalizer customCanonicalizer)
    {
        super();
        this.customCanonicalizer = customCanonicalizer;
    }

    public void disableGVN()
    {
        this.globalValueNumber = false;
    }

    public void disableReadCanonicalization()
    {
        this.canonicalizeReads = false;
    }

    public void disableSimplification()
    {
        this.simplify = false;
    }

    @Override
    protected void run(StructuredGraph graph, PhaseContext context)
    {
        new Instance(context).run(graph);
    }

    /**
     * @param newNodesMark only the {@linkplain Graph#getNewNodes(Mark) new nodes} specified by this
     *            mark are processed
     */
    public void applyIncremental(StructuredGraph graph, PhaseContext context, Mark newNodesMark)
    {
        new Instance(context, newNodesMark).apply(graph);
    }

    /**
     * @param workingSet the initial working set of nodes on which the canonicalizer works, should
     *            be an auto-grow node bitmap
     */
    public void applyIncremental(StructuredGraph graph, PhaseContext context, Iterable<? extends Node> workingSet)
    {
        new Instance(context, workingSet).apply(graph);
    }

    public void applyIncremental(StructuredGraph graph, PhaseContext context, Iterable<? extends Node> workingSet, Mark newNodesMark)
    {
        new Instance(context, workingSet, newNodesMark).apply(graph);
    }

    public NodeView getNodeView()
    {
        return NodeView.DEFAULT;
    }

    // @class CanonicalizerPhase.Instance
    // @closure
    private final class Instance extends Phase
    {
        private final Mark newNodesMark;
        private final PhaseContext context;
        private final Iterable<? extends Node> initWorkingSet;

        private NodeWorkList workList;
        private Tool tool;

        // @cons
        private Instance(PhaseContext context)
        {
            this(context, null, null);
        }

        // @cons
        private Instance(PhaseContext context, Iterable<? extends Node> workingSet)
        {
            this(context, workingSet, null);
        }

        // @cons
        private Instance(PhaseContext context, Mark newNodesMark)
        {
            this(context, null, newNodesMark);
        }

        // @cons
        private Instance(PhaseContext context, Iterable<? extends Node> workingSet, Mark newNodesMark)
        {
            super();
            this.newNodesMark = newNodesMark;
            this.context = context;
            this.initWorkingSet = workingSet;
        }

        @Override
        protected void run(StructuredGraph graph)
        {
            boolean wholeGraph = newNodesMark == null || newNodesMark.isStart();
            if (initWorkingSet == null)
            {
                this.workList = graph.createIterativeNodeWorkList(wholeGraph, MAX_ITERATION_PER_NODE);
            }
            else
            {
                this.workList = graph.createIterativeNodeWorkList(false, MAX_ITERATION_PER_NODE);
                this.workList.addAll(initWorkingSet);
            }
            if (!wholeGraph)
            {
                this.workList.addAll(graph.getNewNodes(newNodesMark));
            }
            tool = new Tool(graph.getAssumptions());
            processWorkSet(graph);
        }

        @SuppressWarnings("try")
        private void processWorkSet(StructuredGraph graph)
        {
            // @closure
            NodeEventListener listener = new NodeEventListener()
            {
                @Override
                public void nodeAdded(Node node)
                {
                    CanonicalizerPhase.Instance.this.workList.add(node);
                }

                @Override
                public void inputChanged(Node node)
                {
                    CanonicalizerPhase.Instance.this.workList.add(node);
                    if (node instanceof IndirectCanonicalization)
                    {
                        for (Node usage : node.usages())
                        {
                            CanonicalizerPhase.Instance.this.workList.add(usage);
                        }
                    }
                }

                @Override
                public void usagesDroppedToZero(Node node)
                {
                    CanonicalizerPhase.Instance.this.workList.add(node);
                }
            };

            try (NodeEventScope nes = graph.trackNodeEvents(listener))
            {
                for (Node n : this.workList)
                {
                    boolean changed = processNode(n);
                }
            }
        }

        /**
         * @return true if the graph was changed.
         */
        private boolean processNode(Node node)
        {
            if (!node.isAlive())
            {
                return false;
            }
            if (GraphUtil.tryKillUnused(node))
            {
                return true;
            }
            NodeClass<?> nodeClass = node.getNodeClass();
            StructuredGraph graph = (StructuredGraph) node.graph();
            if (tryCanonicalize(node, nodeClass))
            {
                return true;
            }
            if (CanonicalizerPhase.this.globalValueNumber && tryGlobalValueNumbering(node, nodeClass))
            {
                return true;
            }
            if (node instanceof ValueNode)
            {
                ValueNode valueNode = (ValueNode) node;
                boolean improvedStamp = tryInferStamp(valueNode);
                Constant constant = valueNode.stamp(NodeView.DEFAULT).asConstant();
                if (constant != null && !(node instanceof ConstantNode))
                {
                    ConstantNode stampConstant = ConstantNode.forConstant(valueNode.stamp(NodeView.DEFAULT), constant, this.context.getMetaAccess(), graph);
                    valueNode.replaceAtUsages(InputType.Value, stampConstant);
                    GraphUtil.tryKillUnused(valueNode);
                    return true;
                }
                else if (improvedStamp)
                {
                    // the improved stamp may enable additional canonicalization
                    if (tryCanonicalize(valueNode, nodeClass))
                    {
                        return true;
                    }
                    valueNode.usages().forEach(this.workList::add);
                }
            }
            return false;
        }

        public boolean tryGlobalValueNumbering(Node node, NodeClass<?> nodeClass)
        {
            if (nodeClass.valueNumberable())
            {
                Node newNode = node.graph().findDuplicate(node);
                if (newNode != null)
                {
                    node.replaceAtUsagesAndDelete(newNode);
                    return true;
                }
            }
            return false;
        }

        public boolean tryCanonicalize(final Node node, NodeClass<?> nodeClass)
        {
            if (CanonicalizerPhase.this.customCanonicalizer != null)
            {
                Node canonical = CanonicalizerPhase.this.customCanonicalizer.canonicalize(node);
                if (performReplacement(node, canonical))
                {
                    return true;
                }
                else
                {
                    CanonicalizerPhase.this.customCanonicalizer.simplify(node, tool);
                    if (node.isDeleted())
                    {
                        return true;
                    }
                }
            }
            if (nodeClass.isCanonicalizable())
            {
                Node canonical;
                try
                {
                    canonical = ((Canonicalizable) node).canonical(tool);
                    if (canonical == node && nodeClass.isCommutative())
                    {
                        canonical = ((BinaryCommutative<?>) node).maybeCommuteInputs();
                    }
                }
                catch (Throwable t)
                {
                    throw new GraalError(t);
                }
                if (performReplacement(node, canonical))
                {
                    return true;
                }
            }

            if (nodeClass.isSimplifiable() && CanonicalizerPhase.this.simplify)
            {
                node.simplify(tool);
                return node.isDeleted();
            }
            return false;
        }

        // cases:                                           original node:
        //                                     |Floating|Fixed-unconnected|Fixed-connected|
        //                                     --------------------------------------------
        //                                 null|   1    |        X        |       3       |
        //                                     --------------------------------------------
        //                             Floating|   2    |        X        |       4       |
        //   canonical node:                   --------------------------------------------
        //                    Fixed-unconnected|   X    |        X        |       5       |
        //                                     --------------------------------------------
        //                      Fixed-connected|   2    |        X        |       6       |
        //                                     --------------------------------------------
        //                          ControlSink|   X    |        X        |       7       |
        //                                     --------------------------------------------
        //   X: must not happen (checked with assertions)

        private boolean performReplacement(final Node node, Node newCanonical)
        {
            if (newCanonical == node)
            {
                return false;
            }
            else
            {
                Node canonical = newCanonical;
                StructuredGraph graph = (StructuredGraph) node.graph();
                if (canonical != null && !canonical.isAlive())
                {
                    canonical = graph.addOrUniqueWithInputs(canonical);
                }
                if (node instanceof FloatingNode)
                {
                    node.replaceAtUsages(canonical);
                    GraphUtil.killWithUnusedFloatingInputs(node, true);
                }
                else
                {
                    FixedNode fixed = (FixedNode) node;
                    if (canonical instanceof ControlSinkNode)
                    {
                        // case 7
                        fixed.replaceAtPredecessor(canonical);
                        GraphUtil.killCFG(fixed);
                        return true;
                    }
                    else
                    {
                        FixedWithNextNode fixedWithNext = (FixedWithNextNode) fixed;
                        // when removing a fixed node, new canonicalization opportunities for its successor may arise
                        tool.addToWorkList(fixedWithNext.next());
                        if (canonical == null)
                        {
                            // case 3
                            node.replaceAtUsages(null);
                            GraphUtil.removeFixedWithUnusedInputs(fixedWithNext);
                        }
                        else if (canonical instanceof FloatingNode)
                        {
                            // case 4
                            graph.replaceFixedWithFloating(fixedWithNext, (FloatingNode) canonical);
                        }
                        else
                        {
                            if (canonical.predecessor() == null)
                            {
                                // case 5
                                graph.replaceFixedWithFixed(fixedWithNext, (FixedWithNextNode) canonical);
                            }
                            else
                            {
                                // case 6
                                node.replaceAtUsages(canonical);
                                GraphUtil.removeFixedWithUnusedInputs(fixedWithNext);
                            }
                        }
                    }
                }
                return true;
            }
        }

        /**
         * Calls {@link ValueNode#inferStamp()} on the node and, if it returns true (which means
         * that the stamp has changed), re-queues the node's usages. If the stamp has changed then
         * this method also checks if the stamp now describes a constant integer value, in which
         * case the node is replaced with a constant.
         */
        private boolean tryInferStamp(ValueNode node)
        {
            if (node.isAlive())
            {
                if (node.inferStamp())
                {
                    for (Node usage : node.usages())
                    {
                        this.workList.add(usage);
                    }
                    return true;
                }
            }
            return false;
        }

        // @class CanonicalizerPhase.Instance.Tool
        // @closure
        private final class Tool implements SimplifierTool, NodeView
        {
            private final Assumptions assumptions;
            private NodeView nodeView;

            // @cons
            Tool(Assumptions assumptions)
            {
                super();
                this.assumptions = assumptions;
                this.nodeView = getNodeView();
            }

            @Override
            public void deleteBranch(Node branch)
            {
                FixedNode fixedBranch = (FixedNode) branch;
                fixedBranch.predecessor().replaceFirstSuccessor(fixedBranch, null);
                GraphUtil.killCFG(fixedBranch);
            }

            @Override
            public MetaAccessProvider getMetaAccess()
            {
                return CanonicalizerPhase.Instance.this.context.getMetaAccess();
            }

            @Override
            public ConstantReflectionProvider getConstantReflection()
            {
                return CanonicalizerPhase.Instance.this.context.getConstantReflection();
            }

            @Override
            public ConstantFieldProvider getConstantFieldProvider()
            {
                return CanonicalizerPhase.Instance.this.context.getConstantFieldProvider();
            }

            @Override
            public void addToWorkList(Node node)
            {
                CanonicalizerPhase.Instance.this.workList.add(node);
            }

            @Override
            public void addToWorkList(Iterable<? extends Node> nodes)
            {
                CanonicalizerPhase.Instance.this.workList.addAll(nodes);
            }

            @Override
            public void removeIfUnused(Node node)
            {
                GraphUtil.tryKillUnused(node);
            }

            @Override
            public boolean canonicalizeReads()
            {
                return CanonicalizerPhase.this.canonicalizeReads;
            }

            @Override
            public boolean allUsagesAvailable()
            {
                return true;
            }

            @Override
            public Assumptions getAssumptions()
            {
                return assumptions;
            }

            @Override
            public Integer smallestCompareWidth()
            {
                return CanonicalizerPhase.Instance.this.context.getLowerer().smallestCompareWidth();
            }

            @Override
            public Stamp stamp(ValueNode node)
            {
                return nodeView.stamp(node);
            }
        }
    }

    public boolean getCanonicalizeReads()
    {
        return this.canonicalizeReads;
    }
}
