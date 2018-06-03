package giraaff.phases.tiers;

import giraaff.lir.alloc.RegisterAllocationPhase;
import giraaff.lir.phases.AllocationPhase.AllocationContext;
import giraaff.lir.phases.LIRPhase;
import giraaff.lir.phases.LIRPhaseSuite;
import giraaff.lir.phases.LIRSuites;
import giraaff.phases.PhaseSuite;

// @class Suites
public final class Suites
{
    // @field
    private final PhaseSuite<HighTierContext> highTier;
    // @field
    private final PhaseSuite<MidTierContext> midTier;
    // @field
    private final PhaseSuite<LowTierContext> lowTier;
    // @field
    private boolean immutable;

    public PhaseSuite<HighTierContext> getHighTier()
    {
        return highTier;
    }

    public PhaseSuite<MidTierContext> getMidTier()
    {
        return midTier;
    }

    public PhaseSuite<LowTierContext> getLowTier()
    {
        return lowTier;
    }

    // @cons
    public Suites(PhaseSuite<HighTierContext> __highTier, PhaseSuite<MidTierContext> __midTier, PhaseSuite<LowTierContext> __lowTier)
    {
        super();
        this.highTier = __highTier;
        this.midTier = __midTier;
        this.lowTier = __lowTier;
    }

    public static Suites createSuites(CompilerConfiguration __config)
    {
        return new Suites(__config.createHighTier(), __config.createMidTier(), __config.createLowTier());
    }

    public static LIRSuites createLIRSuites(CompilerConfiguration __config)
    {
        return new LIRSuites(__config.createPreAllocationOptimizationStage(), __config.createAllocationStage(), __config.createPostAllocationOptimizationStage());
    }

    public boolean isImmutable()
    {
        return immutable;
    }

    public synchronized void setImmutable()
    {
        if (!immutable)
        {
            highTier.setImmutable();
            midTier.setImmutable();
            lowTier.setImmutable();
            immutable = true;
        }
    }

    public Suites copy()
    {
        return new Suites(highTier.copy(), midTier.copy(), lowTier.copy());
    }
}
