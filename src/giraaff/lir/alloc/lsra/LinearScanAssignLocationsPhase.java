package giraaff.lir.alloc.lsra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Value;

import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.lir.ConstantValue;
import giraaff.lir.InstructionValueProcedure;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstruction.OperandMode;
import giraaff.lir.LIRValueUtil;
import giraaff.lir.StandardOp;
import giraaff.lir.StandardOp.MoveOp;
import giraaff.lir.StandardOp.ValueMoveOp;
import giraaff.lir.Variable;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.phases.AllocationPhase.AllocationContext;

/**
 * Phase 7: Assign register numbers back to LIR.
 */
public class LinearScanAssignLocationsPhase extends LinearScanAllocationPhase
{
    protected final LinearScan allocator;

    public LinearScanAssignLocationsPhase(LinearScan allocator)
    {
        this.allocator = allocator;
    }

    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, AllocationContext context)
    {
        assignLocations();
    }

    /**
     * Assigns the allocated location for an LIR instruction operand back into the instruction.
     *
     * @param op current {@link LIRInstruction}
     * @param operand an LIR instruction operand
     * @param mode the usage mode for {@code operand} by the instruction
     * @return the location assigned for the operand
     */
    protected Value colorLirOperand(LIRInstruction op, Variable operand, OperandMode mode)
    {
        int opId = op.id();
        Interval interval = allocator.intervalFor(operand);

        if (opId != -1)
        {
            // Operands are not changed when an interval is split during allocation, so search the right interval here.
            interval = allocator.splitChildAtOpId(interval, opId, mode);
        }

        if (ValueUtil.isIllegal(interval.location()) && interval.canMaterialize())
        {
            return new ConstantValue(interval.kind(), interval.getMaterializedValue());
        }
        return interval.location();
    }

    private Value debugInfoProcedure(LIRInstruction op, Value operand)
    {
        if (LIRValueUtil.isVirtualStackSlot(operand))
        {
            return operand;
        }
        int tempOpId = op.id();
        OperandMode mode = OperandMode.USE;
        AbstractBlockBase<?> block = allocator.blockForId(tempOpId);
        if (block.getSuccessorCount() == 1 && tempOpId == allocator.getLastLirInstructionId(block))
        {
            /*
             * Generating debug information for the last instruction of a block. If this instruction
             * is a branch, spill moves are inserted before this branch and so the wrong operand
             * would be returned (spill moves at block boundaries are not considered in the live
             * ranges of intervals).
             *
             * Solution: use the first opId of the branch target block instead.
             */
            final LIRInstruction instr = allocator.getLIR().getLIRforBlock(block).get(allocator.getLIR().getLIRforBlock(block).size() - 1);
            if (instr instanceof StandardOp.JumpOp)
            {
                if (allocator.getBlockData(block).liveOut.get(allocator.operandNumber(operand)))
                {
                    tempOpId = allocator.getFirstLirInstructionId(block.getSuccessors()[0]);
                    mode = OperandMode.DEF;
                }
            }
        }

        /*
         * Get current location of operand. The operand must be live because debug information is
         * considered when building the intervals if the interval is not live, colorLirOperand will
         * cause an assert on failure.
         */
        Value result = colorLirOperand(op, (Variable) operand, mode);
        return result;
    }

    private void assignLocations(ArrayList<LIRInstruction> instructions)
    {
        int numInst = instructions.size();
        boolean hasDead = false;

        for (int j = 0; j < numInst; j++)
        {
            final LIRInstruction op = instructions.get(j);
            if (op == null)
            {
                // this can happen when spill-moves are removed in eliminateSpillMoves
                hasDead = true;
            }
            else if (assignLocations(op))
            {
                instructions.set(j, null);
                hasDead = true;
            }
        }

        if (hasDead)
        {
            // Remove null values from the list.
            instructions.removeAll(Collections.singleton(null));
        }
    }

    private final InstructionValueProcedure assignProc = new InstructionValueProcedure()
    {
        @Override
        public Value doValue(LIRInstruction instruction, Value value, OperandMode mode, EnumSet<OperandFlag> flags)
        {
            if (LIRValueUtil.isVariable(value))
            {
                return colorLirOperand(instruction, (Variable) value, mode);
            }
            return value;
        }
    };
    private final InstructionValueProcedure debugInfoProc = new InstructionValueProcedure()
    {
        @Override
        public Value doValue(LIRInstruction instruction, Value value, OperandMode mode, EnumSet<OperandFlag> flags)
        {
            return debugInfoProcedure(instruction, value);
        }
    };

    /**
     * Assigns the operand of an {@link LIRInstruction}.
     *
     * @param op The {@link LIRInstruction} that should be colored.
     * @return {@code true} if the instruction should be deleted.
     */
    protected boolean assignLocations(LIRInstruction op)
    {
        // remove useless moves
        if (MoveOp.isMoveOp(op))
        {
            AllocatableValue result = MoveOp.asMoveOp(op).getResult();
            if (LIRValueUtil.isVariable(result) && allocator.isMaterialized(result, op.id(), OperandMode.DEF))
            {
                /*
                 * This happens if a materializable interval is originally not spilled but then
                 * kicked out in LinearScanWalker.splitForSpilling(). When kicking out such an
                 * interval this move operation was already generated.
                 */
                return true;
            }
        }

        op.forEachInput(assignProc);
        op.forEachAlive(assignProc);
        op.forEachTemp(assignProc);
        op.forEachOutput(assignProc);

        // compute reference map and debug information
        op.forEachState(debugInfoProc);

        // remove useless moves
        if (ValueMoveOp.isValueMoveOp(op))
        {
            ValueMoveOp move = ValueMoveOp.asValueMoveOp(op);
            if (move.getInput().equals(move.getResult()))
            {
                return true;
            }
        }
        return false;
    }

    private void assignLocations()
    {
        for (AbstractBlockBase<?> block : allocator.sortedBlocks())
        {
            assignLocations(allocator.getLIR().getLIRforBlock(block));
        }
    }
}