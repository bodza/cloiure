package graalvm.compiler.lir.alloc.lsra.ssa;

import graalvm.compiler.core.common.alloc.RegisterAllocationConfig;
import graalvm.compiler.core.common.cfg.AbstractBlockBase;
import graalvm.compiler.lir.alloc.lsra.LinearScan;
import graalvm.compiler.lir.alloc.lsra.LinearScanEliminateSpillMovePhase;
import graalvm.compiler.lir.alloc.lsra.LinearScanLifetimeAnalysisPhase;
import graalvm.compiler.lir.alloc.lsra.LinearScanResolveDataFlowPhase;
import graalvm.compiler.lir.alloc.lsra.MoveResolver;
import graalvm.compiler.lir.gen.LIRGenerationResult;
import graalvm.compiler.lir.gen.LIRGeneratorTool.MoveFactory;
import graalvm.compiler.lir.ssa.SSAUtil;

import jdk.vm.ci.code.TargetDescription;

public final class SSALinearScan extends LinearScan
{
    public SSALinearScan(TargetDescription target, LIRGenerationResult res, MoveFactory spillMoveFactory, RegisterAllocationConfig regAllocConfig, AbstractBlockBase<?>[] sortedBlocks, boolean neverSpillConstants)
    {
        super(target, res, spillMoveFactory, regAllocConfig, sortedBlocks, neverSpillConstants);
    }

    @Override
    protected MoveResolver createMoveResolver()
    {
        SSAMoveResolver moveResolver = new SSAMoveResolver(this);
        return moveResolver;
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
        /*
         * PHI Ins are needed for the RegisterVerifier, otherwise PHIs where the Out and In value
         * matches (ie. there is no resolution move) are falsely detected as errors.
         */
        for (AbstractBlockBase<?> toBlock : sortedBlocks())
        {
            if (toBlock.getPredecessorCount() > 1)
            {
                SSAUtil.removePhiIn(getLIR(), toBlock);
            }
        }
    }
}
