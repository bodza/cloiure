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
    // @cons HighTier
    public HighTier()
    {
        super();
        CanonicalizerPhase __canonicalizer = new CanonicalizerPhase();
        appendPhase(__canonicalizer);

        if (GraalOptions.inline)
        {
            appendPhase(new InliningPhase(__canonicalizer));
            appendPhase(new DeadCodeEliminationPhase(DeadCodeEliminationPhase.Optionality.Optional));
        }

        if (GraalOptions.optConvertDeoptsToGuards)
        {
            appendPhase(new IncrementalCanonicalizerPhase<>(__canonicalizer, new ConvertDeoptimizeToGuardPhase()));
        }

        if (GraalOptions.conditionalElimination)
        {
            appendPhase(new IterativeConditionalEliminationPhase(__canonicalizer, false));
        }

        LoopPolicies __loopPolicies = createLoopPolicies();
        if (GraalOptions.fullUnroll)
        {
            appendPhase(new LoopFullUnrollPhase(__canonicalizer, __loopPolicies));
        }

        if (GraalOptions.optLoopTransform)
        {
            if (GraalOptions.loopPeeling)
            {
                appendPhase(new LoopPeelingPhase(__loopPolicies));
            }
            if (GraalOptions.loopUnswitch)
            {
                appendPhase(new LoopUnswitchingPhase(__loopPolicies));
            }
        }

        appendPhase(__canonicalizer);

        if (GraalOptions.partialEscapeAnalysis)
        {
            appendPhase(new PartialEscapePhase(true, __canonicalizer));
        }

        if (GraalOptions.optReadElimination)
        {
            appendPhase(new EarlyReadEliminationPhase(__canonicalizer));
        }

        appendPhase(new RemoveValueProxyPhase());
        appendPhase(new LoweringPhase(__canonicalizer, LoweringTool.StandardLoweringStage.HIGH_TIER));
    }

    public LoopPolicies createLoopPolicies()
    {
        return new DefaultLoopPolicies();
    }
}
