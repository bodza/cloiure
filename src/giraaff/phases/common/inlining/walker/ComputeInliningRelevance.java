package giraaff.phases.common.inlining.walker;

import java.util.ArrayList;
import java.util.function.ToDoubleFunction;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;

import giraaff.graph.Node;
import giraaff.graph.NodeWorkList;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.ControlSinkNode;
import giraaff.nodes.ControlSplitNode;
import giraaff.nodes.EndNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.Invoke;
import giraaff.nodes.LoopBeginNode;
import giraaff.nodes.LoopEndNode;
import giraaff.nodes.LoopExitNode;
import giraaff.nodes.StartNode;
import giraaff.nodes.StructuredGraph;
import giraaff.phases.common.inlining.InliningUtil;

// @class ComputeInliningRelevance
public final class ComputeInliningRelevance
{
    // @def
    private static final double EPSILON = 1d / Integer.MAX_VALUE;
    // @def
    private static final double UNINITIALIZED = -1D;

    // @def
    private static final int EXPECTED_MIN_INVOKE_COUNT = 3;
    // @def
    private static final int EXPECTED_INVOKE_RATIO = 20;
    // @def
    private static final int EXPECTED_LOOP_COUNT = 3;

    // @field
    private final StructuredGraph graph;
    // @field
    private final ToDoubleFunction<FixedNode> nodeProbabilities;

    /**
     * Node relevances are pre-computed for all invokes if the graph contains loops. If there are no
     * loops, the computation happens lazily based on {@link #rootScope}.
     */
    // @field
    private EconomicMap<FixedNode, Double> nodeRelevances;
    /**
     * This scope is non-null if (and only if) there are no loops in the graph. In this case, the
     * root scope is used to compute invoke relevances on the fly.
     */
    // @field
    private Scope rootScope;

    // @cons
    public ComputeInliningRelevance(StructuredGraph __graph, ToDoubleFunction<FixedNode> __nodeProbabilities)
    {
        super();
        this.graph = __graph;
        this.nodeProbabilities = __nodeProbabilities;
    }

    /**
     * Initializes or updates the relevance computation. If there are no loops within the graph,
     * most computation happens lazily.
     */
    public void compute()
    {
        rootScope = null;
        if (!graph.hasLoops())
        {
            // fast path for the frequent case of no loops
            rootScope = new Scope(graph.start(), null);
        }
        else
        {
            if (nodeRelevances == null)
            {
                nodeRelevances = EconomicMap.create(Equivalence.IDENTITY, EXPECTED_MIN_INVOKE_COUNT + InliningUtil.getNodeCount(graph) / EXPECTED_INVOKE_RATIO);
            }
            NodeWorkList __workList = graph.createNodeWorkList();
            EconomicMap<LoopBeginNode, Scope> __loops = EconomicMap.create(Equivalence.IDENTITY, EXPECTED_LOOP_COUNT);

            Scope __topScope = new Scope(graph.start(), null);
            for (LoopBeginNode __loopBegin : graph.getNodes(LoopBeginNode.TYPE))
            {
                createLoopScope(__loopBegin, __loops, __topScope);
            }

            __topScope.process(__workList);
            for (Scope __scope : __loops.getValues())
            {
                __scope.process(__workList);
            }
        }
    }

    public double getRelevance(Invoke __invoke)
    {
        if (rootScope != null)
        {
            return rootScope.computeInvokeRelevance(__invoke);
        }
        return nodeRelevances.get(__invoke.asNode());
    }

    /**
     * Determines the parent of the given loop and creates a {@link Scope} object for each one. This
     * method will call itself recursively if no {@link Scope} for the parent loop exists.
     */
    private Scope createLoopScope(LoopBeginNode __loopBegin, EconomicMap<LoopBeginNode, Scope> __loops, Scope __topScope)
    {
        Scope __scope = __loops.get(__loopBegin);
        if (__scope == null)
        {
            final Scope __parent;
            // look for the parent scope
            FixedNode __current = __loopBegin.forwardEnd();
            while (true)
            {
                if (__current.predecessor() == null)
                {
                    if (__current instanceof LoopBeginNode)
                    {
                        // if we reach a LoopBeginNode then we're within this loop
                        __parent = createLoopScope((LoopBeginNode) __current, __loops, __topScope);
                        break;
                    }
                    else if (__current instanceof StartNode)
                    {
                        // we're within the outermost scope
                        __parent = __topScope;
                        break;
                    }
                    else
                    {
                        // follow any path upwards - it doesn't matter which one
                        __current = ((AbstractMergeNode) __current).forwardEndAt(0);
                    }
                }
                else if (__current instanceof LoopExitNode)
                {
                    // if we reach a loop exit then we follow this loop and have the same parent
                    __parent = createLoopScope(((LoopExitNode) __current).loopBegin(), __loops, __topScope).parent;
                    break;
                }
                else
                {
                    __current = (FixedNode) __current.predecessor();
                }
            }
            __scope = new Scope(__loopBegin, __parent);
            __loops.put(__loopBegin, __scope);
        }
        return __scope;
    }

