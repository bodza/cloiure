package graalvm.compiler.lir.alloc.lsra.ssa;

import graalvm.compiler.core.common.cfg.AbstractBlockBase;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.StandardOp.LabelOp;
import graalvm.compiler.lir.StandardOp.MoveOp;
import graalvm.compiler.lir.alloc.lsra.Interval;
import graalvm.compiler.lir.alloc.lsra.LinearScan;
import graalvm.compiler.lir.alloc.lsra.LinearScanEliminateSpillMovePhase;

public class SSALinearScanEliminateSpillMovePhase extends LinearScanEliminateSpillMovePhase
{
    SSALinearScanEliminateSpillMovePhase(LinearScan allocator)
    {
        super(allocator);
    }

    @Override
    protected int firstInstructionOfInterest()
    {
        // also look at Labels as they define PHI values
        return 0;
    }

    @Override
    protected boolean canEliminateSpillMove(AbstractBlockBase<?> block, MoveOp move)
    {
        if (super.canEliminateSpillMove(block, move))
        {
            // SSA Linear Scan might introduce moves to stack slots
            Interval curInterval = allocator.intervalFor(move.getResult());
            if (!isPhiResolutionMove(block, move, curInterval))
            {
                return true;
            }
        }
        return false;
    }

    private boolean isPhiResolutionMove(AbstractBlockBase<?> block, MoveOp move, Interval toInterval)
    {
        if (!toInterval.isSplitParent())
        {
            return false;
        }
        if ((toInterval.from() & 1) == 1)
        {
            // phi intervals start at even positions.
            return false;
        }
        if (block.getSuccessorCount() != 1)
        {
            return false;
        }
        LIRInstruction op = allocator.instructionForId(toInterval.from());
        if (!(op instanceof LabelOp))
        {
            return false;
        }
        AbstractBlockBase<?> intStartBlock = allocator.blockForId(toInterval.from());
        if (!block.getSuccessors()[0].equals(intStartBlock))
        {
            return false;
        }
        return true;
    }
}
