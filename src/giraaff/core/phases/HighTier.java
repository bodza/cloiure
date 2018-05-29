package giraaff.core.phases;

import giraaff.core.common.GraalOptions;
import giraaff.loop.DefaultLoopPolicies;
import giraaff.loop.LoopPolicies;
import giraaff.loop.phases.LoopFullUnrollPhase;
import giraaff.loop.phases.LoopPeelingPhase;
import giraaff.loop.phases.LoopUnswitchingPhase;
import giraaff.nodes.spi.LoweringTool;
import giraaff.options.OptionKey;
import giraaff.options.OptionValues;
import giraaff.phases.PhaseSuite;
import giraaff.phases.common.CanonicalizerPhase;
import giraaff.phases.common.ConvertDeoptimizeToGuardPhase;
import giraaff.phases.common.DeadCodeEliminationPhase;
import giraaff.phases.common.DeadCodeEliminationPhase.Optionality;
import giraaff.phases.common.IncrementalCanonicalizerPhase;
import giraaff.phases.common.IterativeConditionalEliminationPhase;
import giraaff.phases.common.LoweringPhase;
import giraaff.phases.common.RemoveValueProxyPhase;
import giraaff.phases.common.inlining.InliningPhase;
import giraaff.phases.tiers.HighTierContext;
import giraaff.virtual.phases.ea.EarlyReadEliminationPhase;
import giraaff.virtual.phases.ea.PartialEscapePhase;

// @class HighTier
public final class HighTier extends PhaseSuite<HighTierContext>
{
    // @class HighTier.Options
    public static final class Options
    {
        // @Option "Enable inlining."
        public static final OptionKey<Boolean> Inline = new OptionKey<>(true);
    }

    // @cons
    public HighTier(OptionValues options)
    {
        super();
        CanonicalizerPhase canonicalizer = new CanonicalizerPhase();
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
