package graalvm.compiler.phases.common.inlining.policy;

import static graalvm.compiler.core.common.GraalOptions.InlineEverything;
import static graalvm.compiler.core.common.GraalOptions.LimitInlinedInvokes;
import static graalvm.compiler.core.common.GraalOptions.MaximumDesiredSize;
import static graalvm.compiler.core.common.GraalOptions.MaximumInliningSize;
import static graalvm.compiler.core.common.GraalOptions.SmallCompiledLowLevelGraphSize;
import static graalvm.compiler.core.common.GraalOptions.TrivialInliningSize;

import java.util.Map;

import graalvm.compiler.nodes.Invoke;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.spi.Replacements;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.common.inlining.InliningUtil;
import graalvm.compiler.phases.common.inlining.info.InlineInfo;
import graalvm.compiler.phases.common.inlining.walker.MethodInvocation;

public class GreedyInliningPolicy extends AbstractInliningPolicy
{
    public GreedyInliningPolicy(Map<Invoke, Double> hints)
    {
        super(hints);
    }

    @Override
    public boolean continueInlining(StructuredGraph currentGraph)
    {
        if (InliningUtil.getNodeCount(currentGraph) >= MaximumDesiredSize.getValue(currentGraph.getOptions()))
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

        if (InlineEverything.getValue(options))
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

        if (SmallCompiledLowLevelGraphSize.getValue(options) > 0 && lowLevelGraphSize > SmallCompiledLowLevelGraphSize.getValue(options) * inliningBonus)
        {
            return InliningPolicy.Decision.NO;
        }

        if (nodes < TrivialInliningSize.getValue(options) * inliningBonus)
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
        if (LimitInlinedInvokes.getValue(options) > 0 && fullyProcessed && invokes > LimitInlinedInvokes.getValue(options) * inliningBonus)
        {
            return InliningPolicy.Decision.NO;
        }

        double maximumNodes = computeMaximumSize(relevance, (int) (MaximumInliningSize.getValue(options) * inliningBonus));
        if (nodes <= maximumNodes)
        {
            return InliningPolicy.Decision.YES;
        }

        return InliningPolicy.Decision.NO;
    }
}
