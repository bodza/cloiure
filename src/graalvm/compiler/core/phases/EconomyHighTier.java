package graalvm.compiler.core.phases;

import static graalvm.compiler.core.common.GraalOptions.ImmutableCode;

import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.PhaseSuite;
import graalvm.compiler.phases.common.CanonicalizerPhase;
import graalvm.compiler.phases.common.LoweringPhase;
import graalvm.compiler.phases.tiers.HighTierContext;

public class EconomyHighTier extends PhaseSuite<HighTierContext> {

    public EconomyHighTier(OptionValues options) {
        CanonicalizerPhase canonicalizer = new CanonicalizerPhase();
        if (ImmutableCode.getValue(options)) {
            canonicalizer.disableReadCanonicalization();
        }

        appendPhase(canonicalizer);
        appendPhase(new LoweringPhase(canonicalizer, LoweringTool.StandardLoweringStage.HIGH_TIER));
    }
}
