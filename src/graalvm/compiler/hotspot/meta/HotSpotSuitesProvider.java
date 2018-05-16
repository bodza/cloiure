package graalvm.compiler.hotspot.meta;

import static graalvm.compiler.core.common.GraalOptions.GeneratePIC;
import static graalvm.compiler.core.common.GraalOptions.ImmutableCode;
import static graalvm.compiler.core.common.GraalOptions.VerifyPhases;
import static graalvm.compiler.core.phases.HighTier.Options.Inline;

import java.util.ListIterator;
import graalvm.compiler.debug.Assertions;

import graalvm.compiler.hotspot.GraalHotSpotVMConfig;
import graalvm.compiler.hotspot.HotSpotBackend;
import graalvm.compiler.hotspot.HotSpotGraalRuntimeProvider;
import graalvm.compiler.hotspot.HotSpotInstructionProfiling;
import graalvm.compiler.hotspot.lir.VerifyMaxRegisterSizePhase;
import graalvm.compiler.hotspot.phases.AheadOfTimeVerificationPhase;
import graalvm.compiler.hotspot.phases.LoadJavaMirrorWithKlassPhase;
import graalvm.compiler.hotspot.phases.WriteBarrierAdditionPhase;
import graalvm.compiler.hotspot.phases.WriteBarrierVerificationPhase;
import graalvm.compiler.hotspot.phases.aot.AOTInliningPolicy;
import graalvm.compiler.hotspot.phases.aot.EliminateRedundantInitializationPhase;
import graalvm.compiler.hotspot.phases.aot.ReplaceConstantNodesPhase;
import graalvm.compiler.hotspot.phases.profiling.FinalizeProfileNodesPhase;
import graalvm.compiler.java.GraphBuilderPhase;
import graalvm.compiler.java.SuitesProviderBase;
import graalvm.compiler.lir.phases.LIRSuites;
import graalvm.compiler.nodes.EncodedGraph;
import graalvm.compiler.nodes.GraphEncoder;
import graalvm.compiler.nodes.SimplifyingGraphDecoder;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.StructuredGraph.AllowAssumptions;
import graalvm.compiler.nodes.graphbuilderconf.GraphBuilderConfiguration;
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

        if (ImmutableCode.getValue(options))
        {
            // lowering introduces class constants, therefore it must be after lowering
            ret.getHighTier().appendPhase(new LoadJavaMirrorWithKlassPhase(config));
            if (VerifyPhases.getValue(options))
            {
                ret.getHighTier().appendPhase(new AheadOfTimeVerificationPhase());
            }
            if (GeneratePIC.getValue(options))
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
                if (Inline.getValue(options))
                {
                    ListIterator<BasePhase<? super HighTierContext>> iter = ret.getHighTier().findPhase(InliningPhase.class);
                    InliningPhase inlining = (InliningPhase) iter.previous();
                    CanonicalizerPhase canonicalizer = inlining.getCanonicalizer();
                    iter.set(new InliningPhase(new AOTInliningPolicy(null), canonicalizer));
                }
            }
        }

        ret.getMidTier().appendPhase(new WriteBarrierAdditionPhase(config));
        if (VerifyPhases.getValue(options))
        {
            ret.getMidTier().appendPhase(new WriteBarrierVerificationPhase(config));
        }

        return ret;
    }

    protected PhaseSuite<HighTierContext> createGraphBuilderSuite()
    {
        PhaseSuite<HighTierContext> suite = defaultSuitesCreator.getDefaultGraphBuilderSuite().copy();
        assert appendGraphEncoderTest(suite);
        return suite;
    }

    /**
     * When assertions are enabled, we encode and decode every parsed graph, to ensure that the
     * encoding and decoding process work correctly. The decoding performs canonicalization during
     * decoding, so the decoded graph can be different than the encoded graph - we cannot check them
     * for equality here. However, the encoder {@link GraphEncoder#verifyEncoding verifies the
     * encoding itself}, i.e., performs a decoding without canonicalization and checks the graphs
     * for equality.
     */
    private boolean appendGraphEncoderTest(PhaseSuite<HighTierContext> suite)
    {
        suite.appendPhase(new BasePhase<HighTierContext>()
        {
            @Override
            protected void run(StructuredGraph graph, HighTierContext context)
            {
                EncodedGraph encodedGraph = GraphEncoder.encodeSingleGraph(graph, runtime.getTarget().arch);

                StructuredGraph targetGraph = new StructuredGraph.Builder(graph.getOptions(), graph.getDebug(), AllowAssumptions.YES).method(graph.method()).build();
                SimplifyingGraphDecoder graphDecoder = new SimplifyingGraphDecoder(runtime.getTarget().arch, targetGraph, context.getMetaAccess(), context.getConstantReflection(), context.getConstantFieldProvider(), context.getStampProvider(), !ImmutableCode.getValue(graph.getOptions()));

                if (graph.trackNodeSourcePosition())
                {
                    targetGraph.setTrackNodeSourcePosition();
                }
                graphDecoder.decode(encodedGraph);
            }

            @Override
            protected CharSequence getName()
            {
                return "VerifyEncodingDecoding";
            }
        });
        return true;
    }

    /**
     * Modifies a given {@link GraphBuilderConfiguration} to record per node source information.
     *
     * @param gbs the current graph builder suite to modify
     */
    public static PhaseSuite<HighTierContext> withNodeSourcePosition(PhaseSuite<HighTierContext> gbs)
    {
        PhaseSuite<HighTierContext> newGbs = gbs.copy();
        GraphBuilderPhase graphBuilderPhase = (GraphBuilderPhase) newGbs.findPhase(GraphBuilderPhase.class).previous();
        GraphBuilderConfiguration graphBuilderConfig = graphBuilderPhase.getGraphBuilderConfig();
        GraphBuilderPhase newGraphBuilderPhase = new GraphBuilderPhase(graphBuilderConfig.withNodeSourcePosition(true));
        newGbs.findPhase(GraphBuilderPhase.class).set(newGraphBuilderPhase);
        return newGbs;
    }

    @Override
    public LIRSuites createLIRSuites(OptionValues options)
    {
        LIRSuites suites = defaultSuitesCreator.createLIRSuites(options);
        String profileInstructions = HotSpotBackend.Options.ASMInstructionProfiling.getValue(options);
        if (profileInstructions != null)
        {
            suites.getPostAllocationOptimizationStage().appendPhase(new HotSpotInstructionProfiling(profileInstructions));
        }
        if (Assertions.detailedAssertionsEnabled(options))
        {
            suites.getPostAllocationOptimizationStage().appendPhase(new VerifyMaxRegisterSizePhase(config.maxVectorSize));
        }
        return suites;
    }
}
