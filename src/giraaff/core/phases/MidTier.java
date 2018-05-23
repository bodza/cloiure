package giraaff.core.phases;

import giraaff.core.common.GraalOptions;
import giraaff.loop.DefaultLoopPolicies;
import giraaff.loop.LoopPolicies;
import giraaff.loop.phases.LoopPartialUnrollPhase;
import giraaff.loop.phases.LoopSafepointEliminationPhase;
import giraaff.loop.phases.ReassociateInvariantPhase;
import giraaff.nodes.spi.LoweringTool;
import giraaff.options.OptionValues;
import giraaff.phases.PhaseSuite;
import giraaff.phases.common.CanonicalizerPhase;
import giraaff.phases.common.DeoptimizationGroupingPhase;
import giraaff.phases.common.FloatingReadPhase;
import giraaff.phases.common.FrameStateAssignmentPhase;
import giraaff.phases.common.GuardLoweringPhase;
import giraaff.phases.common.IncrementalCanonicalizerPhase;
import giraaff.phases.common.IterativeConditionalEliminationPhase;
import giraaff.phases.common.LockEliminationPhase;
import giraaff.phases.common.LoopSafepointInsertionPhase;
import giraaff.phases.common.LoweringPhase;
import giraaff.phases.tiers.MidTierContext;

public class MidTier extends PhaseSuite<MidTierContext>
{
    public MidTier(OptionValues options)
    {
        CanonicalizerPhase canonicalizer = new CanonicalizerPhase();

        appendPhase(new LockEliminationPhase());

        if (GraalOptions.OptFloatingReads.getValue(options))
        {
            appendPhase(new IncrementalCanonicalizerPhase<>(canonicalizer, new FloatingReadPhase()));
        }

        if (GraalOptions.ConditionalElimination.getValue(options))
        {
            appendPhase(new IterativeConditionalEliminationPhase(canonicalizer, true));
        }

        appendPhase(new LoopSafepointEliminationPhase());
        appendPhase(new LoopSafepointInsertionPhase());
        appendPhase(new GuardLoweringPhase());
        appendPhase(new LoweringPhase(canonicalizer, LoweringTool.StandardLoweringStage.MID_TIER));
        appendPhase(new FrameStateAssignmentPhase());

        LoopPolicies loopPolicies = createLoopPolicies();
        if (GraalOptions.OptLoopTransform.getValue(options))
        {
            if (GraalOptions.PartialUnroll.getValue(options))
            {
                appendPhase(new LoopPartialUnrollPhase(loopPolicies, canonicalizer));
            }
        }
        if (GraalOptions.ReassociateInvariants.getValue(options))
        {
            appendPhase(new ReassociateInvariantPhase());
        }

        if (GraalOptions.OptDeoptimizationGrouping.getValue(options))
        {
            appendPhase(new DeoptimizationGroupingPhase());
        }

        appendPhase(canonicalizer);
    }

    public LoopPolicies createLoopPolicies()
    {
        return new DefaultLoopPolicies();
    }
}
