package giraaff.phases.common.inlining.policy;

import java.util.Map;

import giraaff.core.common.GraalOptions;
import giraaff.nodes.Invoke;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.spi.Replacements;
import giraaff.phases.common.inlining.InliningUtil;
import giraaff.phases.common.inlining.info.InlineInfo;
import giraaff.phases.common.inlining.walker.MethodInvocation;

// @class GreedyInliningPolicy
public final class GreedyInliningPolicy extends AbstractInliningPolicy
{
    // @cons GreedyInliningPolicy
    public GreedyInliningPolicy(Map<Invoke, Double> __hints)
    {
        super(__hints);
    }

    @Override
    public boolean continueInlining(StructuredGraph __currentGraph)
    {
        return InliningUtil.getNodeCount(__currentGraph) < GraalOptions.maximumDesiredSize;
    }

    @Override
    public InliningPolicy.Decision isWorthInlining(Replacements __replacements, MethodInvocation __invocation, int __inliningDepth, boolean __fullyProcessed)
    {
        final InlineInfo __info = __invocation.callee();
        final double __probability = __invocation.probability();
        final double __relevance = __invocation.relevance();

        if (GraalOptions.inlineEverything)
        {
            return InliningPolicy.Decision.YES;
        }

        if (isIntrinsic(__replacements, __info))
        {
            return InliningPolicy.Decision.YES;
        }

        if (__info.shouldInline())
        {
            return InliningPolicy.Decision.YES;
        }

        double __inliningBonus = getInliningBonus(__info);
        int __nodes = __info.determineNodeCount();
        int __lowLevelGraphSize = previousLowLevelGraphSize(__info);

        if (GraalOptions.smallCompiledLowLevelGraphSize > 0 && __lowLevelGraphSize > GraalOptions.smallCompiledLowLevelGraphSize * __inliningBonus)
        {
            return InliningPolicy.Decision.NO;
        }

        if (__nodes < GraalOptions.trivialInliningSize * __inliningBonus)
        {
            return InliningPolicy.Decision.YES;
        }

        // TODO invoked methods that are on important paths but not yet compiled -> will be
        // compiled anyways and it is likely that we are the only caller... might be useful
        // to inline those methods but increases bootstrap time (maybe those methods are
        // also getting queued in the compilation queue concurrently)
        double __invokes = determineInvokeProbability(__info);
        if (GraalOptions.limitInlinedInvokes > 0 && __fullyProcessed && __invokes > GraalOptions.limitInlinedInvokes * __inliningBonus)
        {
            return InliningPolicy.Decision.NO;
        }

        double __maximumNodes = computeMaximumSize(__relevance, (int) (GraalOptions.maximumInliningSize * __inliningBonus));
        if (__nodes <= __maximumNodes)
        {
            return InliningPolicy.Decision.YES;
        }

        return InliningPolicy.Decision.NO;
    }
}
