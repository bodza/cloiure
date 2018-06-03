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
    private final LIRPhaseSuite<PreAllocationOptimizationContext> preAllocOptStage;
    // @field
    private final LIRPhaseSuite<AllocationContext> allocStage;
    // @field
    private final LIRPhaseSuite<PostAllocationOptimizationContext> postAllocStage;
    // @field
    private boolean immutable;

    // @cons
    public LIRSuites(LIRPhaseSuite<PreAllocationOptimizationContext> __preAllocOptStage, LIRPhaseSuite<AllocationContext> __allocStage, LIRPhaseSuite<PostAllocationOptimizationContext> __postAllocStage)
    {
        super();
        this.preAllocOptStage = __preAllocOptStage;
        this.allocStage = __allocStage;
        this.postAllocStage = __postAllocStage;
    }

    // @cons
    public LIRSuites(LIRSuites __other)
    {
        this(__other.getPreAllocationOptimizationStage().copy(), __other.getAllocationStage().copy(), __other.getPostAllocationOptimizationStage().copy());
    }

    /**
     * {@link PreAllocationOptimizationPhase}s are executed between {@link LIR} generation and
     * register allocation.
     *
     * {@link PreAllocationOptimizationPhase Implementers} can create new
     * {@link LIRGeneratorTool#newVariable variables}, {@link LIRGenerationResult#getFrameMap stack
     * slots} and {@link LIRGenerationResult#getFrameMapBuilder virtual stack slots}.
     */
    public LIRPhaseSuite<PreAllocationOptimizationContext> getPreAllocationOptimizationStage()
    {
        return preAllocOptStage;
    }

    /**
     * {@link AllocationPhase}s are responsible for register allocation and translating
     * {@link VirtualStackSlot}s into {@link StackSlot}s.
     *
     * After the {@link AllocationStage} there should be no more {@link Variable}s and
     * {@link VirtualStackSlot}s.
     */
    public LIRPhaseSuite<AllocationContext> getAllocationStage()
    {
        return allocStage;
    }

    /**
     * {@link PostAllocationOptimizationPhase}s are executed after register allocation and before
     * machine code generation.
     *
     * A {@link PostAllocationOptimizationPhase} must not introduce new {@link Variable}s,
     * {@link VirtualStackSlot}s or {@link StackSlot}s. Blocks might be removed from
     * {@link LIR#codeEmittingOrder()} by overwriting them with {@code null}.
     */
    public LIRPhaseSuite<PostAllocationOptimizationContext> getPostAllocationOptimizationStage()
    {
        return postAllocStage;
    }

    public boolean isImmutable()
    {
        return immutable;
    }

    public synchronized void setImmutable()
    {
        if (!immutable)
        {
            preAllocOptStage.setImmutable();
            allocStage.setImmutable();
            postAllocStage.setImmutable();
            immutable = true;
        }
    }

    public LIRSuites copy()
    {
        return new LIRSuites(preAllocOptStage.copy(), allocStage.copy(), postAllocStage.copy());
    }
}
