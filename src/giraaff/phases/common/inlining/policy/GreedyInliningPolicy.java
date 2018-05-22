package giraaff.phases.common.inlining.policy;

import java.util.Map;

import giraaff.core.common.GraalOptions;
import giraaff.nodes.Invoke;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.spi.Replacements;
import giraaff.options.OptionValues;
import giraaff.phases.common.inlining.InliningUtil;
import giraaff.phases.common.inlining.info.InlineInfo;
import giraaff.phases.common.inlining.walker.MethodInvocation;

public class GreedyInliningPolicy extends AbstractInliningPolicy
{
    public GreedyInliningPolicy(Map<Invoke, Double> hints)
    {
        super(hints);
    }

    @Override
    public boolean continueInlining(StructuredGraph currentGraph)
    {
        if (InliningUtil.getNodeCount(currentGraph) >= GraalOptions.MaximumDesiredSize.getValue(currentGraph.getOptions()))
        {
            return false;
        }
        return true;
    }

    @Override
    public Decision isWorthInlining(Replacements replacements, MethodInvocation invocation, int inliningDepth, boolean fullyProcessed)
    {
        final InlineInfo info = invocation.callee();
        OptionValues options = info.graph().getOptions();
        final double probability = invocation.probability();
        final double relevance = invocation.relevance();

        if (GraalOptions.InlineEverything.getValue(options))
        {
            return InliningPolicy.Decision.YES;
        }

        if (isIntrinsic(replacements, info))
        {
            return InliningPolicy.Decision.YES;
        }

        if (info.shouldInline())
        {
            return InliningPolicy.Decision.YES;
        }

        double inliningBonus = getInliningBonus(info);
        int nodes = info.determineNodeCount();
        int lowLevelGraphSize = previousLowLevelGraphSize(info);

        if (GraalOptions.SmallCompiledLowLevelGraphSize.getValue(options) > 0 && lowLevelGraphSize > GraalOptions.SmallCompiledLowLevelGraphSize.getValue(options) * inliningBonus)
        {
            return InliningPolicy.Decision.NO;
        }

        if (nodes < GraalOptions.TrivialInliningSize.getValue(options) * inliningBonus)
        {
            return InliningPolicy.Decision.YES;
        }

        /*
         * TODO (chaeubl): invoked methods that are on important paths but not yet compiled -> will
         * be compiled anyways and it is likely that we are the only caller... might be useful to
         * inline those methods but increases bootstrap time (maybe those methods are also getting
         * queued in the compilation queue concurrently)
         */
        double invokes = determineInvokeProbability(info);
        if (GraalOptions.LimitInlinedInvokes.getValue(options) > 0 && fullyProcessed && invokes > GraalOptions.LimitInlinedInvokes.getValue(options) * inliningBonus)
        {
            return InliningPolicy.Decision.NO;
        }

        double maximumNodes = computeMaximumSize(relevance, (int) (GraalOptions.MaximumInliningSize.getValue(options) * inliningBonus));
        if (nodes <= maximumNodes)
        {
            return InliningPolicy.Decision.YES;
        }

        return InliningPolicy.Decision.NO;
    }
}
