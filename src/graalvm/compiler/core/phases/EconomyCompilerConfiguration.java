package graalvm.compiler.core.phases;

import graalvm.compiler.lir.phases.AllocationPhase.AllocationContext;
import graalvm.compiler.lir.phases.EconomyAllocationStage;
import graalvm.compiler.lir.phases.EconomyPostAllocationOptimizationStage;
import graalvm.compiler.lir.phases.EconomyPreAllocationOptimizationStage;
import graalvm.compiler.lir.phases.LIRPhaseSuite;
import graalvm.compiler.lir.phases.PostAllocationOptimizationPhase.PostAllocationOptimizationContext;
import graalvm.compiler.lir.phases.PreAllocationOptimizationPhase.PreAllocationOptimizationContext;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.PhaseSuite;
import graalvm.compiler.phases.tiers.CompilerConfiguration;
import graalvm.compiler.phases.tiers.HighTierContext;
import graalvm.compiler.phases.tiers.LowTierContext;
import graalvm.compiler.phases.tiers.MidTierContext;

/**
 * A compiler configuration that performs fewer Graal IR optimizations while using the same backend
 * as the {@link CommunityCompilerConfiguration}.
 */
public class EconomyCompilerConfiguration implements CompilerConfiguration
{
    @Override
    public PhaseSuite<HighTierContext> createHighTier(OptionValues options)
    {
        return new EconomyHighTier(options);
    }

    @Override
    public PhaseSuite<MidTierContext> createMidTier(OptionValues options)
    {
        return new EconomyMidTier(options);
    }

    @Override
    public PhaseSuite<LowTierContext> createLowTier(OptionValues options)
    {
        return new EconomyLowTier(options);
    }

    @Override
    public LIRPhaseSuite<PreAllocationOptimizationContext> createPreAllocationOptimizationStage(OptionValues options)
    {
        return new EconomyPreAllocationOptimizationStage();
    }

    @Override
    public LIRPhaseSuite<AllocationContext> createAllocationStage(OptionValues options)
    {
        return new EconomyAllocationStage(options);
    }

    @Override
    public LIRPhaseSuite<PostAllocationOptimizationContext> createPostAllocationOptimizationStage(OptionValues options)
    {
        return new EconomyPostAllocationOptimizationStage();
    }
}
