package giraaff.core.phases;

import giraaff.lir.phases.AllocationPhase.AllocationContext;
import giraaff.lir.phases.EconomyAllocationStage;
import giraaff.lir.phases.EconomyPostAllocationOptimizationStage;
import giraaff.lir.phases.EconomyPreAllocationOptimizationStage;
import giraaff.lir.phases.LIRPhaseSuite;
import giraaff.lir.phases.PostAllocationOptimizationPhase.PostAllocationOptimizationContext;
import giraaff.lir.phases.PreAllocationOptimizationPhase.PreAllocationOptimizationContext;
import giraaff.options.OptionValues;
import giraaff.phases.PhaseSuite;
import giraaff.phases.tiers.CompilerConfiguration;
import giraaff.phases.tiers.HighTierContext;
import giraaff.phases.tiers.LowTierContext;
import giraaff.phases.tiers.MidTierContext;

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
