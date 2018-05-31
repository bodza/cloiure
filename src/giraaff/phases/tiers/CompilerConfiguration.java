package giraaff.phases.tiers;

import giraaff.lir.phases.AllocationPhase.AllocationContext;
import giraaff.lir.phases.LIRPhaseSuite;
import giraaff.lir.phases.PostAllocationOptimizationPhase.PostAllocationOptimizationContext;
import giraaff.lir.phases.PreAllocationOptimizationPhase.PreAllocationOptimizationContext;
import giraaff.phases.PhaseSuite;

// @iface CompilerConfiguration
public interface CompilerConfiguration
{
    PhaseSuite<HighTierContext> createHighTier();

    PhaseSuite<MidTierContext> createMidTier();

    PhaseSuite<LowTierContext> createLowTier();

    LIRPhaseSuite<PreAllocationOptimizationContext> createPreAllocationOptimizationStage();

    LIRPhaseSuite<AllocationContext> createAllocationStage();

    LIRPhaseSuite<PostAllocationOptimizationContext> createPostAllocationOptimizationStage();
}
