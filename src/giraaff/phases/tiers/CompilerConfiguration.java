package giraaff.phases.tiers;

import giraaff.lir.phases.AllocationPhase;
import giraaff.lir.phases.LIRPhaseSuite;
import giraaff.lir.phases.PostAllocationOptimizationPhase;
import giraaff.lir.phases.PreAllocationOptimizationPhase;
import giraaff.phases.PhaseSuite;

// @iface CompilerConfiguration
public interface CompilerConfiguration
{
    PhaseSuite<HighTierContext> createHighTier();

    PhaseSuite<MidTierContext> createMidTier();

    PhaseSuite<LowTierContext> createLowTier();

    LIRPhaseSuite<PreAllocationOptimizationPhase.PreAllocationOptimizationContext> createPreAllocationOptimizationStage();

    LIRPhaseSuite<AllocationPhase.AllocationContext> createAllocationStage();

    LIRPhaseSuite<PostAllocationOptimizationPhase.PostAllocationOptimizationContext> createPostAllocationOptimizationStage();
}
