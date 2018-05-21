package graalvm.compiler.core.phases;

import static graalvm.compiler.core.common.GraalOptions.ImmutableCode;
import static graalvm.compiler.phases.common.DeadCodeEliminationPhase.Optionality.Required;

import graalvm.compiler.core.common.GraalOptions;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.PhaseSuite;
import graalvm.compiler.phases.common.CanonicalizerPhase;
import graalvm.compiler.phases.common.DeadCodeEliminationPhase;
import graalvm.compiler.phases.common.ExpandLogicPhase;
import graalvm.compiler.phases.common.FixReadsPhase;
import graalvm.compiler.phases.common.LoweringPhase;
import graalvm.compiler.phases.common.PropagateDeoptimizeProbabilityPhase;
import graalvm.compiler.phases.common.UseTrappingNullChecksPhase;
import graalvm.compiler.phases.schedule.SchedulePhase;
import graalvm.compiler.phases.schedule.SchedulePhase.SchedulingStrategy;
import graalvm.compiler.phases.tiers.LowTierContext;

public class LowTier extends PhaseSuite<LowTierContext>
{
    public LowTier(OptionValues options)
    {
        CanonicalizerPhase canonicalizer = new CanonicalizerPhase();
        CanonicalizerPhase canonicalizerWithoutGVN = new CanonicalizerPhase();
        canonicalizerWithoutGVN.disableGVN();
        if (ImmutableCode.getValue(options))
        {
            canonicalizer.disableReadCanonicalization();
            canonicalizerWithoutGVN.disableReadCanonicalization();
        }

        appendPhase(new LoweringPhase(canonicalizer, LoweringTool.StandardLoweringStage.LOW_TIER));

        appendPhase(new ExpandLogicPhase());

        appendPhase(new FixReadsPhase(true, new SchedulePhase(GraalOptions.StressTestEarlyReads.getValue(options) ? SchedulingStrategy.EARLIEST : SchedulingStrategy.LATEST_OUT_OF_LOOPS)));

        appendPhase(canonicalizerWithoutGVN);

        appendPhase(new UseTrappingNullChecksPhase());

        appendPhase(new DeadCodeEliminationPhase(Required));

        appendPhase(new PropagateDeoptimizeProbabilityPhase());

        appendPhase(new SchedulePhase(SchedulePhase.SchedulingStrategy.FINAL_SCHEDULE));
    }
}
