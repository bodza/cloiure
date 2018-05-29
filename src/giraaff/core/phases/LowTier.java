package giraaff.core.phases;

import giraaff.core.common.GraalOptions;
import giraaff.nodes.spi.LoweringTool;
import giraaff.options.OptionValues;
import giraaff.phases.PhaseSuite;
import giraaff.phases.common.CanonicalizerPhase;
import giraaff.phases.common.DeadCodeEliminationPhase;
import giraaff.phases.common.DeadCodeEliminationPhase.Optionality;
import giraaff.phases.common.ExpandLogicPhase;
import giraaff.phases.common.FixReadsPhase;
import giraaff.phases.common.LoweringPhase;
import giraaff.phases.common.PropagateDeoptimizeProbabilityPhase;
import giraaff.phases.common.UseTrappingNullChecksPhase;
import giraaff.phases.schedule.SchedulePhase;
import giraaff.phases.schedule.SchedulePhase.SchedulingStrategy;
import giraaff.phases.tiers.LowTierContext;

// @class LowTier
public final class LowTier extends PhaseSuite<LowTierContext>
{
    // @cons
    public LowTier(OptionValues options)
    {
        super();
        CanonicalizerPhase canonicalizer = new CanonicalizerPhase();

        appendPhase(new LoweringPhase(canonicalizer, LoweringTool.StandardLoweringStage.LOW_TIER));
        appendPhase(new ExpandLogicPhase());
        appendPhase(new FixReadsPhase(true, new SchedulePhase(GraalOptions.StressTestEarlyReads.getValue(options) ? SchedulingStrategy.EARLIEST : SchedulingStrategy.LATEST_OUT_OF_LOOPS)));

        CanonicalizerPhase canonicalizerWithoutGVN = new CanonicalizerPhase();
        canonicalizerWithoutGVN.disableGVN();
        appendPhase(canonicalizerWithoutGVN);

        appendPhase(new UseTrappingNullChecksPhase());
        appendPhase(new DeadCodeEliminationPhase(Optionality.Required));
        appendPhase(new PropagateDeoptimizeProbabilityPhase());
        appendPhase(new SchedulePhase(SchedulePhase.SchedulingStrategy.FINAL_SCHEDULE));
    }
}
