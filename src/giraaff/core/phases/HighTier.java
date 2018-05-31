package giraaff.core.phases;

import giraaff.core.common.GraalOptions;
import giraaff.loop.DefaultLoopPolicies;
import giraaff.loop.LoopPolicies;
import giraaff.loop.phases.LoopFullUnrollPhase;
import giraaff.loop.phases.LoopPeelingPhase;
import giraaff.loop.phases.LoopUnswitchingPhase;
import giraaff.nodes.spi.LoweringTool;
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
    // @cons
    public HighTier()
    {
        super();
        CanonicalizerPhase canonicalizer = new CanonicalizerPhase();
        appendPhase(canonicalizer);

        if (GraalOptions.inline)
        {
            appendPhase(new InliningPhase(canonicalizer));
            appendPhase(new DeadCodeEliminationPhase(Optionality.Optional));
        }

        if (GraalOptions.optConvertDeoptsToGuards)
        {
            appendPhase(new IncrementalCanonicalizerPhase<>(canonicalizer, new ConvertDeoptimizeToGuardPhase()));
        }

        if (GraalOptions.conditionalElimination)
        {
            appendPhase(new IterativeConditionalEliminationPhase(canonicalizer, false));
        }

        LoopPolicies loopPolicies = createLoopPolicies();
        if (GraalOptions.fullUnroll)
        {
            appendPhase(new LoopFullUnrollPhase(canonicalizer, loopPolicies));
        }

        if (GraalOptions.optLoopTransform)
        {
            if (GraalOptions.loopPeeling)
            {
                appendPhase(new LoopPeelingPhase(loopPolicies));
            }
            if (GraalOptions.loopUnswitch)
            {
                appendPhase(new LoopUnswitchingPhase(loopPolicies));
            }
        }

        appendPhase(canonicalizer);

        if (GraalOptions.partialEscapeAnalysis)
        {
            appendPhase(new PartialEscapePhase(true, canonicalizer));
        }

        if (GraalOptions.optReadElimination)
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
