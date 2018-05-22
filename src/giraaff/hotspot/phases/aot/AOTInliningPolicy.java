package giraaff.hotspot.phases.aot;

import java.util.Map;

import jdk.vm.ci.hotspot.HotSpotResolvedObjectType;

import giraaff.core.common.GraalOptions;
import giraaff.nodes.Invoke;
import giraaff.nodes.spi.Replacements;
import giraaff.options.OptionKey;
import giraaff.options.OptionValues;
import giraaff.phases.common.inlining.info.InlineInfo;
import giraaff.phases.common.inlining.policy.GreedyInliningPolicy;
import giraaff.phases.common.inlining.policy.InliningPolicy;
import giraaff.phases.common.inlining.walker.MethodInvocation;

public class AOTInliningPolicy extends GreedyInliningPolicy
{
    public static class Options
    {
        public static final OptionKey<Double> AOTInliningDepthToSizeRate = new OptionKey<>(2.5);
        public static final OptionKey<Integer> AOTInliningSizeMaximum = new OptionKey<>(300);
        public static final OptionKey<Integer> AOTInliningSizeMinimum = new OptionKey<>(50);
    }

    public AOTInliningPolicy(Map<Invoke, Double> hints)
    {
        super(hints);
    }

    protected double maxInliningSize(int inliningDepth, OptionValues options)
    {
        return Math.max(Options.AOTInliningSizeMaximum.getValue(options) / (inliningDepth * Options.AOTInliningDepthToSizeRate.getValue(options)), Options.AOTInliningSizeMinimum.getValue(options));
    }

    @Override
    public Decision isWorthInlining(Replacements replacements, MethodInvocation invocation, int inliningDepth, boolean fullyProcessed)
    {
        final InlineInfo info = invocation.callee();

        for (int i = 0; i < info.numberOfMethods(); ++i)
        {
            HotSpotResolvedObjectType t = (HotSpotResolvedObjectType) info.methodAt(i).getDeclaringClass();
            if (t.getFingerprint() == 0)
            {
                return InliningPolicy.Decision.NO;
            }
        }

        final double probability = invocation.probability();
        final double relevance = invocation.relevance();

        OptionValues options = info.graph().getOptions();
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

        if (nodes < GraalOptions.TrivialInliningSize.getValue(options) * inliningBonus)
        {
            return InliningPolicy.Decision.YES;
        }

        double maximumNodes = computeMaximumSize(relevance, (int) (maxInliningSize(inliningDepth, options) * inliningBonus));
        if (nodes <= maximumNodes)
        {
            return InliningPolicy.Decision.YES;
        }

        return InliningPolicy.Decision.NO;
    }
}
