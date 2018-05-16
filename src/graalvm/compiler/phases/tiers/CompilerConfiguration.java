package graalvm.compiler.phases.tiers;

import graalvm.compiler.lir.phases.AllocationPhase.AllocationContext;
import graalvm.compiler.lir.phases.LIRPhaseSuite;
import graalvm.compiler.lir.phases.PostAllocationOptimizationPhase.PostAllocationOptimizationContext;
import graalvm.compiler.lir.phases.PreAllocationOptimizationPhase.PreAllocationOptimizationContext;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.PhaseSuite;

public interface CompilerConfiguration
{
    PhaseSuite<HighTierContext> createHighTier(OptionValues options);

    PhaseSuite<MidTierContext> createMidTier(OptionValues options);

    PhaseSuite<LowTierContext> createLowTier(OptionValues options);

    LIRPhaseSuite<PreAllocationOptimizationContext> createPreAllocationOptimizationStage(OptionValues options);

    LIRPhaseSuite<AllocationContext> createAllocationStage(OptionValues options);

    LIRPhaseSuite<PostAllocationOptimizationContext> createPostAllocationOptimizationStage(OptionValues options);
}
