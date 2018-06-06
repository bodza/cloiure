package giraaff.lir.alloc.lsra.ssa;

import jdk.vm.ci.code.TargetDescription;

import giraaff.core.common.alloc.RegisterAllocationConfig;
import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.lir.alloc.lsra.LinearScan;
import giraaff.lir.alloc.lsra.LinearScanEliminateSpillMovePhase;
import giraaff.lir.alloc.lsra.LinearScanLifetimeAnalysisPhase;
import giraaff.lir.alloc.lsra.LinearScanResolveDataFlowPhase;
import giraaff.lir.alloc.lsra.MoveResolver;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.gen.LIRGeneratorTool;
import giraaff.lir.ssa.SSAUtil;

// @class SSALinearScan
public final class SSALinearScan extends LinearScan
{
    // @cons SSALinearScan
    public SSALinearScan(TargetDescription __target, LIRGenerationResult __res, LIRGeneratorTool.MoveFactory __spillMoveFactory, RegisterAllocationConfig __regAllocConfig, AbstractBlockBase<?>[] __sortedBlocks, boolean __neverSpillConstants)
    {
        super(__target, __res, __spillMoveFactory, __regAllocConfig, __sortedBlocks, __neverSpillConstants);
    }

    @Override
    protected MoveResolver createMoveResolver()
    {
        return new SSAMoveResolver(this);
    }

    @Override
    protected LinearScanLifetimeAnalysisPhase createLifetimeAnalysisPhase()
    {
        return new SSALinearScanLifetimeAnalysisPhase(this);
    }

    @Override
    protected LinearScanResolveDataFlowPhase createResolveDataFlowPhase()
    {
        return new SSALinearScanResolveDataFlowPhase(this);
    }

    @Override
    protected LinearScanEliminateSpillMovePhase createSpillMoveEliminationPhase()
    {
        return new SSALinearScanEliminateSpillMovePhase(this);
    }

    @Override
    protected void beforeSpillMoveElimination()
    {
        // PHIs where the Out and In value matches (ie. there is no resolution move) are falsely detected as errors.
        for (AbstractBlockBase<?> __toBlock : sortedBlocks())
        {
            if (__toBlock.getPredecessorCount() > 1)
            {
                SSAUtil.removePhiIn(getLIR(), __toBlock);
            }
        }
    }
}
