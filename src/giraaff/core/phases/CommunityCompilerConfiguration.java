package giraaff.core.phases;

import giraaff.lir.phases.AllocationPhase.AllocationContext;
import giraaff.lir.phases.AllocationStage;
import giraaff.lir.phases.LIRPhaseSuite;
import giraaff.lir.phases.PostAllocationOptimizationPhase.PostAllocationOptimizationContext;
import giraaff.lir.phases.PostAllocationOptimizationStage;
import giraaff.lir.phases.PreAllocationOptimizationPhase.PreAllocationOptimizationContext;
import giraaff.lir.phases.PreAllocationOptimizationStage;
import giraaff.options.OptionValues;
import giraaff.phases.PhaseSuite;
import giraaff.phases.tiers.CompilerConfiguration;
import giraaff.phases.tiers.HighTierContext;
import giraaff.phases.tiers.LowTierContext;
import giraaff.phases.tiers.MidTierContext;

/**
 * The default configuration for the community edition of Graal.
 */
public class CommunityCompilerConfiguration implements CompilerConfiguration
{
    @Override
    public PhaseSuite<HighTierContext> createHighTier(OptionValues options)
    {
        return new HighTier(options);
    }

    @Override
    public PhaseSuite<MidTierContext> createMidTier(OptionValues options)
    {
        return new MidTier(options);
    }

    @Override
    public PhaseSuite<LowTierContext> createLowTier(OptionValues options)
    {
        return new LowTier(options);
    }

    @Override
    public LIRPhaseSuite<PreAllocationOptimizationContext> createPreAllocationOptimizationStage(OptionValues options)
    {
        return new PreAllocationOptimizationStage(options);
    }

    @Override
    public LIRPhaseSuite<AllocationContext> createAllocationStage(OptionValues options)
    {
        return new AllocationStage(options);
    }

    @Override
    public LIRPhaseSuite<PostAllocationOptimizationContext> createPostAllocationOptimizationStage(OptionValues options)
    {
        return new PostAllocationOptimizationStage(options);
    }
}
