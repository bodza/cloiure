package graalvm.compiler.lir.phases;

import jdk.vm.ci.code.StackSlot;

import graalvm.compiler.lir.LIR;
import graalvm.compiler.lir.Variable;
import graalvm.compiler.lir.VirtualStackSlot;
import graalvm.compiler.lir.gen.LIRGenerationResult;
import graalvm.compiler.lir.gen.LIRGeneratorTool;
import graalvm.compiler.lir.phases.AllocationPhase.AllocationContext;
import graalvm.compiler.lir.phases.PostAllocationOptimizationPhase.PostAllocationOptimizationContext;
import graalvm.compiler.lir.phases.PreAllocationOptimizationPhase.PreAllocationOptimizationContext;

public class LIRSuites
{
    private final LIRPhaseSuite<PreAllocationOptimizationContext> preAllocOptStage;
    private final LIRPhaseSuite<AllocationContext> allocStage;
    private final LIRPhaseSuite<PostAllocationOptimizationContext> postAllocStage;
    private boolean immutable;

    public LIRSuites(LIRPhaseSuite<PreAllocationOptimizationContext> preAllocOptStage, LIRPhaseSuite<AllocationContext> allocStage, LIRPhaseSuite<PostAllocationOptimizationContext> postAllocStage)
    {
        this.preAllocOptStage = preAllocOptStage;
        this.allocStage = allocStage;
        this.postAllocStage = postAllocStage;
    }

    public LIRSuites(LIRSuites other)
    {
        this(other.getPreAllocationOptimizationStage().copy(), other.getAllocationStage().copy(), other.getPostAllocationOptimizationStage().copy());
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
