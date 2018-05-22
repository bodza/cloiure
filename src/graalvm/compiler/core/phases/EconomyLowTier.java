package graalvm.compiler.core.phases;

import graalvm.compiler.core.common.GraalOptions;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.PhaseSuite;
import graalvm.compiler.phases.common.CanonicalizerPhase;
import graalvm.compiler.phases.common.ExpandLogicPhase;
import graalvm.compiler.phases.common.LoweringPhase;
import graalvm.compiler.phases.schedule.SchedulePhase;
import graalvm.compiler.phases.tiers.LowTierContext;

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
