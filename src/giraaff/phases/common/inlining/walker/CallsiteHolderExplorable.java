package giraaff.phases.common.inlining.walker;

import java.util.BitSet;
import java.util.LinkedList;
import java.util.function.ToDoubleFunction;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import org.graalvm.collections.EconomicSet;
import org.graalvm.collections.Equivalence;

import giraaff.nodes.FixedNode;
import giraaff.nodes.Invoke;
import giraaff.nodes.ParameterNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.phases.common.inlining.policy.AbstractInliningPolicy;
import giraaff.phases.graph.FixedNodeProbabilityCache;

/**
 * A {@link CallsiteHolder} whose graph has been copied already and thus can be modified without
 * affecting the original (usually cached) version.
 *
 * An instance of this class is derived from an
 * {@link giraaff.phases.common.inlining.info.elem.InlineableGraph InlineableGraph} and
 * contains a subset of the information there: just the {@link Invoke} nodes from it. Such nodes are
 * candidates for depth-first search of further inlining opportunities (thus the adjective
 * "explorable" given to this class)
 *
 * @see InliningData#moveForward()
 */
// @class CallsiteHolderExplorable
public final class CallsiteHolderExplorable extends CallsiteHolder
{
    /**
     * Graph in which inlining may be performed at one or more of the callsites containined in
     * {@link #remainingInvokes}.
     */
    // @field
    private final StructuredGraph graph;

    // @field
    private final LinkedList<Invoke> remainingInvokes;
    // @field
    private final double probability;
    // @field
    private final double relevance;

    /**
     * @see #getFixedParams()
     */
    // @field
    private final EconomicSet<ParameterNode> fixedParams;

    // @field
    private final ToDoubleFunction<FixedNode> probabilities;
    // @field
    private final ComputeInliningRelevance computeInliningRelevance;

    // @cons
    public CallsiteHolderExplorable(StructuredGraph __graph, double __probability, double __relevance, BitSet __freshlyInstantiatedArguments, LinkedList<Invoke> __invokes)
    {
        super();
        this.graph = __graph;
        this.probability = __probability;
        this.relevance = __relevance;
        this.fixedParams = fixedParamsAt(__freshlyInstantiatedArguments);
        remainingInvokes = __invokes == null ? new InliningIterator(__graph).apply() : __invokes;
        if (remainingInvokes.isEmpty())
        {
            probabilities = null;
            computeInliningRelevance = null;
        }
        else
        {
            probabilities = new FixedNodeProbabilityCache();
            computeInliningRelevance = new ComputeInliningRelevance(__graph, probabilities);
            computeProbabilities();
        }
    }

    /**
     * @see #getFixedParams()
     */
    private EconomicSet<ParameterNode> fixedParamsAt(BitSet __freshlyInstantiatedArguments)
    {
        if (__freshlyInstantiatedArguments == null || __freshlyInstantiatedArguments.isEmpty())
        {
            return EconomicSet.create(Equivalence.IDENTITY);
        }
        EconomicSet<ParameterNode> __result = EconomicSet.create(Equivalence.IDENTITY);
        for (ParameterNode __p : graph.getNodes(ParameterNode.TYPE))
        {
            if (__freshlyInstantiatedArguments.get(__p.index()))
            {
                __result.add(__p);
            }
        }
        return __result;
    }

    /**
     * Parameters for which the callsite targeting {@link #graph()} provides "fixed" arguments. That
     * callsite isn't referenced by this instance. Instead, it belongs to the graph of the caller of
     * this {@link CallsiteHolderExplorable}
     *
     * Constant arguments don't contribute to fixed-params: those params have been removed already,
     * see {@link giraaff.phases.common.inlining.info.elem.InlineableGraph}.
     *
     * Instead, fixed-params are those receiving freshly instantiated arguments (possibly
     * instantiated several levels up in the call-hierarchy)
     */
    public EconomicSet<ParameterNode> getFixedParams()
    {
        return fixedParams;
    }

    public boolean repOK()
    {
        for (Invoke __invoke : remainingInvokes)
        {
            if (!__invoke.asNode().isAlive() || !containsInvoke(__invoke))
            {
                return false;
            }
            if (!allArgsNonNull(__invoke))
            {
                return false;
            }
        }
        for (ParameterNode __fixedParam : fixedParams)
        {
            if (!containsParam(__fixedParam))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public ResolvedJavaMethod method()
    {
        return graph == null ? null : graph.method();
    }

    @Override
    public boolean hasRemainingInvokes()
    {
        return !remainingInvokes.isEmpty();
    }

    @Override
    public StructuredGraph graph()
    {
        return graph;
    }

    public Invoke popInvoke()
    {
        return remainingInvokes.removeFirst();
    }

    public void pushInvoke(Invoke __invoke)
    {
        remainingInvokes.push(__invoke);
    }

    public static boolean allArgsNonNull(Invoke __invoke)
    {
        for (ValueNode __arg : __invoke.callTarget().arguments())
        {
            if (__arg == null)
            {
                return false;
            }
        }
        return true;
    }

    public boolean containsInvoke(Invoke __invoke)
    {
        for (Invoke __i : graph().getInvokes())
        {
            if (__i == __invoke)
            {
                return true;
            }
        }
        return false;
    }

    public boolean containsParam(ParameterNode __param)
    {
        for (ParameterNode __p : graph.getNodes(ParameterNode.TYPE))
        {
            if (__p == __param)
            {
                return true;
            }
        }
        return false;
    }

    public void computeProbabilities()
    {
        computeInliningRelevance.compute();
    }

    public double invokeProbability(Invoke __invoke)
    {
        return probability * probabilities.applyAsDouble(__invoke.asNode());
    }

    public double invokeRelevance(Invoke __invoke)
    {
        return Math.min(AbstractInliningPolicy.CapInheritedRelevance, relevance) * computeInliningRelevance.getRelevance(__invoke);
    }
}
