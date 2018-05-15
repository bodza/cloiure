package graalvm.compiler.core.phases;

import static graalvm.compiler.core.common.GraalOptions.ImmutableCode;

import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.PhaseSuite;
import graalvm.compiler.phases.common.CanonicalizerPhase;
import graalvm.compiler.phases.common.FrameStateAssignmentPhase;
import graalvm.compiler.phases.common.GuardLoweringPhase;
import graalvm.compiler.phases.common.LoopSafepointInsertionPhase;
import graalvm.compiler.phases.common.LoweringPhase;
import graalvm.compiler.phases.common.RemoveValueProxyPhase;
import graalvm.compiler.phases.tiers.MidTierContext;

public class EconomyMidTier extends PhaseSuite<MidTierContext> {

    public EconomyMidTier(OptionValues options) {
        CanonicalizerPhase canonicalizer = new CanonicalizerPhase();
        if (ImmutableCode.getValue(options)) {
            canonicalizer.disableReadCanonicalization();
        }
        appendPhase(new RemoveValueProxyPhase());

        appendPhase(new LoopSafepointInsertionPhase());

        appendPhase(new GuardLoweringPhase());

        appendPhase(new LoweringPhase(canonicalizer, LoweringTool.StandardLoweringStage.MID_TIER));

        appendPhase(new FrameStateAssignmentPhase());
    }
}
