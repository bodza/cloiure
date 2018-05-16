package graalvm.compiler.phases.common;

import graalvm.compiler.core.common.spi.ConstantFieldProvider;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.debug.CounterKey;
import graalvm.compiler.debug.DebugCloseable;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.graph.GraalGraphError;
import graalvm.compiler.graph.Graph;
import graalvm.compiler.graph.Graph.Mark;
import graalvm.compiler.graph.Graph.NodeEventListener;
import graalvm.compiler.graph.Graph.NodeEventScope;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.Node.IndirectCanonicalization;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.NodeWorkList;
import graalvm.compiler.graph.spi.Canonicalizable;
import graalvm.compiler.graph.spi.Canonicalizable.BinaryCommutative;
import graalvm.compiler.graph.spi.SimplifierTool;
import graalvm.compiler.nodeinfo.InputType;
import graalvm.compiler.nodes.AbstractMergeNode;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.ControlSinkNode;
import graalvm.compiler.nodes.FixedNode;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.StartNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.calc.FloatingNode;
import graalvm.compiler.nodes.util.GraphUtil;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.BasePhase;
import graalvm.compiler.phases.Phase;
import graalvm.compiler.phases.tiers.PhaseContext;

import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;

public class CanonicalizerPhase extends BasePhase<PhaseContext>
{
    private static final int MAX_ITERATION_PER_NODE = 10;
    private static final CounterKey COUNTER_CANONICALIZED_NODES = DebugContext.counter("CanonicalizedNodes");
    private static final CounterKey COUNTER_PROCESSED_NODES = DebugContext.counter("ProcessedNodes");
    private static final CounterKey COUNTER_CANONICALIZATION_CONSIDERED_NODES = DebugContext.counter("CanonicalizationConsideredNodes");
    private static final CounterKey COUNTER_INFER_STAMP_CALLED = DebugContext.counter("InferStampCalled");
    private static final CounterKey COUNTER_STAMP_CHANGED = DebugContext.counter("StampChanged");
    private static final CounterKey COUNTER_SIMPLIFICATION_CONSIDERED_NODES = DebugContext.counter("SimplificationConsideredNodes");
    private static final CounterKey COUNTER_GLOBAL_VALUE_NUMBERING_HITS = DebugContext.counter("GlobalValueNumberingHits");

    private boolean globalValueNumber = true;
    private boolean canonicalizeReads = true;
    private boolean simplify = true;
    private final CustomCanonicalizer customCanonicalizer;

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

    public CanonicalizerPhase()
    {
        this(null);
    }

    public CanonicalizerPhase(CustomCanonicalizer customCanonicalizer)
    {
        this.customCanonicalizer = customCanonicalizer;
    }

    public void disableGVN()
    {
        globalValueNumber = false;
    }

    public void disableReadCanonicalization()
    {
        canonicalizeReads = false;
    }

    public void disableSimplification()
    {
        simplify = false;
    }

    @Override
    public boolean checkContract()
    {
        /*
         * There are certain canonicalizations we make that heavily increase code size by e.g.
         * replacing a merge followed by a return of the merge's phi with returns in each
         * predecessor.
         */
        return false;
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
        applyIncremental(graph, context, newNodesMark, true);
    }

    public void applyIncremental(StructuredGraph graph, PhaseContext context, Mark newNodesMark, boolean dumpGraph)
    {
        new Instance(context, newNodesMark).apply(graph, dumpGraph);
    }

    /**
     * @param workingSet the initial working set of nodes on which the canonicalizer works, should
     *            be an auto-grow node bitmap
     */
    public void applyIncremental(StructuredGraph graph, PhaseContext context, Iterable<? extends Node> workingSet)
    {
        applyIncremental(graph, context, workingSet, true);
    }

    public void applyIncremental(StructuredGraph graph, PhaseContext context, Iterable<? extends Node> workingSet, boolean dumpGraph)
    {
        new Instance(context, workingSet).apply(graph, dumpGraph);
    }

    public void applyIncremental(StructuredGraph graph, PhaseContext context, Iterable<? extends Node> workingSet, Mark newNodesMark)
    {
        applyIncremental(graph, context, workingSet, newNodesMark, true);
    }

    public void applyIncremental(StructuredGraph graph, PhaseContext context, Iterable<? extends Node> workingSet, Mark newNodesMark, boolean dumpGraph)
    {
        new Instance(context, workingSet, newNodesMark).apply(graph, dumpGraph);
    }

    public NodeView getNodeView()
    {
        return NodeView.DEFAULT;
    }

    private final class Instance extends Phase
    {
        private final Mark newNodesMark;
        private final PhaseContext context;
        private final Iterable<? extends Node> initWorkingSet;

        private NodeWorkList workList;
        private Tool tool;
        private DebugContext debug;

