package giraaff.phases.tiers;

import giraaff.lir.alloc.RegisterAllocationPhase;
import giraaff.lir.phases.AllocationPhase;
import giraaff.lir.phases.LIRPhase;
import giraaff.lir.phases.LIRPhaseSuite;
import giraaff.lir.phases.LIRSuites;
import giraaff.phases.PhaseSuite;

// @class Suites
public final class Suites
{
    // @field
    private final PhaseSuite<HighTierContext> ___highTier;
    // @field
    private final PhaseSuite<MidTierContext> ___midTier;
    // @field
    private final PhaseSuite<LowTierContext> ___lowTier;
    // @field
    private boolean ___immutable;

    public PhaseSuite<HighTierContext> getHighTier()
    {
        return this.___highTier;
    }

    public PhaseSuite<MidTierContext> getMidTier()
    {
        return this.___midTier;
    }

    public PhaseSuite<LowTierContext> getLowTier()
    {
        return this.___lowTier;
    }

    // @cons Suites
    public Suites(PhaseSuite<HighTierContext> __highTier, PhaseSuite<MidTierContext> __midTier, PhaseSuite<LowTierContext> __lowTier)
    {
        super();
        this.___highTier = __highTier;
        this.___midTier = __midTier;
        this.___lowTier = __lowTier;
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
        return this.___immutable;
    }

    public synchronized void setImmutable()
    {
        if (!this.___immutable)
        {
            this.___highTier.setImmutable();
            this.___midTier.setImmutable();
            this.___lowTier.setImmutable();
            this.___immutable = true;
        }
    }

    public Suites copy()
    {
        return new Suites(this.___highTier.copy(), this.___midTier.copy(), this.___lowTier.copy());
    }
}
