package graalvm.compiler.hotspot.phases.aot;

import static graalvm.compiler.core.common.GraalOptions.InlineEverything;
import static graalvm.compiler.core.common.GraalOptions.TrivialInliningSize;

import java.util.Map;

import graalvm.compiler.core.common.GraalOptions;
import graalvm.compiler.nodes.Invoke;
import graalvm.compiler.nodes.spi.Replacements;
import graalvm.compiler.options.Option;
import graalvm.compiler.options.OptionKey;
import graalvm.compiler.options.OptionType;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.common.inlining.InliningUtil;
import graalvm.compiler.phases.common.inlining.info.InlineInfo;
import graalvm.compiler.phases.common.inlining.policy.GreedyInliningPolicy;
import graalvm.compiler.phases.common.inlining.policy.InliningPolicy;
import graalvm.compiler.phases.common.inlining.walker.MethodInvocation;

import jdk.vm.ci.hotspot.HotSpotResolvedObjectType;

public class AOTInliningPolicy extends GreedyInliningPolicy
{
    public static class Options
    {
        @Option(help = "", type = OptionType.Expert)
        public static final OptionKey<Double> AOTInliningDepthToSizeRate = new OptionKey<>(2.5);
        @Option(help = "", type = OptionType.Expert)
        public static final OptionKey<Integer> AOTInliningSizeMaximum = new OptionKey<>(300);
        @Option(help = "", type = OptionType.Expert)
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
        if (InlineEverything.getValue(options))
        {
            InliningUtil.traceInlinedMethod(info, inliningDepth, fullyProcessed, "inline everything");
            return InliningPolicy.Decision.YES;
        }

        if (isIntrinsic(replacements, info))
        {
            InliningUtil.traceInlinedMethod(info, inliningDepth, fullyProcessed, "intrinsic");
            return InliningPolicy.Decision.YES;
        }

        if (info.shouldInline())
        {
            InliningUtil.traceInlinedMethod(info, inliningDepth, fullyProcessed, "forced inlining");
            return InliningPolicy.Decision.YES;
        }

        double inliningBonus = getInliningBonus(info);
        int nodes = info.determineNodeCount();

        if (nodes < TrivialInliningSize.getValue(options) * inliningBonus)
        {
            InliningUtil.traceInlinedMethod(info, inliningDepth, fullyProcessed, "trivial (relevance=%f, probability=%f, bonus=%f, nodes=%d)", relevance, probability, inliningBonus, nodes);
            return InliningPolicy.Decision.YES;
        }

        double maximumNodes = computeMaximumSize(relevance, (int) (maxInliningSize(inliningDepth, options) * inliningBonus));
        if (nodes <= maximumNodes)
        {
            InliningUtil.traceInlinedMethod(info, inliningDepth, fullyProcessed, "relevance-based (relevance=%f, probability=%f, bonus=%f, nodes=%d <= %f)", relevance, probability, inliningBonus, nodes, maximumNodes);
            return InliningPolicy.Decision.YES;
        }

        InliningUtil.traceNotInlinedMethod(info, inliningDepth, "relevance-based (relevance=%f, probability=%f, bonus=%f, nodes=%d > %f)", relevance, probability, inliningBonus, nodes, maximumNodes);
        return InliningPolicy.Decision.NO;
    }
}
