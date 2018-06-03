package giraaff.core.phases;

import giraaff.core.common.GraalOptions;
import giraaff.loop.DefaultLoopPolicies;
import giraaff.loop.LoopPolicies;
import giraaff.loop.phases.LoopPartialUnrollPhase;
import giraaff.loop.phases.LoopSafepointEliminationPhase;
import giraaff.loop.phases.ReassociateInvariantPhase;
import giraaff.nodes.spi.LoweringTool;
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

// @class MidTier
public final class MidTier extends PhaseSuite<MidTierContext>
{
    // @cons
    public MidTier()
    {
        super();
        CanonicalizerPhase __canonicalizer = new CanonicalizerPhase();

        appendPhase(new LockEliminationPhase());

        if (GraalOptions.optFloatingReads)
        {
            appendPhase(new IncrementalCanonicalizerPhase<>(__canonicalizer, new FloatingReadPhase()));
        }

        if (GraalOptions.conditionalElimination)
        {
            appendPhase(new IterativeConditionalEliminationPhase(__canonicalizer, true));
        }

        appendPhase(new LoopSafepointEliminationPhase());
        appendPhase(new LoopSafepointInsertionPhase());
        appendPhase(new GuardLoweringPhase());
        appendPhase(new LoweringPhase(__canonicalizer, LoweringTool.StandardLoweringStage.MID_TIER));
        appendPhase(new FrameStateAssignmentPhase());

        LoopPolicies __loopPolicies = createLoopPolicies();
        if (GraalOptions.optLoopTransform)
        {
            if (GraalOptions.partialUnroll)
            {
                appendPhase(new LoopPartialUnrollPhase(__loopPolicies, __canonicalizer));
            }
        }
        if (GraalOptions.reassociateInvariants)
        {
            appendPhase(new ReassociateInvariantPhase());
        }

        if (GraalOptions.optDeoptimizationGrouping)
        {
            appendPhase(new DeoptimizationGroupingPhase());
        }

        appendPhase(__canonicalizer);
    }

    public LoopPolicies createLoopPolicies()
    {
        return new DefaultLoopPolicies();
    }
}
