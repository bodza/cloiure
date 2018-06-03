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
    private final StructuredGraph ___graph;
    // @field
    private final ToDoubleFunction<FixedNode> ___nodeProbabilities;

    ///
    // Node relevances are pre-computed for all invokes if the graph contains loops. If there are no
    // loops, the computation happens lazily based on {@link #rootScope}.
    ///
    // @field
    private EconomicMap<FixedNode, Double> ___nodeRelevances;
    ///
    // This scope is non-null if (and only if) there are no loops in the graph. In this case, the
    // root scope is used to compute invoke relevances on the fly.
    ///
    // @field
    private Scope ___rootScope;

    // @cons
    public ComputeInliningRelevance(StructuredGraph __graph, ToDoubleFunction<FixedNode> __nodeProbabilities)
    {
        super();
        this.___graph = __graph;
        this.___nodeProbabilities = __nodeProbabilities;
    }

    ///
    // Initializes or updates the relevance computation. If there are no loops within the graph,
    // most computation happens lazily.
    ///
    public void compute()
    {
        this.___rootScope = null;
        if (!this.___graph.hasLoops())
        {
            // fast path for the frequent case of no loops
            this.___rootScope = new Scope(this.___graph.start(), null);
        }
        else
        {
            if (this.___nodeRelevances == null)
            {
                this.___nodeRelevances = EconomicMap.create(Equivalence.IDENTITY, EXPECTED_MIN_INVOKE_COUNT + InliningUtil.getNodeCount(this.___graph) / EXPECTED_INVOKE_RATIO);
            }
            NodeWorkList __workList = this.___graph.createNodeWorkList();
            EconomicMap<LoopBeginNode, Scope> __loops = EconomicMap.create(Equivalence.IDENTITY, EXPECTED_LOOP_COUNT);

            Scope __topScope = new Scope(this.___graph.start(), null);
            for (LoopBeginNode __loopBegin : this.___graph.getNodes(LoopBeginNode.TYPE))
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
        if (this.___rootScope != null)
        {
            return this.___rootScope.computeInvokeRelevance(__invoke);
        }
        return this.___nodeRelevances.get(__invoke.asNode());
    }

    ///
    // Determines the parent of the given loop and creates a {@link Scope} object for each one. This
    // method will call itself recursively if no {@link Scope} for the parent loop exists.
    ///
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
                    __parent = createLoopScope(((LoopExitNode) __current).loopBegin(), __loops, __topScope).___parent;
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

    ///
    // A scope holds information for the contents of one loop or of the root of the method. It does
    // not include child loops, i.e., the iteration in {@link #process(NodeWorkList)} explicitly
    // excludes the nodes of child loops.
    ///
    // @class ComputeInliningRelevance.Scope
    // @closure
    private final class Scope
    {
        // @field
        public final FixedNode ___start;
        // @field
        public final Scope ___parent; // can be null for the outermost scope

        ///
        // The minimum probability along the most probable path in this scope. Computed lazily.
        ///
        // @field
        private double ___fastPathMinProbability = ComputeInliningRelevance.UNINITIALIZED;
        ///
        // A measure of how important this scope is within its parent scope. Computed lazily.
        ///
        // @field
        private double ___scopeRelevanceWithinParent = ComputeInliningRelevance.UNINITIALIZED;

        // @cons
        Scope(FixedNode __start, Scope __parent)
        {
            super();
            this.___start = __start;
            this.___parent = __parent;
        }

        public double getFastPathMinProbability()
        {
            if (this.___fastPathMinProbability == ComputeInliningRelevance.UNINITIALIZED)
            {
                this.___fastPathMinProbability = Math.max(ComputeInliningRelevance.EPSILON, ComputeInliningRelevance.this.computeFastPathMinProbability(this.___start));
            }
            return this.___fastPathMinProbability;
        }

        ///
        // Computes the ratio between the probabilities of the current scope's entry point and the
        // parent scope's fastPathMinProbability.
        ///
        public double getScopeRelevanceWithinParent()
        {
            if (this.___scopeRelevanceWithinParent == ComputeInliningRelevance.UNINITIALIZED)
            {
                if (this.___start instanceof LoopBeginNode)
                {
                    double __scopeEntryProbability = ComputeInliningRelevance.this.___nodeProbabilities.applyAsDouble(((LoopBeginNode) this.___start).forwardEnd());

                    this.___scopeRelevanceWithinParent = __scopeEntryProbability / this.___parent.getFastPathMinProbability();
                }
                else
                {
                    this.___scopeRelevanceWithinParent = 1D;
                }
            }
            return this.___scopeRelevanceWithinParent;
        }

        ///
        // Processes all invokes in this scope by starting at the scope's start node and iterating
        // all fixed nodes. Child loops are skipped by going from loop entries directly to the loop
        // exits. Processing stops at loop exits of the current loop.
        ///
        public void process(NodeWorkList __workList)
        {
            __workList.addAll(this.___start.successors());

            for (Node __current : __workList)
            {
                if (__current instanceof Invoke)
                {
                    // process the invoke and queue its successors
                    ComputeInliningRelevance.this.___nodeRelevances.put((FixedNode) __current, computeInvokeRelevance((Invoke) __current));
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

        ///
        // The relevance of an invoke is the ratio between the invoke's probability and the current
        // scope's fastPathMinProbability, adjusted by scopeRelevanceWithinParent.
        ///
        public double computeInvokeRelevance(Invoke __invoke)
        {
            return (ComputeInliningRelevance.this.___nodeProbabilities.applyAsDouble(__invoke.asNode()) / getFastPathMinProbability()) * Math.min(getScopeRelevanceWithinParent(), 1.0);
        }
    }

    ///
    // Computes the minimum probability along the most probable path within the scope. During
    // iteration, the method returns immediately once a loop exit is discovered.
    ///
    private double computeFastPathMinProbability(FixedNode __scopeStart)
    {
        ArrayList<FixedNode> __pathBeginNodes = new ArrayList<>();
        __pathBeginNodes.add(__scopeStart);
        double __minPathProbability = this.___nodeProbabilities.applyAsDouble(__scopeStart);
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
        return __current == null ? __minPathProbability : Math.min(__minPathProbability, this.___nodeProbabilities.applyAsDouble(__current));
    }

    ///
    // Returns the most probable successor. If multiple successors share the maximum probability,
    // one is returned and the others are enqueued in pathBeginNodes.
    ///
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

    ///
    // Returns the most probable loop exit. If multiple successors share the maximum probability,
    // one is returned and the others are enqueued in pathBeginNodes.
    ///
    private Node getMaxProbabilityLoopExit(LoopBeginNode __loopBegin, ArrayList<FixedNode> __pathBeginNodes)
    {
        Node __maxSux = null;
        double __maxProbability = 0.0;
        int __pathBeginCount = __pathBeginNodes.size();

        for (LoopExitNode __sux : __loopBegin.loopExits())
        {
            double __probability = this.___nodeProbabilities.applyAsDouble(__sux);
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
