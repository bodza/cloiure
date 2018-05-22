package graalvm.compiler.core.phases;

import graalvm.compiler.core.common.GraalOptions;
import graalvm.compiler.loop.DefaultLoopPolicies;
import graalvm.compiler.loop.LoopPolicies;
import graalvm.compiler.loop.phases.LoopFullUnrollPhase;
import graalvm.compiler.loop.phases.LoopPeelingPhase;
import graalvm.compiler.loop.phases.LoopUnswitchingPhase;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.options.OptionKey;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.PhaseSuite;
import graalvm.compiler.phases.common.CanonicalizerPhase;
import graalvm.compiler.phases.common.ConvertDeoptimizeToGuardPhase;
import graalvm.compiler.phases.common.DeadCodeEliminationPhase;
import graalvm.compiler.phases.common.DeadCodeEliminationPhase.Optionality;
import graalvm.compiler.phases.common.IncrementalCanonicalizerPhase;
import graalvm.compiler.phases.common.IterativeConditionalEliminationPhase;
import graalvm.compiler.phases.common.LoweringPhase;
import graalvm.compiler.phases.common.RemoveValueProxyPhase;
import graalvm.compiler.phases.common.inlining.InliningPhase;
import graalvm.compiler.phases.tiers.HighTierContext;
import graalvm.compiler.virtual.phases.ea.EarlyReadEliminationPhase;
import graalvm.compiler.virtual.phases.ea.PartialEscapePhase;

public class HighTier extends PhaseSuite<HighTierContext>
{
    public static class Options
    {
        // Option "Enable inlining."
        public static final OptionKey<Boolean> Inline = new OptionKey<>(true);
    }

    public HighTier(OptionValues options)
    {
        CanonicalizerPhase canonicalizer = new CanonicalizerPhase();
        if (GraalOptions.ImmutableCode.getValue(options))
        {
            canonicalizer.disableReadCanonicalization();
        }

        appendPhase(canonicalizer);

        if (Options.Inline.getValue(options))
        {
            appendPhase(new InliningPhase(canonicalizer));
            appendPhase(new DeadCodeEliminationPhase(Optionality.Optional));
        }

        if (GraalOptions.OptConvertDeoptsToGuards.getValue(options))
        {
            appendPhase(new IncrementalCanonicalizerPhase<>(canonicalizer, new ConvertDeoptimizeToGuardPhase()));
        }

        if (GraalOptions.ConditionalElimination.getValue(options))
        {
            appendPhase(new IterativeConditionalEliminationPhase(canonicalizer, false));
        }

        LoopPolicies loopPolicies = createLoopPolicies();
        if (GraalOptions.FullUnroll.getValue(options))
        {
            appendPhase(new LoopFullUnrollPhase(canonicalizer, loopPolicies));
        }

        if (GraalOptions.OptLoopTransform.getValue(options))
        {
            if (GraalOptions.LoopPeeling.getValue(options))
            {
                appendPhase(new LoopPeelingPhase(loopPolicies));
            }
            if (GraalOptions.LoopUnswitch.getValue(options))
            {
                appendPhase(new LoopUnswitchingPhase(loopPolicies));
            }
        }

        appendPhase(canonicalizer);

        if (GraalOptions.PartialEscapeAnalysis.getValue(options))
        {
            appendPhase(new PartialEscapePhase(true, canonicalizer, options));
        }

        if (GraalOptions.OptReadElimination.getValue(options))
        {
            appendPhase(new EarlyReadEliminationPhase(canonicalizer));
        }

        appendPhase(new RemoveValueProxyPhase());

        appendPhase(new LoweringPhase(canonicalizer, LoweringTool.StandardLoweringStage.HIGH_TIER));
    }

    public LoopPolicies createLoopPolicies()
    {
        return new DefaultLoopPolicies();
    }
}
