package giraaff.core.phases;

import giraaff.lir.phases.AllocationPhase.AllocationContext;
import giraaff.lir.phases.AllocationStage;
import giraaff.lir.phases.LIRPhaseSuite;
import giraaff.lir.phases.PostAllocationOptimizationPhase.PostAllocationOptimizationContext;
import giraaff.lir.phases.PostAllocationOptimizationStage;
import giraaff.lir.phases.PreAllocationOptimizationPhase.PreAllocationOptimizationContext;
import giraaff.lir.phases.PreAllocationOptimizationStage;
import giraaff.phases.PhaseSuite;
import giraaff.phases.tiers.CompilerConfiguration;
import giraaff.phases.tiers.HighTierContext;
import giraaff.phases.tiers.LowTierContext;
import giraaff.phases.tiers.MidTierContext;

///
// The default configuration for the community edition of Graal.
///
// @class CommunityCompilerConfiguration
public final class CommunityCompilerConfiguration implements CompilerConfiguration
{
    // @cons
    public CommunityCompilerConfiguration()
    {
        super();
    }

    @Override
    public PhaseSuite<HighTierContext> createHighTier()
    {
        return new HighTier();
    }

    @Override
    public PhaseSuite<MidTierContext> createMidTier()
    {
        return new MidTier();
    }

    @Override
    public PhaseSuite<LowTierContext> createLowTier()
    {
        return new LowTier();
    }

    @Override
    public LIRPhaseSuite<PreAllocationOptimizationContext> createPreAllocationOptimizationStage()
    {
        return new PreAllocationOptimizationStage();
    }

    @Override
    public LIRPhaseSuite<AllocationContext> createAllocationStage()
    {
        return new AllocationStage();
    }

    @Override
    public LIRPhaseSuite<PostAllocationOptimizationContext> createPostAllocationOptimizationStage()
    {
        return new PostAllocationOptimizationStage();
    }
}
