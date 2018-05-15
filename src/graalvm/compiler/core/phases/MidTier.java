package graalvm.compiler.core.phases;

import static graalvm.compiler.core.common.GraalOptions.ConditionalElimination;
import static graalvm.compiler.core.common.GraalOptions.ImmutableCode;
import static graalvm.compiler.core.common.GraalOptions.OptDeoptimizationGrouping;
import static graalvm.compiler.core.common.GraalOptions.OptFloatingReads;
import static graalvm.compiler.core.common.GraalOptions.OptLoopTransform;
import static graalvm.compiler.core.common.GraalOptions.PartialUnroll;
import static graalvm.compiler.core.common.GraalOptions.ReassociateInvariants;
import static graalvm.compiler.core.common.GraalOptions.VerifyHeapAtReturn;

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

public class MidTier extends PhaseSuite<MidTierContext> {

    public MidTier(OptionValues options) {
        CanonicalizerPhase canonicalizer = new CanonicalizerPhase();
        if (ImmutableCode.getValue(options)) {
            canonicalizer.disableReadCanonicalization();
        }

        appendPhase(new LockEliminationPhase());

        if (OptFloatingReads.getValue(options)) {
            appendPhase(new IncrementalCanonicalizerPhase<>(canonicalizer, new FloatingReadPhase()));
        }

        if (ConditionalElimination.getValue(options)) {
            appendPhase(new IterativeConditionalEliminationPhase(canonicalizer, true));
        }

        appendPhase(new LoopSafepointEliminationPhase());

        appendPhase(new LoopSafepointInsertionPhase());

        appendPhase(new GuardLoweringPhase());

        if (VerifyHeapAtReturn.getValue(options)) {
            appendPhase(new VerifyHeapAtReturnPhase());
        }

        appendPhase(new LoweringPhase(canonicalizer, LoweringTool.StandardLoweringStage.MID_TIER));

        appendPhase(new FrameStateAssignmentPhase());

        LoopPolicies loopPolicies = createLoopPolicies();
        if (OptLoopTransform.getValue(options)) {
            if (PartialUnroll.getValue(options)) {
                appendPhase(new LoopPartialUnrollPhase(loopPolicies, canonicalizer));
            }
        }
        if (ReassociateInvariants.getValue(options)) {
            appendPhase(new ReassociateInvariantPhase());
        }

        if (OptDeoptimizationGrouping.getValue(options)) {
            appendPhase(new DeoptimizationGroupingPhase());
        }

        appendPhase(canonicalizer);
    }

    public LoopPolicies createLoopPolicies() {
        return new DefaultLoopPolicies();
    }
}
