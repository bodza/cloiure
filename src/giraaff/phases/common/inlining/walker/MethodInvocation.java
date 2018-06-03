package giraaff.phases.common.inlining.walker;

import java.util.BitSet;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.nodes.CallTargetNode;
import giraaff.nodes.java.MethodCallTargetNode;
import giraaff.phases.common.inlining.info.InlineInfo;
import giraaff.phases.common.inlining.info.elem.Inlineable;
import giraaff.phases.common.inlining.info.elem.InlineableGraph;

///
// An instance of this class denotes a callsite being analyzed for inlining.
//
// Each element of the {@link InliningData} stack contains one such instance, the accompanying
// {@link CallsiteHolder}s in that element represent feasible targets for the callsite in question.
//
// @see InliningData#moveForward()
///
// @class MethodInvocation
public final class MethodInvocation
{
    // @field
    private final InlineInfo ___callee;
    // @field
    private final double ___probability;
    // @field
    private final double ___relevance;

    // @field
    private int ___processedGraphs;

    ///
    // The immutable positions of freshly instantiated arguments (ie, positions in
    // <code>callee.invoke.callTarget.arguments</code>).
    //
    // A freshly instantiated argument is either:
    //
    // <li>an {@link InliningData#isFreshInstantiation(giraaff.nodes.ValueNode)}</li>
    // <li>a fixed-param of the graph containing the callsite (ie, of <code>callee.graph()</code>
    // that contains <code>callee.invoke</code>)</li>
    //
    // Given those positions, the {@link giraaff.phases.common.inlining.walker.CallsiteHolderExplorable}
    // instantiated in {@link #buildCallsiteHolderForElement(int)} can determine which of <i>its</i>
    // parameters are fixed.
    ///
    // @field
    private final BitSet ___freshlyInstantiatedArguments;

    // @field
    private final int ___sizeFreshArgs;

    // @cons
    public MethodInvocation(InlineInfo __info, double __probability, double __relevance, BitSet __freshlyInstantiatedArguments)
    {
        super();
        this.___callee = __info;
        this.___probability = __probability;
        this.___relevance = __relevance;
        this.___freshlyInstantiatedArguments = __freshlyInstantiatedArguments;
        this.___sizeFreshArgs = __freshlyInstantiatedArguments == null ? 0 : __freshlyInstantiatedArguments.cardinality();
    }

    public void incrementProcessedGraphs()
    {
        this.___processedGraphs++;
    }

    public int processedGraphs()
    {
        return this.___processedGraphs;
    }

    public int totalGraphs()
    {
        return this.___callee.numberOfMethods();
    }

    public InlineInfo callee()
    {
        return this.___callee;
    }

    public double probability()
    {
        return this.___probability;
    }

    public double relevance()
    {
        return this.___relevance;
    }

    public boolean isRoot()
    {
        return this.___callee == null;
    }

    public BitSet getFreshlyInstantiatedArguments()
    {
        return this.___freshlyInstantiatedArguments;
    }

    public int getSizeFreshArgs()
    {
        return this.___sizeFreshArgs;
    }

    public CallsiteHolder buildCallsiteHolderForElement(int __index)
    {
        Inlineable __elem = this.___callee.inlineableElementAt(__index);
        InlineableGraph __ig = (InlineableGraph) __elem;
        final double __invokeProbability = this.___probability * this.___callee.probabilityAt(__index);
        final double __invokeRelevance = this.___relevance * this.___callee.relevanceAt(__index);
        return new CallsiteHolderExplorable(__ig.getGraph(), __invokeProbability, __invokeRelevance, this.___freshlyInstantiatedArguments, null);
    }
}
