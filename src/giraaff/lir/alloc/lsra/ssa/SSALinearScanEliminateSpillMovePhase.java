package giraaff.lir.alloc.lsra.ssa;

import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.lir.LIRInstruction;
import giraaff.lir.StandardOp.LabelOp;
import giraaff.lir.StandardOp.MoveOp;
import giraaff.lir.alloc.lsra.Interval;
import giraaff.lir.alloc.lsra.LinearScan;
import giraaff.lir.alloc.lsra.LinearScanEliminateSpillMovePhase;

// @class SSALinearScanEliminateSpillMovePhase
public final class SSALinearScanEliminateSpillMovePhase extends LinearScanEliminateSpillMovePhase
{
    // @cons
    SSALinearScanEliminateSpillMovePhase(LinearScan __allocator)
    {
        super(__allocator);
    }

    @Override
    protected int firstInstructionOfInterest()
    {
        // also look at Labels as they define PHI values
        return 0;
    }

    @Override
    protected boolean canEliminateSpillMove(AbstractBlockBase<?> __block, MoveOp __move)
    {
        if (super.canEliminateSpillMove(__block, __move))
        {
            // SSA Linear Scan might introduce moves to stack slots
            Interval __curInterval = allocator.intervalFor(__move.getResult());
            if (!isPhiResolutionMove(__block, __move, __curInterval))
            {
                return true;
            }
        }
        return false;
    }

    private boolean isPhiResolutionMove(AbstractBlockBase<?> __block, MoveOp __move, Interval __toInterval)
    {
        if (!__toInterval.isSplitParent())
        {
            return false;
        }
        if ((__toInterval.from() & 1) == 1)
        {
            // phi intervals start at even positions.
            return false;
        }
        if (__block.getSuccessorCount() != 1)
        {
            return false;
        }
        LIRInstruction __op = allocator.instructionForId(__toInterval.from());
        if (!(__op instanceof LabelOp))
        {
            return false;
        }
        AbstractBlockBase<?> __intStartBlock = allocator.blockForId(__toInterval.from());
        if (!__block.getSuccessors()[0].equals(__intStartBlock))
        {
            return false;
        }
        return true;
    }
}