        private Instance(PhaseContext context)
        {
            this(context, null, null);
        }

        private Instance(PhaseContext context, Iterable<? extends Node> workingSet)
        {
            this(context, workingSet, null);
        }

        private Instance(PhaseContext context, Mark newNodesMark)
        {
            this(context, null, newNodesMark);
        }

        private Instance(PhaseContext context, Iterable<? extends Node> workingSet, Mark newNodesMark)
        {
            this.newNodesMark = newNodesMark;
            this.context = context;
            this.initWorkingSet = workingSet;
        }

        @Override
        public boolean checkContract()
        {
            return false;
        }

        @Override
        protected void run(StructuredGraph graph)
        {
            this.debug = graph.getDebug();
            boolean wholeGraph = newNodesMark == null || newNodesMark.isStart();
            if (initWorkingSet == null)
            {
                workList = graph.createIterativeNodeWorkList(wholeGraph, MAX_ITERATION_PER_NODE);
            }
            else
            {
                workList = graph.createIterativeNodeWorkList(false, MAX_ITERATION_PER_NODE);
                workList.addAll(initWorkingSet);
            }
            if (!wholeGraph)
            {
                workList.addAll(graph.getNewNodes(newNodesMark));
            }
            tool = new Tool(graph.getAssumptions(), graph.getOptions());
            processWorkSet(graph);
        }

        @SuppressWarnings("try")
        private void processWorkSet(StructuredGraph graph)
        {
            NodeEventListener listener = new NodeEventListener()
            {
                @Override
                public void nodeAdded(Node node)
                {
                    workList.add(node);
                }

                @Override
                public void inputChanged(Node node)
                {
                    workList.add(node);
                    if (node instanceof IndirectCanonicalization)
                    {
                        for (Node usage : node.usages())
                        {
                            workList.add(usage);
                        }
                    }
                }

                @Override
                public void usagesDroppedToZero(Node node)
                {
                    workList.add(node);
                }
            };

            try (NodeEventScope nes = graph.trackNodeEvents(listener))
            {
                for (Node n : workList)
                {
                    boolean changed = processNode(n);
                    if (changed && debug.isDumpEnabled(DebugContext.DETAILED_LEVEL))
                    {
                        debug.dump(DebugContext.DETAILED_LEVEL, graph, "CanonicalizerPhase %s", n);
                    }
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
            COUNTER_PROCESSED_NODES.increment(debug);
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
            if (globalValueNumber && tryGlobalValueNumbering(node, nodeClass))
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
                    ConstantNode stampConstant = ConstantNode.forConstant(valueNode.stamp(NodeView.DEFAULT), constant, context.getMetaAccess(), graph);
                    debug.log("Canonicalizer: constant stamp replaces %1s with %1s", valueNode, stampConstant);
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
                    valueNode.usages().forEach(workList::add);
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
                    assert !(node instanceof FixedNode || newNode instanceof FixedNode);
                    node.replaceAtUsagesAndDelete(newNode);
                    COUNTER_GLOBAL_VALUE_NUMBERING_HITS.increment(debug);
                    debug.log("GVN applied and new node is %1s", newNode);
                    return true;
                }
            }
            return false;
        }

        private AutoCloseable getCanonicalizeableContractAssertion(Node node)
        {
            boolean needsAssertion = false;
            assert (needsAssertion = true) == true;
            if (needsAssertion)
            {
                Mark mark = node.graph().getMark();
                return () ->
                {
                    assert mark.equals(node.graph().getMark()) : "new node created while canonicalizing " + node.getClass().getSimpleName() + " " + node + ": " + node.graph().getNewNodes(mark).snapshot();
                };
            }
            else
            {
                return null;
            }
        }

