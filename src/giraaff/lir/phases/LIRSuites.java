package giraaff.lir.phases;

import jdk.vm.ci.code.StackSlot;

import giraaff.lir.LIR;
import giraaff.lir.Variable;
import giraaff.lir.VirtualStackSlot;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.gen.LIRGeneratorTool;
import giraaff.lir.phases.AllocationPhase.AllocationContext;
import giraaff.lir.phases.PostAllocationOptimizationPhase.PostAllocationOptimizationContext;
import giraaff.lir.phases.PreAllocationOptimizationPhase.PreAllocationOptimizationContext;

// @class LIRSuites
public final class LIRSuites
{
    // @field
    private final LIRPhaseSuite<PreAllocationOptimizationContext> ___preAllocOptStage;
    // @field
    private final LIRPhaseSuite<AllocationContext> ___allocStage;
    // @field
    private final LIRPhaseSuite<PostAllocationOptimizationContext> ___postAllocStage;
    // @field
    private boolean ___immutable;

    // @cons
    public LIRSuites(LIRPhaseSuite<PreAllocationOptimizationContext> __preAllocOptStage, LIRPhaseSuite<AllocationContext> __allocStage, LIRPhaseSuite<PostAllocationOptimizationContext> __postAllocStage)
    {
        super();
        this.___preAllocOptStage = __preAllocOptStage;
        this.___allocStage = __allocStage;
        this.___postAllocStage = __postAllocStage;
    }

    // @cons
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
    public LIRPhaseSuite<PreAllocationOptimizationContext> getPreAllocationOptimizationStage()
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
    public LIRPhaseSuite<AllocationContext> getAllocationStage()
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
    public LIRPhaseSuite<PostAllocationOptimizationContext> getPostAllocationOptimizationStage()
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
