package giraaff.phases.common;

import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;

import giraaff.core.common.spi.ConstantFieldProvider;
import giraaff.core.common.type.Stamp;
import giraaff.graph.Graph;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.NodeWorkList;
import giraaff.graph.spi.Canonicalizable;
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
    // @def
    private static final int MAX_ITERATION_PER_NODE = 10;

    // @field
    private boolean ___globalValueNumber = true;
    // @field
    private boolean ___canonicalizeReads = true;
    // @field
    private boolean ___simplify = true;
    // @field
    private final CanonicalizerPhase.CustomCanonicalizer ___customCanonicalizer;

    // @class CanonicalizerPhase.CustomCanonicalizer
    public abstract static class CustomCanonicalizer
    {
        public Node canonicalize(Node __node)
        {
            return __node;
        }

        @SuppressWarnings("unused")
        public void simplify(Node __node, SimplifierTool __tool)
        {
        }
    }

    // @cons CanonicalizerPhase
    public CanonicalizerPhase()
    {
        this(null);
    }

    // @cons CanonicalizerPhase
    public CanonicalizerPhase(CanonicalizerPhase.CustomCanonicalizer __customCanonicalizer)
    {
        super();
        this.___customCanonicalizer = __customCanonicalizer;
    }

    public void disableGVN()
    {
        this.___globalValueNumber = false;
    }

    public void disableReadCanonicalization()
    {
        this.___canonicalizeReads = false;
    }

    public void disableSimplification()
    {
        this.___simplify = false;
    }

    @Override
    protected void run(StructuredGraph __graph, PhaseContext __context)
    {
        new CanonicalizerPhase.CanonicalizerInstance(__context).run(__graph);
    }

    ///
    // @param newNodesMark only the {@linkplain Graph#getNewNodes(Graph.NodeMark) new nodes} specified
    //            by this mark are processed
    ///
    public void applyIncremental(StructuredGraph __graph, PhaseContext __context, Graph.NodeMark __newNodesMark)
    {
        new CanonicalizerPhase.CanonicalizerInstance(__context, __newNodesMark).apply(__graph);
    }

    ///
    // @param workingSet the initial working set of nodes on which the canonicalizer works, should
    //            be an auto-grow node bitmap
    ///
    public void applyIncremental(StructuredGraph __graph, PhaseContext __context, Iterable<? extends Node> __workingSet)
    {
        new CanonicalizerPhase.CanonicalizerInstance(__context, __workingSet).apply(__graph);
    }

    public void applyIncremental(StructuredGraph __graph, PhaseContext __context, Iterable<? extends Node> __workingSet, Graph.NodeMark __newNodesMark)
    {
        new CanonicalizerPhase.CanonicalizerInstance(__context, __workingSet, __newNodesMark).apply(__graph);
    }

    public NodeView getNodeView()
    {
        return NodeView.DEFAULT;
    }

    // @class CanonicalizerPhase.CanonicalizerInstance
    // @closure
    private final class CanonicalizerInstance extends Phase
    {
        // @field
        private final Graph.NodeMark ___newNodesMark;
        // @field
        private final PhaseContext ___context;
        // @field
        private final Iterable<? extends Node> ___initWorkingSet;

        // @field
        private NodeWorkList ___workList;
        // @field
        private CanonicalizerPhase.CanonicalizerInstance.Tool ___tool;

        // @cons CanonicalizerPhase.CanonicalizerInstance
        private CanonicalizerInstance(PhaseContext __context)
        {
            this(__context, null, null);
        }

        // @cons CanonicalizerPhase.CanonicalizerInstance
        private CanonicalizerInstance(PhaseContext __context, Iterable<? extends Node> __workingSet)
        {
            this(__context, __workingSet, null);
        }

        // @cons CanonicalizerPhase.CanonicalizerInstance
        private CanonicalizerInstance(PhaseContext __context, Graph.NodeMark __newNodesMark)
        {
            this(__context, null, __newNodesMark);
        }

        // @cons CanonicalizerPhase.CanonicalizerInstance
        private CanonicalizerInstance(PhaseContext __context, Iterable<? extends Node> __workingSet, Graph.NodeMark __newNodesMark)
        {
            super();
            this.___newNodesMark = __newNodesMark;
            this.___context = __context;
            this.___initWorkingSet = __workingSet;
        }

        @Override
        protected void run(StructuredGraph __graph)
        {
            boolean __wholeGraph = this.___newNodesMark == null || this.___newNodesMark.isStart();
            if (this.___initWorkingSet == null)
            {
                this.___workList = __graph.createIterativeNodeWorkList(__wholeGraph, MAX_ITERATION_PER_NODE);
            }
            else
            {
                this.___workList = __graph.createIterativeNodeWorkList(false, MAX_ITERATION_PER_NODE);
                this.___workList.addAll(this.___initWorkingSet);
            }
            if (!__wholeGraph)
            {
                this.___workList.addAll(__graph.getNewNodes(this.___newNodesMark));
            }
            this.___tool = new CanonicalizerPhase.CanonicalizerInstance.Tool(__graph.getAssumptions());
            processWorkSet(__graph);
        }

        @SuppressWarnings("try")
        private void processWorkSet(StructuredGraph __graph)
        {
            // @closure
            Graph.NodeEventListener listener = new Graph.NodeEventListener()
            {
                @Override
                public void nodeAdded(Node __node)
                {
                    CanonicalizerPhase.CanonicalizerInstance.this.___workList.add(__node);
                }

                @Override
                public void inputChanged(Node __node)
                {
                    CanonicalizerPhase.CanonicalizerInstance.this.___workList.add(__node);
                    if (__node instanceof Node.IndirectCanonicalization)
                    {
                        for (Node __usage : __node.usages())
                        {
                            CanonicalizerPhase.CanonicalizerInstance.this.___workList.add(__usage);
                        }
                    }
                }

                @Override
                public void usagesDroppedToZero(Node __node)
                {
                    CanonicalizerPhase.CanonicalizerInstance.this.___workList.add(__node);
                }
            };

            try (Graph.NodeEventScope __nes = __graph.trackNodeEvents(listener))
            {
                for (Node __n : this.___workList)
                {
                    boolean __changed = processNode(__n);
                }
            }
        }

        ///
        // @return true if the graph was changed.
        ///
        private boolean processNode(Node __node)
        {
            if (!__node.isAlive())
            {
                return false;
            }
            if (GraphUtil.tryKillUnused(__node))
            {
                return true;
            }
            NodeClass<?> __nodeClass = __node.getNodeClass();
            StructuredGraph __graph = (StructuredGraph) __node.graph();
            if (tryCanonicalize(__node, __nodeClass))
            {
                return true;
            }
            if (CanonicalizerPhase.this.___globalValueNumber && tryGlobalValueNumbering(__node, __nodeClass))
            {
                return true;
            }
            if (__node instanceof ValueNode)
            {
                ValueNode __valueNode = (ValueNode) __node;
                boolean __improvedStamp = tryInferStamp(__valueNode);
                Constant __constant = __valueNode.stamp(NodeView.DEFAULT).asConstant();
                if (__constant != null && !(__node instanceof ConstantNode))
                {
                    ConstantNode __stampConstant = ConstantNode.forConstant(__valueNode.stamp(NodeView.DEFAULT), __constant, this.___context.getMetaAccess(), __graph);
                    __valueNode.replaceAtUsages(InputType.Value, __stampConstant);
                    GraphUtil.tryKillUnused(__valueNode);
                    return true;
                }
                else if (__improvedStamp)
                {
                    // the improved stamp may enable additional canonicalization
                    if (tryCanonicalize(__valueNode, __nodeClass))
                    {
                        return true;
                    }
                    __valueNode.usages().forEach(this.___workList::add);
                }
            }
            return false;
        }

        public boolean tryGlobalValueNumbering(Node __node, NodeClass<?> __nodeClass)
        {
            if (__nodeClass.valueNumberable())
            {
                Node __newNode = __node.graph().findDuplicate(__node);
                if (__newNode != null)
                {
                    __node.replaceAtUsagesAndDelete(__newNode);
                    return true;
                }
            }
            return false;
        }

        public boolean tryCanonicalize(final Node __node, NodeClass<?> __nodeClass)
        {
            if (CanonicalizerPhase.this.___customCanonicalizer != null)
            {
                Node __canonical = CanonicalizerPhase.this.___customCanonicalizer.canonicalize(__node);
                if (performReplacement(__node, __canonical))
                {
                    return true;
                }
                else
                {
                    CanonicalizerPhase.this.___customCanonicalizer.simplify(__node, this.___tool);
                    if (__node.isDeleted())
                    {
                        return true;
                    }
                }
            }
            if (__nodeClass.isCanonicalizable())
            {
                Node __canonical;
                try
                {
                    __canonical = ((Canonicalizable) __node).canonical(this.___tool);
                    if (__canonical == __node && __nodeClass.isCommutative())
                    {
                        __canonical = ((Canonicalizable.BinaryCommutative<?>) __node).maybeCommuteInputs();
                    }
                }
                catch (Throwable __t)
                {
                    throw new GraalError(__t);
                }
                if (performReplacement(__node, __canonical))
                {
                    return true;
                }
            }

            if (__nodeClass.isSimplifiable() && CanonicalizerPhase.this.___simplify)
            {
                __node.simplify(this.___tool);
                return __node.isDeleted();
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

        private boolean performReplacement(final Node __node, Node __newCanonical)
        {
            if (__newCanonical == __node)
            {
                return false;
            }
            else
            {
                Node __canonical = __newCanonical;
                StructuredGraph __graph = (StructuredGraph) __node.graph();
                if (__canonical != null && !__canonical.isAlive())
                {
                    __canonical = __graph.addOrUniqueWithInputs(__canonical);
                }
                if (__node instanceof FloatingNode)
                {
                    __node.replaceAtUsages(__canonical);
                    GraphUtil.killWithUnusedFloatingInputs(__node, true);
                }
                else
                {
                    FixedNode __fixed = (FixedNode) __node;
                    if (__canonical instanceof ControlSinkNode)
                    {
                        // case 7
                        __fixed.replaceAtPredecessor(__canonical);
                        GraphUtil.killCFG(__fixed);
                        return true;
                    }
                    else
                    {
                        FixedWithNextNode __fixedWithNext = (FixedWithNextNode) __fixed;
                        // when removing a fixed node, new canonicalization opportunities for its successor may arise
                        this.___tool.addToWorkList(__fixedWithNext.next());
                        if (__canonical == null)
                        {
                            // case 3
                            __node.replaceAtUsages(null);
                            GraphUtil.removeFixedWithUnusedInputs(__fixedWithNext);
                        }
                        else if (__canonical instanceof FloatingNode)
                        {
                            // case 4
                            __graph.replaceFixedWithFloating(__fixedWithNext, (FloatingNode) __canonical);
                        }
                        else
                        {
                            if (__canonical.predecessor() == null)
                            {
                                // case 5
                                __graph.replaceFixedWithFixed(__fixedWithNext, (FixedWithNextNode) __canonical);
                            }
                            else
                            {
                                // case 6
                                __node.replaceAtUsages(__canonical);
                                GraphUtil.removeFixedWithUnusedInputs(__fixedWithNext);
                            }
                        }
                    }
                }
                return true;
            }
        }

        ///
        // Calls {@link ValueNode#inferStamp()} on the node and, if it returns true (which means
        // that the stamp has changed), re-queues the node's usages. If the stamp has changed then
        // this method also checks if the stamp now describes a constant integer value, in which
        // case the node is replaced with a constant.
        ///
        private boolean tryInferStamp(ValueNode __node)
        {
            if (__node.isAlive())
            {
                if (__node.inferStamp())
                {
                    for (Node __usage : __node.usages())
                    {
                        this.___workList.add(__usage);
                    }
                    return true;
                }
            }
            return false;
        }

        // @class CanonicalizerPhase.CanonicalizerInstance.Tool
        // @closure
        private final class Tool implements SimplifierTool, NodeView
        {
            // @field
            private final Assumptions ___assumptions;
            // @field
            private NodeView ___nodeView;

            // @cons CanonicalizerPhase.CanonicalizerInstance.Tool
            Tool(Assumptions __assumptions)
            {
                super();
                this.___assumptions = __assumptions;
                this.___nodeView = getNodeView();
            }

            @Override
            public void deleteBranch(Node __branch)
            {
                FixedNode __fixedBranch = (FixedNode) __branch;
                __fixedBranch.predecessor().replaceFirstSuccessor(__fixedBranch, null);
                GraphUtil.killCFG(__fixedBranch);
            }

            @Override
            public MetaAccessProvider getMetaAccess()
            {
                return CanonicalizerPhase.CanonicalizerInstance.this.___context.getMetaAccess();
            }

            @Override
            public ConstantReflectionProvider getConstantReflection()
            {
                return CanonicalizerPhase.CanonicalizerInstance.this.___context.getConstantReflection();
            }

            @Override
            public ConstantFieldProvider getConstantFieldProvider()
            {
                return CanonicalizerPhase.CanonicalizerInstance.this.___context.getConstantFieldProvider();
            }

            @Override
            public void addToWorkList(Node __node)
            {
                CanonicalizerPhase.CanonicalizerInstance.this.___workList.add(__node);
            }

            @Override
            public void addToWorkList(Iterable<? extends Node> __nodes)
            {
                CanonicalizerPhase.CanonicalizerInstance.this.___workList.addAll(__nodes);
            }

            @Override
            public void removeIfUnused(Node __node)
            {
                GraphUtil.tryKillUnused(__node);
            }

            @Override
            public boolean canonicalizeReads()
            {
                return CanonicalizerPhase.this.___canonicalizeReads;
            }

            @Override
            public boolean allUsagesAvailable()
            {
                return true;
            }

            @Override
            public Assumptions getAssumptions()
            {
                return this.___assumptions;
            }

            @Override
            public Integer smallestCompareWidth()
            {
                return CanonicalizerPhase.CanonicalizerInstance.this.___context.getLowerer().smallestCompareWidth();
            }

            @Override
            public Stamp stamp(ValueNode __node)
            {
                return this.___nodeView.stamp(__node);
            }
        }
    }

    public boolean getCanonicalizeReads()
    {
        return this.___canonicalizeReads;
    }
}
