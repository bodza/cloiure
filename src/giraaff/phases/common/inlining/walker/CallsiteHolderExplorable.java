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
    private final StructuredGraph graph;

    private final LinkedList<Invoke> remainingInvokes;
    private final double probability;
    private final double relevance;

    /**
     * @see #getFixedParams()
     */
    private final EconomicSet<ParameterNode> fixedParams;

    private final ToDoubleFunction<FixedNode> probabilities;
    private final ComputeInliningRelevance computeInliningRelevance;

    // @cons
    public CallsiteHolderExplorable(StructuredGraph graph, double probability, double relevance, BitSet freshlyInstantiatedArguments, LinkedList<Invoke> invokes)
    {
        super();
        this.graph = graph;
        this.probability = probability;
        this.relevance = relevance;
        this.fixedParams = fixedParamsAt(freshlyInstantiatedArguments);
        remainingInvokes = invokes == null ? new InliningIterator(graph).apply() : invokes;
        if (remainingInvokes.isEmpty())
        {
            probabilities = null;
            computeInliningRelevance = null;
        }
        else
        {
            probabilities = new FixedNodeProbabilityCache();
            computeInliningRelevance = new ComputeInliningRelevance(graph, probabilities);
            computeProbabilities();
        }
    }

    /**
     * @see #getFixedParams()
     */
    private EconomicSet<ParameterNode> fixedParamsAt(BitSet freshlyInstantiatedArguments)
    {
        if (freshlyInstantiatedArguments == null || freshlyInstantiatedArguments.isEmpty())
        {
            return EconomicSet.create(Equivalence.IDENTITY);
        }
        EconomicSet<ParameterNode> result = EconomicSet.create(Equivalence.IDENTITY);
        for (ParameterNode p : graph.getNodes(ParameterNode.TYPE))
        {
            if (freshlyInstantiatedArguments.get(p.index()))
            {
                result.add(p);
            }
        }
        return result;
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
        for (Invoke invoke : remainingInvokes)
        {
            if (!invoke.asNode().isAlive() || !containsInvoke(invoke))
            {
                return false;
            }
            if (!allArgsNonNull(invoke))
            {
                return false;
            }
        }
        for (ParameterNode fixedParam : fixedParams)
        {
            if (!containsParam(fixedParam))
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

    public void pushInvoke(Invoke invoke)
    {
        remainingInvokes.push(invoke);
    }

    public static boolean allArgsNonNull(Invoke invoke)
    {
        for (ValueNode arg : invoke.callTarget().arguments())
        {
            if (arg == null)
            {
                return false;
            }
        }
        return true;
    }

    public boolean containsInvoke(Invoke invoke)
    {
        for (Invoke i : graph().getInvokes())
        {
            if (i == invoke)
            {
                return true;
            }
        }
        return false;
    }

    public boolean containsParam(ParameterNode param)
    {
        for (ParameterNode p : graph.getNodes(ParameterNode.TYPE))
        {
            if (p == param)
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

    public double invokeProbability(Invoke invoke)
    {
        return probability * probabilities.applyAsDouble(invoke.asNode());
    }

    public double invokeRelevance(Invoke invoke)
    {
        return Math.min(AbstractInliningPolicy.CapInheritedRelevance, relevance) * computeInliningRelevance.getRelevance(invoke);
    }

    @Override
    public String toString()
    {
        return (graph != null ? method().format("%H.%n(%p)") : "<null method>") + remainingInvokes;
    }
}
