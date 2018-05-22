package giraaff.core.phases;

import giraaff.core.common.GraalOptions;
import giraaff.nodes.spi.LoweringTool;
import giraaff.options.OptionValues;
import giraaff.phases.PhaseSuite;
import giraaff.phases.common.CanonicalizerPhase;
import giraaff.phases.common.LoweringPhase;
import giraaff.phases.tiers.HighTierContext;

public class EconomyHighTier extends PhaseSuite<HighTierContext>
{
    public EconomyHighTier(OptionValues options)
    {
        CanonicalizerPhase canonicalizer = new CanonicalizerPhase();
        if (GraalOptions.ImmutableCode.getValue(options))
        {
            canonicalizer.disableReadCanonicalization();
        }

        appendPhase(canonicalizer);
        appendPhase(new LoweringPhase(canonicalizer, LoweringTool.StandardLoweringStage.HIGH_TIER));
    }
}
