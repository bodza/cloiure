package giraaff.phases.common.inlining.walker;

import java.util.BitSet;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.nodes.CallTargetNode;
import giraaff.nodes.java.MethodCallTargetNode;
import giraaff.phases.common.inlining.info.InlineInfo;
import giraaff.phases.common.inlining.info.elem.Inlineable;
import giraaff.phases.common.inlining.info.elem.InlineableGraph;

/**
 * An instance of this class denotes a callsite being analyzed for inlining.
 *
 * Each element of the {@link InliningData} stack contains one such instance, the accompanying
 * {@link CallsiteHolder}s in that element represent feasible targets for the callsite in question.
 *
 * @see InliningData#moveForward()
 */
// @class MethodInvocation
public final class MethodInvocation
{
    // @field
    private final InlineInfo callee;
    // @field
    private final double probability;
    // @field
    private final double relevance;

    // @field
    private int processedGraphs;

    /**
     * The immutable positions of freshly instantiated arguments (ie, positions in
     * <code>callee.invoke.callTarget.arguments</code>).
     *
     * A freshly instantiated argument is either:
     *
     * <li>an {@link InliningData#isFreshInstantiation(giraaff.nodes.ValueNode)}</li>
     * <li>a fixed-param of the graph containing the callsite (ie, of <code>callee.graph()</code>
     * that contains <code>callee.invoke</code>)</li>
     *
     * Given those positions, the {@link giraaff.phases.common.inlining.walker.CallsiteHolderExplorable}
     * instantiated in {@link #buildCallsiteHolderForElement(int)} can determine which of <i>its</i>
     * parameters are fixed.
     */
    // @field
    private final BitSet freshlyInstantiatedArguments;

    // @field
    private final int sizeFreshArgs;

    // @cons
    public MethodInvocation(InlineInfo __info, double __probability, double __relevance, BitSet __freshlyInstantiatedArguments)
    {
        super();
        this.callee = __info;
        this.probability = __probability;
        this.relevance = __relevance;
        this.freshlyInstantiatedArguments = __freshlyInstantiatedArguments;
        this.sizeFreshArgs = __freshlyInstantiatedArguments == null ? 0 : __freshlyInstantiatedArguments.cardinality();
    }

    public void incrementProcessedGraphs()
    {
        processedGraphs++;
    }

    public int processedGraphs()
    {
        return processedGraphs;
    }

    public int totalGraphs()
    {
        return callee.numberOfMethods();
    }

    public InlineInfo callee()
    {
        return callee;
    }

    public double probability()
    {
        return probability;
    }

    public double relevance()
    {
        return relevance;
    }

    public boolean isRoot()
    {
        return callee == null;
    }

    public BitSet getFreshlyInstantiatedArguments()
    {
        return freshlyInstantiatedArguments;
    }

    public int getSizeFreshArgs()
    {
        return sizeFreshArgs;
    }

    public CallsiteHolder buildCallsiteHolderForElement(int __index)
    {
        Inlineable __elem = callee.inlineableElementAt(__index);
        InlineableGraph __ig = (InlineableGraph) __elem;
        final double __invokeProbability = probability * callee.probabilityAt(__index);
        final double __invokeRelevance = relevance * callee.relevanceAt(__index);
        return new CallsiteHolderExplorable(__ig.getGraph(), __invokeProbability, __invokeRelevance, freshlyInstantiatedArguments, null);
    }
}
