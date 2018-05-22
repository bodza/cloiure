package graalvm.compiler.hotspot.meta;

import java.util.ListIterator;

import graalvm.compiler.core.common.GraalOptions;
import graalvm.compiler.core.phases.HighTier.Options;
import graalvm.compiler.hotspot.GraalHotSpotVMConfig;
import graalvm.compiler.hotspot.HotSpotGraalRuntimeProvider;
import graalvm.compiler.hotspot.phases.LoadJavaMirrorWithKlassPhase;
import graalvm.compiler.hotspot.phases.WriteBarrierAdditionPhase;
import graalvm.compiler.hotspot.phases.aot.AOTInliningPolicy;
import graalvm.compiler.hotspot.phases.aot.EliminateRedundantInitializationPhase;
import graalvm.compiler.hotspot.phases.aot.ReplaceConstantNodesPhase;
import graalvm.compiler.hotspot.phases.profiling.FinalizeProfileNodesPhase;
import graalvm.compiler.java.SuitesProviderBase;
import graalvm.compiler.lir.phases.LIRSuites;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.BasePhase;
import graalvm.compiler.phases.PhaseSuite;
import graalvm.compiler.phases.common.CanonicalizerPhase;
import graalvm.compiler.phases.common.LoweringPhase;
import graalvm.compiler.phases.common.inlining.InliningPhase;
import graalvm.compiler.phases.tiers.HighTierContext;
import graalvm.compiler.phases.tiers.MidTierContext;
import graalvm.compiler.phases.tiers.Suites;
import graalvm.compiler.phases.tiers.SuitesCreator;

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
