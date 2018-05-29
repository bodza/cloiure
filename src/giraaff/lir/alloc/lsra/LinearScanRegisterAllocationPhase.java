package giraaff.lir.alloc.lsra;

import jdk.vm.ci.code.TargetDescription;

import org.graalvm.collections.Pair;

import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.phases.AllocationPhase.AllocationContext;

// @class LinearScanRegisterAllocationPhase
public final class LinearScanRegisterAllocationPhase extends LinearScanAllocationPhase
{
    private final LinearScan allocator;

    // @cons
    LinearScanRegisterAllocationPhase(LinearScan allocator)
    {
        super();
        this.allocator = allocator;
    }

    @Override
    protected void run(TargetDescription target, LIRGenerationResult result, AllocationContext context)
    {
        allocateRegisters();
    }

    void allocateRegisters()
    {
        Interval precoloredIntervals;
        Interval notPrecoloredIntervals;

        Pair<Interval, Interval> result = allocator.createUnhandledLists(LinearScan.IS_PRECOLORED_INTERVAL, LinearScan.IS_VARIABLE_INTERVAL);
        precoloredIntervals = result.getLeft();
        notPrecoloredIntervals = result.getRight();

        // allocate cpu registers
        LinearScanWalker lsw;
        if (OptimizingLinearScanWalker.Options.LSRAOptimization.getValue(allocator.getOptions()))
        {
            lsw = new OptimizingLinearScanWalker(allocator, precoloredIntervals, notPrecoloredIntervals);
        }
        else
        {
            lsw = new LinearScanWalker(allocator, precoloredIntervals, notPrecoloredIntervals);
        }
        lsw.walk();
        lsw.finishAllocation();
    }
}
