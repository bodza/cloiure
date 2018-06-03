package giraaff.lir.alloc.lsra;

import jdk.vm.ci.code.TargetDescription;

import org.graalvm.collections.Pair;

import giraaff.core.common.GraalOptions;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.phases.AllocationPhase.AllocationContext;

// @class LinearScanRegisterAllocationPhase
public final class LinearScanRegisterAllocationPhase extends LinearScanAllocationPhase
{
    // @field
    private final LinearScan ___allocator;

    // @cons
    LinearScanRegisterAllocationPhase(LinearScan __allocator)
    {
        super();
        this.___allocator = __allocator;
    }

    @Override
    protected void run(TargetDescription __target, LIRGenerationResult __result, AllocationContext __context)
    {
        allocateRegisters();
    }

    void allocateRegisters()
    {
        Interval __precoloredIntervals;
        Interval __notPrecoloredIntervals;

        Pair<Interval, Interval> __result = this.___allocator.createUnhandledLists(LinearScan.IS_PRECOLORED_INTERVAL, LinearScan.IS_VARIABLE_INTERVAL);
        __precoloredIntervals = __result.getLeft();
        __notPrecoloredIntervals = __result.getRight();

        // allocate cpu registers
        LinearScanWalker __lsw;
        if (GraalOptions.lsraOptimization)
        {
            __lsw = new OptimizingLinearScanWalker(this.___allocator, __precoloredIntervals, __notPrecoloredIntervals);
        }
        else
        {
            __lsw = new LinearScanWalker(this.___allocator, __precoloredIntervals, __notPrecoloredIntervals);
        }
        __lsw.walk();
        __lsw.finishAllocation();
    }
}
