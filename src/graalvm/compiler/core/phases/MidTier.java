package graalvm.compiler.core.phases;

import graalvm.compiler.core.common.GraalOptions;
import graalvm.compiler.loop.DefaultLoopPolicies;
import graalvm.compiler.loop.LoopPolicies;
import graalvm.compiler.loop.phases.LoopPartialUnrollPhase;
import graalvm.compiler.loop.phases.LoopSafepointEliminationPhase;
import graalvm.compiler.loop.phases.ReassociateInvariantPhase;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.PhaseSuite;
import graalvm.compiler.phases.common.CanonicalizerPhase;
import graalvm.compiler.phases.common.DeoptimizationGroupingPhase;
import graalvm.compiler.phases.common.FloatingReadPhase;
import graalvm.compiler.phases.common.FrameStateAssignmentPhase;
import graalvm.compiler.phases.common.GuardLoweringPhase;
import graalvm.compiler.phases.common.IncrementalCanonicalizerPhase;
import graalvm.compiler.phases.common.IterativeConditionalEliminationPhase;
import graalvm.compiler.phases.common.LockEliminationPhase;
import graalvm.compiler.phases.common.LoopSafepointInsertionPhase;
import graalvm.compiler.phases.common.LoweringPhase;
import graalvm.compiler.phases.common.VerifyHeapAtReturnPhase;
import graalvm.compiler.phases.tiers.MidTierContext;

public class MidTier extends PhaseSuite<MidTierContext>
{
    public MidTier(OptionValues options)
    {
        CanonicalizerPhase canonicalizer = new CanonicalizerPhase();
        if (GraalOptions.ImmutableCode.getValue(options))
        {
            canonicalizer.disableReadCanonicalization();
        }

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

        if (GraalOptions.VerifyHeapAtReturn.getValue(options))
        {
            appendPhase(new VerifyHeapAtReturnPhase());
        }

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
