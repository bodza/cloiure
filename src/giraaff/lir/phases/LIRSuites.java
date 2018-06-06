package giraaff.lir.phases;

import jdk.vm.ci.code.StackSlot;

import giraaff.lir.LIR;
import giraaff.lir.Variable;
import giraaff.lir.VirtualStackSlot;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.gen.LIRGeneratorTool;
import giraaff.lir.phases.AllocationPhase;
import giraaff.lir.phases.PostAllocationOptimizationPhase;
import giraaff.lir.phases.PreAllocationOptimizationPhase;

// @class LIRSuites
public final class LIRSuites
{
    // @field
    private final LIRPhaseSuite<PreAllocationOptimizationPhase.PreAllocationOptimizationContext> ___preAllocOptStage;
    // @field
    private final LIRPhaseSuite<AllocationPhase.AllocationContext> ___allocStage;
    // @field
    private final LIRPhaseSuite<PostAllocationOptimizationPhase.PostAllocationOptimizationContext> ___postAllocStage;
    // @field
    private boolean ___immutable;

    // @cons LIRSuites
    public LIRSuites(LIRPhaseSuite<PreAllocationOptimizationPhase.PreAllocationOptimizationContext> __preAllocOptStage, LIRPhaseSuite<AllocationPhase.AllocationContext> __allocStage, LIRPhaseSuite<PostAllocationOptimizationPhase.PostAllocationOptimizationContext> __postAllocStage)
    {
        super();
        this.___preAllocOptStage = __preAllocOptStage;
        this.___allocStage = __allocStage;
        this.___postAllocStage = __postAllocStage;
    }

    // @cons LIRSuites
    public LIRSuites(LIRSuites __other)
    {
        this(__other.getPreAllocationOptimizationStage().copy(), __other.getAllocationStage().copy(), __other.getPostAllocationOptimizationStage().copy());
    }

    ///
    // {@link PreAllocationOptimizationPhase}s are executed between {@link LIR} generation and
    // register allocation.
    //
    // {@link PreAllocationOptimizationPhase Implementers} can create new
    // {@link LIRGeneratorTool#newVariable variables}, {@link LIRGenerationResult#getFrameMap stack
    // slots} and {@link LIRGenerationResult#getFrameMapBuilder virtual stack slots}.
    ///
    public LIRPhaseSuite<PreAllocationOptimizationPhase.PreAllocationOptimizationContext> getPreAllocationOptimizationStage()
    {
        return this.___preAllocOptStage;
    }

    ///
    // {@link AllocationPhase}s are responsible for register allocation and translating
    // {@link VirtualStackSlot}s into {@link StackSlot}s.
    //
    // After the {@link AllocationStage} there should be no more {@link Variable}s and
    // {@link VirtualStackSlot}s.
    ///
    public LIRPhaseSuite<AllocationPhase.AllocationContext> getAllocationStage()
    {
        return this.___allocStage;
    }

    ///
    // {@link PostAllocationOptimizationPhase}s are executed after register allocation and before
    // machine code generation.
    //
    // A {@link PostAllocationOptimizationPhase} must not introduce new {@link Variable}s,
    // {@link VirtualStackSlot}s or {@link StackSlot}s. Blocks might be removed from
    // {@link LIR#codeEmittingOrder()} by overwriting them with {@code null}.
    ///
    public LIRPhaseSuite<PostAllocationOptimizationPhase.PostAllocationOptimizationContext> getPostAllocationOptimizationStage()
    {
        return this.___postAllocStage;
    }

    public boolean isImmutable()
    {
        return this.___immutable;
    }

    public synchronized void setImmutable()
    {
        if (!this.___immutable)
        {
            this.___preAllocOptStage.setImmutable();
            this.___allocStage.setImmutable();
            this.___postAllocStage.setImmutable();
            this.___immutable = true;
        }
    }

    public LIRSuites copy()
    {
        return new LIRSuites(this.___preAllocOptStage.copy(), this.___allocStage.copy(), this.___postAllocStage.copy());
    }
}
