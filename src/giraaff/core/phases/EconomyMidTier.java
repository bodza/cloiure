package giraaff.core.phases;

import giraaff.core.common.GraalOptions;
import giraaff.nodes.spi.LoweringTool;
import giraaff.options.OptionValues;
import giraaff.phases.PhaseSuite;
import giraaff.phases.common.CanonicalizerPhase;
import giraaff.phases.common.FrameStateAssignmentPhase;
import giraaff.phases.common.GuardLoweringPhase;
import giraaff.phases.common.LoopSafepointInsertionPhase;
import giraaff.phases.common.LoweringPhase;
import giraaff.phases.common.RemoveValueProxyPhase;
import giraaff.phases.tiers.MidTierContext;

public class EconomyMidTier extends PhaseSuite<MidTierContext>
{
    public EconomyMidTier(OptionValues options)
    {
        CanonicalizerPhase canonicalizer = new CanonicalizerPhase();
        appendPhase(new RemoveValueProxyPhase());
        appendPhase(new LoopSafepointInsertionPhase());
        appendPhase(new GuardLoweringPhase());
        appendPhase(new LoweringPhase(canonicalizer, LoweringTool.StandardLoweringStage.MID_TIER));
        appendPhase(new FrameStateAssignmentPhase());
    }
}
