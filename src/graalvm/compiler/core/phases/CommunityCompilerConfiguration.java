package graalvm.compiler.core.phases;

import graalvm.compiler.lir.phases.AllocationPhase.AllocationContext;
import graalvm.compiler.lir.phases.AllocationStage;
import graalvm.compiler.lir.phases.LIRPhaseSuite;
import graalvm.compiler.lir.phases.PostAllocationOptimizationPhase.PostAllocationOptimizationContext;
import graalvm.compiler.lir.phases.PostAllocationOptimizationStage;
import graalvm.compiler.lir.phases.PreAllocationOptimizationPhase.PreAllocationOptimizationContext;
import graalvm.compiler.lir.phases.PreAllocationOptimizationStage;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.PhaseSuite;
import graalvm.compiler.phases.tiers.CompilerConfiguration;
import graalvm.compiler.phases.tiers.HighTierContext;
import graalvm.compiler.phases.tiers.LowTierContext;
import graalvm.compiler.phases.tiers.MidTierContext;

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
