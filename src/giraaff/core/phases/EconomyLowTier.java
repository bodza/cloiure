package giraaff.core.phases;

import giraaff.core.common.GraalOptions;
import giraaff.nodes.spi.LoweringTool;
import giraaff.options.OptionValues;
import giraaff.phases.PhaseSuite;
import giraaff.phases.common.CanonicalizerPhase;
import giraaff.phases.common.ExpandLogicPhase;
import giraaff.phases.common.LoweringPhase;
import giraaff.phases.schedule.SchedulePhase;
import giraaff.phases.tiers.LowTierContext;

public class EconomyLowTier extends PhaseSuite<LowTierContext>
{
    public EconomyLowTier(OptionValues options)
    {
        CanonicalizerPhase canonicalizer = new CanonicalizerPhase();
        if (GraalOptions.ImmutableCode.getValue(options))
        {
            canonicalizer.disableReadCanonicalization();
        }

        appendPhase(new LoweringPhase(canonicalizer, LoweringTool.StandardLoweringStage.LOW_TIER));

        appendPhase(new ExpandLogicPhase());

        appendPhase(new SchedulePhase(SchedulePhase.SchedulingStrategy.FINAL_SCHEDULE));
    }
}
