package giraaff.core.phases;

import giraaff.core.common.GraalOptions;
import giraaff.nodes.spi.LoweringTool;
import giraaff.phases.PhaseSuite;
import giraaff.phases.common.CanonicalizerPhase;
import giraaff.phases.common.DeadCodeEliminationPhase;
import giraaff.phases.common.ExpandLogicPhase;
import giraaff.phases.common.FixReadsPhase;
import giraaff.phases.common.LoweringPhase;
import giraaff.phases.common.PropagateDeoptimizeProbabilityPhase;
import giraaff.phases.common.UseTrappingNullChecksPhase;
import giraaff.phases.schedule.SchedulePhase;
import giraaff.phases.tiers.LowTierContext;

// @class LowTier
public final class LowTier extends PhaseSuite<LowTierContext>
{
    // @cons LowTier
    public LowTier()
    {
        super();
        CanonicalizerPhase __canonicalizer = new CanonicalizerPhase();

        appendPhase(new LoweringPhase(__canonicalizer, LoweringTool.StandardLoweringStage.LOW_TIER));
        appendPhase(new ExpandLogicPhase());
        appendPhase(new FixReadsPhase(true, new SchedulePhase(GraalOptions.stressTestEarlyReads ? SchedulePhase.SchedulingStrategy.EARLIEST : SchedulePhase.SchedulingStrategy.LATEST_OUT_OF_LOOPS)));

        CanonicalizerPhase __canonicalizerWithoutGVN = new CanonicalizerPhase();
        __canonicalizerWithoutGVN.disableGVN();
        appendPhase(__canonicalizerWithoutGVN);

        appendPhase(new UseTrappingNullChecksPhase());
        appendPhase(new DeadCodeEliminationPhase(DeadCodeEliminationPhase.Optionality.Required));
        appendPhase(new PropagateDeoptimizeProbabilityPhase());
        appendPhase(new SchedulePhase(SchedulePhase.SchedulingStrategy.FINAL_SCHEDULE));
    }
}