    /**
     * A scope holds information for the contents of one loop or of the root of the method. It does
     * not include child loops, i.e., the iteration in {@link #process(NodeWorkList)} explicitly
     * excludes the nodes of child loops.
     */
    // @class ComputeInliningRelevance.Scope
    // @closure
    private final class Scope
    {
        // @field
        public final FixedNode start;
        // @field
        public final Scope parent; // can be null for the outermost scope

        /**
         * The minimum probability along the most probable path in this scope. Computed lazily.
         */
        // @field
        private double fastPathMinProbability = ComputeInliningRelevance.UNINITIALIZED;
        /**
         * A measure of how important this scope is within its parent scope. Computed lazily.
         */
        // @field
        private double scopeRelevanceWithinParent = ComputeInliningRelevance.UNINITIALIZED;

        // @cons
        Scope(FixedNode __start, Scope __parent)
        {
            super();
            this.start = __start;
            this.parent = __parent;
        }

        public double getFastPathMinProbability()
        {
            if (fastPathMinProbability == ComputeInliningRelevance.UNINITIALIZED)
            {
                fastPathMinProbability = Math.max(ComputeInliningRelevance.EPSILON, ComputeInliningRelevance.this.computeFastPathMinProbability(start));
            }
            return fastPathMinProbability;
        }

        /**
         * Computes the ratio between the probabilities of the current scope's entry point and the
         * parent scope's fastPathMinProbability.
         */
        public double getScopeRelevanceWithinParent()
        {
            if (scopeRelevanceWithinParent == ComputeInliningRelevance.UNINITIALIZED)
            {
                if (start instanceof LoopBeginNode)
                {
                    double __scopeEntryProbability = ComputeInliningRelevance.this.nodeProbabilities.applyAsDouble(((LoopBeginNode) start).forwardEnd());

                    scopeRelevanceWithinParent = __scopeEntryProbability / parent.getFastPathMinProbability();
                }
                else
                {
                    scopeRelevanceWithinParent = 1D;
                }
            }
            return scopeRelevanceWithinParent;
        }

        /**
         * Processes all invokes in this scope by starting at the scope's start node and iterating
         * all fixed nodes. Child loops are skipped by going from loop entries directly to the loop
         * exits. Processing stops at loop exits of the current loop.
         */
        public void process(NodeWorkList __workList)
        {
            __workList.addAll(start.successors());

            for (Node __current : __workList)
            {
                if (__current instanceof Invoke)
                {
                    // process the invoke and queue its successors
                    ComputeInliningRelevance.this.nodeRelevances.put((FixedNode) __current, computeInvokeRelevance((Invoke) __current));
                    __workList.addAll(__current.successors());
                }
                else if (__current instanceof LoopBeginNode)
                {
                    // skip child loops by advancing over the loop exits
                    ((LoopBeginNode) __current).loopExits().forEach(__exit -> __workList.add(__exit.next()));
                }
                else if (__current instanceof LoopEndNode)
                {
                    // nothing to do
                }
                else if (__current instanceof LoopExitNode)
                {
                    // nothing to do
                }
                else if (__current instanceof FixedWithNextNode)
                {
                    __workList.add(((FixedWithNextNode) __current).next());
                }
                else if (__current instanceof EndNode)
                {
                    __workList.add(((EndNode) __current).merge());
                }
                else if (__current instanceof ControlSinkNode)
                {
                    // nothing to do
                }
                else if (__current instanceof ControlSplitNode)
                {
                    __workList.addAll(__current.successors());
                }
            }
        }

