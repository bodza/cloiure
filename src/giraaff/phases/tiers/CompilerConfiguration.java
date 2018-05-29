package giraaff.phases.tiers;

import giraaff.lir.phases.AllocationPhase.AllocationContext;
import giraaff.lir.phases.LIRPhaseSuite;
import giraaff.lir.phases.PostAllocationOptimizationPhase.PostAllocationOptimizationContext;
import giraaff.lir.phases.PreAllocationOptimizationPhase.PreAllocationOptimizationContext;
import giraaff.options.OptionValues;
import giraaff.phases.PhaseSuite;

// @iface CompilerConfiguration
public interface CompilerConfiguration
{
    PhaseSuite<HighTierContext> createHighTier(OptionValues options);

    PhaseSuite<MidTierContext> createMidTier(OptionValues options);

    PhaseSuite<LowTierContext> createLowTier(OptionValues options);

    LIRPhaseSuite<PreAllocationOptimizationContext> createPreAllocationOptimizationStage(OptionValues options);

    LIRPhaseSuite<AllocationContext> createAllocationStage(OptionValues options);

    LIRPhaseSuite<PostAllocationOptimizationContext> createPostAllocationOptimizationStage(OptionValues options);
}
