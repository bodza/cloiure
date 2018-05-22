package giraaff.hotspot.meta;

import java.util.ListIterator;

import giraaff.core.common.GraalOptions;
import giraaff.core.phases.HighTier.Options;
import giraaff.hotspot.GraalHotSpotVMConfig;
import giraaff.hotspot.HotSpotGraalRuntimeProvider;
import giraaff.hotspot.phases.LoadJavaMirrorWithKlassPhase;
import giraaff.hotspot.phases.WriteBarrierAdditionPhase;
import giraaff.hotspot.phases.aot.AOTInliningPolicy;
import giraaff.hotspot.phases.aot.EliminateRedundantInitializationPhase;
import giraaff.hotspot.phases.aot.ReplaceConstantNodesPhase;
import giraaff.hotspot.phases.profiling.FinalizeProfileNodesPhase;
import giraaff.java.SuitesProviderBase;
import giraaff.lir.phases.LIRSuites;
import giraaff.options.OptionValues;
import giraaff.phases.BasePhase;
import giraaff.phases.PhaseSuite;
import giraaff.phases.common.CanonicalizerPhase;
import giraaff.phases.common.LoweringPhase;
import giraaff.phases.common.inlining.InliningPhase;
import giraaff.phases.tiers.HighTierContext;
import giraaff.phases.tiers.MidTierContext;
import giraaff.phases.tiers.Suites;
import giraaff.phases.tiers.SuitesCreator;

/**
 * HotSpot implementation of {@link SuitesCreator}.
 */
public class HotSpotSuitesProvider extends SuitesProviderBase
{
    protected final GraalHotSpotVMConfig config;
    protected final HotSpotGraalRuntimeProvider runtime;

    private final SuitesCreator defaultSuitesCreator;

    public HotSpotSuitesProvider(SuitesCreator defaultSuitesCreator, GraalHotSpotVMConfig config, HotSpotGraalRuntimeProvider runtime)
    {
        this.defaultSuitesCreator = defaultSuitesCreator;
        this.config = config;
        this.runtime = runtime;
        this.defaultGraphBuilderSuite = createGraphBuilderSuite();
    }

    @Override
    public Suites createSuites(OptionValues options)
    {
        Suites ret = defaultSuitesCreator.createSuites(options);

        if (GraalOptions.ImmutableCode.getValue(options))
        {
            // lowering introduces class constants, therefore it must be after lowering
            ret.getHighTier().appendPhase(new LoadJavaMirrorWithKlassPhase(config));
            if (GraalOptions.GeneratePIC.getValue(options))
            {
                ListIterator<BasePhase<? super HighTierContext>> highTierLowering = ret.getHighTier().findPhase(LoweringPhase.class);
                highTierLowering.previous();
                highTierLowering.add(new EliminateRedundantInitializationPhase());
                if (HotSpotAOTProfilingPlugin.Options.TieredAOT.getValue(options))
                {
                    highTierLowering.add(new FinalizeProfileNodesPhase(HotSpotAOTProfilingPlugin.Options.TierAInvokeInlineeNotifyFreqLog.getValue(options)));
                }
                ListIterator<BasePhase<? super MidTierContext>> midTierLowering = ret.getMidTier().findPhase(LoweringPhase.class);
                midTierLowering.add(new ReplaceConstantNodesPhase());

                // Replace inlining policy
                if (Options.Inline.getValue(options))
                {
                    ListIterator<BasePhase<? super HighTierContext>> iter = ret.getHighTier().findPhase(InliningPhase.class);
                    InliningPhase inlining = (InliningPhase) iter.previous();
                    CanonicalizerPhase canonicalizer = inlining.getCanonicalizer();
                    iter.set(new InliningPhase(new AOTInliningPolicy(null), canonicalizer));
                }
            }
        }

        ret.getMidTier().appendPhase(new WriteBarrierAdditionPhase(config));

        return ret;
    }

    protected PhaseSuite<HighTierContext> createGraphBuilderSuite()
    {
        PhaseSuite<HighTierContext> suite = defaultSuitesCreator.getDefaultGraphBuilderSuite().copy();
        return suite;
    }

    @Override
    public LIRSuites createLIRSuites(OptionValues options)
    {
        return defaultSuitesCreator.createLIRSuites(options);
    }
}
