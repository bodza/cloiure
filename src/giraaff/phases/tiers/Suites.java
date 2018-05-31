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
    private final PhaseSuite<HighTierContext> highTier;
    private final PhaseSuite<MidTierContext> midTier;
    private final PhaseSuite<LowTierContext> lowTier;
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
    public Suites(PhaseSuite<HighTierContext> highTier, PhaseSuite<MidTierContext> midTier, PhaseSuite<LowTierContext> lowTier)
    {
        super();
        this.highTier = highTier;
        this.midTier = midTier;
        this.lowTier = lowTier;
    }

    public static Suites createSuites(CompilerConfiguration config)
    {
        return new Suites(config.createHighTier(), config.createMidTier(), config.createLowTier());
    }

    public static LIRSuites createLIRSuites(CompilerConfiguration config)
    {
        return new LIRSuites(config.createPreAllocationOptimizationStage(), config.createAllocationStage(), config.createPostAllocationOptimizationStage());
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