        /**
         * The relevance of an invoke is the ratio between the invoke's probability and the current
         * scope's fastPathMinProbability, adjusted by scopeRelevanceWithinParent.
         */
        public double computeInvokeRelevance(Invoke __invoke)
        {
            return (ComputeInliningRelevance.this.nodeProbabilities.applyAsDouble(__invoke.asNode()) / getFastPathMinProbability()) * Math.min(getScopeRelevanceWithinParent(), 1.0);
        }
    }

    /**
     * Computes the minimum probability along the most probable path within the scope. During
     * iteration, the method returns immediately once a loop exit is discovered.
     */
    private double computeFastPathMinProbability(FixedNode __scopeStart)
    {
        ArrayList<FixedNode> __pathBeginNodes = new ArrayList<>();
        __pathBeginNodes.add(__scopeStart);
        double __minPathProbability = nodeProbabilities.applyAsDouble(__scopeStart);
        boolean __isLoopScope = __scopeStart instanceof LoopBeginNode;

        do
        {
            Node __current = __pathBeginNodes.remove(__pathBeginNodes.size() - 1);
            do
            {
                if (__isLoopScope && __current instanceof LoopExitNode && ((LoopBeginNode) __scopeStart).loopExits().contains((LoopExitNode) __current))
                {
                    return __minPathProbability;
                }
                else if (__current instanceof LoopBeginNode && __current != __scopeStart)
                {
                    __current = getMaxProbabilityLoopExit((LoopBeginNode) __current, __pathBeginNodes);
                    __minPathProbability = getMinPathProbability((FixedNode) __current, __minPathProbability);
                }
                else if (__current instanceof ControlSplitNode)
                {
                    __current = getMaxProbabilitySux((ControlSplitNode) __current, __pathBeginNodes);
                    __minPathProbability = getMinPathProbability((FixedNode) __current, __minPathProbability);
                }
                else
                {
                    __current = __current.successors().first();
                }
            } while (__current != null);
        } while (!__pathBeginNodes.isEmpty());

        return __minPathProbability;
    }

    private double getMinPathProbability(FixedNode __current, double __minPathProbability)
    {
        return __current == null ? __minPathProbability : Math.min(__minPathProbability, nodeProbabilities.applyAsDouble(__current));
    }

    /**
     * Returns the most probable successor. If multiple successors share the maximum probability,
     * one is returned and the others are enqueued in pathBeginNodes.
     */
    private static Node getMaxProbabilitySux(ControlSplitNode __controlSplit, ArrayList<FixedNode> __pathBeginNodes)
    {
        Node __maxSux = null;
        double __maxProbability = 0.0;
        int __pathBeginCount = __pathBeginNodes.size();

        for (Node __sux : __controlSplit.successors())
        {
            double __probability = __controlSplit.probability((AbstractBeginNode) __sux);
            if (__probability > __maxProbability)
            {
                __maxProbability = __probability;
                __maxSux = __sux;
                truncate(__pathBeginNodes, __pathBeginCount);
            }
            else if (__probability == __maxProbability)
            {
                __pathBeginNodes.add((FixedNode) __sux);
            }
        }

        return __maxSux;
    }

    /**
     * Returns the most probable loop exit. If multiple successors share the maximum probability,
     * one is returned and the others are enqueued in pathBeginNodes.
     */
    private Node getMaxProbabilityLoopExit(LoopBeginNode __loopBegin, ArrayList<FixedNode> __pathBeginNodes)
    {
        Node __maxSux = null;
        double __maxProbability = 0.0;
        int __pathBeginCount = __pathBeginNodes.size();

        for (LoopExitNode __sux : __loopBegin.loopExits())
        {
            double __probability = nodeProbabilities.applyAsDouble(__sux);
            if (__probability > __maxProbability)
            {
                __maxProbability = __probability;
                __maxSux = __sux;
                truncate(__pathBeginNodes, __pathBeginCount);
            }
            else if (__probability == __maxProbability)
            {
                __pathBeginNodes.add(__sux);
            }
        }

        return __maxSux;
    }

    private static void truncate(ArrayList<FixedNode> __pathBeginNodes, int __pathBeginCount)
    {
        for (int __i = __pathBeginNodes.size() - __pathBeginCount; __i > 0; __i--)
        {
            __pathBeginNodes.remove(__pathBeginNodes.size() - 1);
        }
    }
}