        @SuppressWarnings("try")
        public boolean tryCanonicalize(final Node node, NodeClass<?> nodeClass)
        {
            try (DebugCloseable position = node.withNodeSourcePosition(); DebugContext.Scope scope = debug.withContext(node))
            {
                if (customCanonicalizer != null)
                {
                    Node canonical = customCanonicalizer.canonicalize(node);
                    if (performReplacement(node, canonical))
                    {
                        return true;
                    }
                    else
                    {
                        customCanonicalizer.simplify(node, tool);
                        if (node.isDeleted())
                        {
                            return true;
                        }
                    }
                }
                if (nodeClass.isCanonicalizable())
                {
                    COUNTER_CANONICALIZATION_CONSIDERED_NODES.increment(debug);
                    Node canonical;
                    try (AutoCloseable verify = getCanonicalizeableContractAssertion(node))
                    {
                        canonical = ((Canonicalizable) node).canonical(tool);
                        if (canonical == node && nodeClass.isCommutative())
                        {
                            canonical = ((BinaryCommutative<?>) node).maybeCommuteInputs();
                        }
                    }
                    catch (Throwable e)
                    {
                        throw new GraalGraphError(e).addContext(node);
                    }
                    if (performReplacement(node, canonical))
                    {
                        return true;
                    }
                }

                if (nodeClass.isSimplifiable() && simplify)
                {
                    debug.log(DebugContext.VERBOSE_LEVEL, "Canonicalizer: simplifying %s", node);
                    COUNTER_SIMPLIFICATION_CONSIDERED_NODES.increment(debug);
                    node.simplify(tool);
                    return node.isDeleted();
                }
                return false;
            }
            catch (Throwable throwable)
            {
                throw debug.handle(throwable);
            }
        }

//     cases:                                           original node:
//                                         |Floating|Fixed-unconnected|Fixed-connected|
//                                         --------------------------------------------
//                                     null|   1    |        X        |       3       |
//                                         --------------------------------------------
//                                 Floating|   2    |        X        |       4       |
//       canonical node:                   --------------------------------------------
//                        Fixed-unconnected|   X    |        X        |       5       |
//                                         --------------------------------------------
//                          Fixed-connected|   2    |        X        |       6       |
//                                         --------------------------------------------
//                              ControlSink|   X    |        X        |       7       |
//                                         --------------------------------------------
//       X: must not happen (checked with assertions)

        private boolean performReplacement(final Node node, Node newCanonical)
        {
            if (newCanonical == node)
            {
                debug.log(DebugContext.VERBOSE_LEVEL, "Canonicalizer: work on %1s", node);
                return false;
            }
            else
            {
                Node canonical = newCanonical;
                debug.log("Canonicalizer: replacing %1s with %1s", node, canonical);
                COUNTER_CANONICALIZED_NODES.increment(debug);
                StructuredGraph graph = (StructuredGraph) node.graph();
                if (canonical != null && !canonical.isAlive())
                {
                    assert !canonical.isDeleted();
                    canonical = graph.addOrUniqueWithInputs(canonical);
                }
                if (node instanceof FloatingNode)
                {
                    assert canonical == null || !(canonical instanceof FixedNode) || (canonical.predecessor() != null || canonical instanceof StartNode || canonical instanceof AbstractMergeNode) : node + " -> " + canonical + " : replacement should be floating or fixed and connected";
                    node.replaceAtUsages(canonical);
                    GraphUtil.killWithUnusedFloatingInputs(node, true);
                }
                else
                {
                    assert node instanceof FixedNode && node.predecessor() != null : node + " -> " + canonical + " : node should be fixed & connected (" + node.predecessor() + ")";
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
                        assert fixed instanceof FixedWithNextNode;
                        FixedWithNextNode fixedWithNext = (FixedWithNextNode) fixed;
                        // When removing a fixed node, new canonicalization
                        // opportunities for its successor may arise
                        assert fixedWithNext.next() != null;
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
                            assert canonical instanceof FixedNode;
                            if (canonical.predecessor() == null)
                            {
                                assert !canonical.cfgSuccessors().iterator().hasNext() : "replacement " + canonical + " shouldn't have successors";
                                // case 5
                                graph.replaceFixedWithFixed(fixedWithNext, (FixedWithNextNode) canonical);
                            }
                            else
                            {
                                assert canonical.cfgSuccessors().iterator().hasNext() : "replacement " + canonical + " should have successors";
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
                COUNTER_INFER_STAMP_CALLED.increment(debug);
                if (node.inferStamp())
                {
                    COUNTER_STAMP_CHANGED.increment(debug);
                    for (Node usage : node.usages())
                    {
                        workList.add(usage);
                    }
                    return true;
                }
            }
            return false;
        }

        private final class Tool implements SimplifierTool, NodeView
        {
            private final Assumptions assumptions;
            private final OptionValues options;
            private NodeView nodeView;

            Tool(Assumptions assumptions, OptionValues options)
            {
                this.assumptions = assumptions;
                this.options = options;
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
                return context.getMetaAccess();
            }

            @Override
            public ConstantReflectionProvider getConstantReflection()
            {
                return context.getConstantReflection();
            }

            @Override
            public ConstantFieldProvider getConstantFieldProvider()
            {
                return context.getConstantFieldProvider();
            }

            @Override
            public void addToWorkList(Node node)
            {
                workList.add(node);
            }

            @Override
            public void addToWorkList(Iterable<? extends Node> nodes)
            {
                workList.addAll(nodes);
            }

            @Override
            public void removeIfUnused(Node node)
            {
                GraphUtil.tryKillUnused(node);
            }

            @Override
            public boolean canonicalizeReads()
            {
                return canonicalizeReads;
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
                return context.getLowerer().smallestCompareWidth();
            }

            @Override
            public OptionValues getOptions()
            {
                return options;
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
        return canonicalizeReads;
    }
}
