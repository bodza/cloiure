package graalvm.compiler.phases.common.inlining.walker;

import java.util.BitSet;

import graalvm.compiler.nodes.CallTargetNode;
import graalvm.compiler.nodes.java.MethodCallTargetNode;
import graalvm.compiler.phases.common.inlining.info.InlineInfo;
import graalvm.compiler.phases.common.inlining.info.elem.Inlineable;
import graalvm.compiler.phases.common.inlining.info.elem.InlineableGraph;

import jdk.vm.ci.meta.ResolvedJavaMethod;

/**
 * An instance of this class denotes a callsite being analyzed for inlining.
 *
 * Each element of the {@link InliningData} stack contains one such instance, the accompanying
 * {@link CallsiteHolder}s in that element represent feasible targets for the callsite in question.
 *
 * @see InliningData#moveForward()
 */
public class MethodInvocation
{
    private final InlineInfo callee;
    private final double probability;
    private final double relevance;

    private int processedGraphs;

    /**
     * The immutable positions of freshly instantiated arguments (ie, positions in
     * <code>callee.invoke.callTarget.arguments</code>).
     *
     * A freshly instantiated argument is either:
     *
     * <li>an {@link InliningData#isFreshInstantiation(graalvm.compiler.nodes.ValueNode)}</li>
     * <li>a fixed-param of the graph containing the callsite (ie, of <code>callee.graph()</code>
     * that contains <code>callee.invoke</code>)</li>
     *
     * Given those positions, the
     * {@link graalvm.compiler.phases.common.inlining.walker.CallsiteHolderExplorable}
     * instantiated in {@link #buildCallsiteHolderForElement(int)} can determine which of <i>its</i>
     * parameters are fixed.
     */
    private final BitSet freshlyInstantiatedArguments;

    private final int sizeFreshArgs;

    public MethodInvocation(InlineInfo info, double probability, double relevance, BitSet freshlyInstantiatedArguments)
    {
        this.callee = info;
        this.probability = probability;
        this.relevance = relevance;
        this.freshlyInstantiatedArguments = freshlyInstantiatedArguments;
        this.sizeFreshArgs = freshlyInstantiatedArguments == null ? 0 : freshlyInstantiatedArguments.cardinality();
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

    public CallsiteHolder buildCallsiteHolderForElement(int index)
    {
        Inlineable elem = callee.inlineableElementAt(index);
        InlineableGraph ig = (InlineableGraph) elem;
        final double invokeProbability = probability * callee.probabilityAt(index);
        final double invokeRelevance = relevance * callee.relevanceAt(index);
        return new CallsiteHolderExplorable(ig.getGraph(), invokeProbability, invokeRelevance, freshlyInstantiatedArguments, null);
    }

    @Override
    public String toString()
    {
        if (isRoot())
        {
            return "<root>";
        }
        CallTargetNode callTarget = callee.invoke().callTarget();
        if (callTarget instanceof MethodCallTargetNode)
        {
            ResolvedJavaMethod calleeMethod = ((MethodCallTargetNode) callTarget).targetMethod();
            return calleeMethod.format("Invoke#%H.%n(%p)");
        }
        else
        {
            return "Invoke#" + callTarget.targetName();
        }
    }
}
