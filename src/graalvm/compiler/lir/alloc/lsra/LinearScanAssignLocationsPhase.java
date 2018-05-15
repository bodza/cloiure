package graalvm.compiler.lir.alloc.lsra;

import static jdk.vm.ci.code.ValueUtil.isIllegal;
import static graalvm.compiler.lir.LIRValueUtil.isJavaConstant;
import static graalvm.compiler.lir.LIRValueUtil.isStackSlotValue;
import static graalvm.compiler.lir.LIRValueUtil.isVariable;
import static graalvm.compiler.lir.LIRValueUtil.isVirtualStackSlot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;

import graalvm.compiler.core.common.cfg.AbstractBlockBase;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.debug.Indent;
import graalvm.compiler.lir.ConstantValue;
import graalvm.compiler.lir.InstructionValueProcedure;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.LIRInstruction.OperandFlag;
import graalvm.compiler.lir.LIRInstruction.OperandMode;
import graalvm.compiler.lir.StandardOp;
import graalvm.compiler.lir.StandardOp.MoveOp;
import graalvm.compiler.lir.StandardOp.ValueMoveOp;
import graalvm.compiler.lir.Variable;
import graalvm.compiler.lir.gen.LIRGenerationResult;
import graalvm.compiler.lir.phases.AllocationPhase.AllocationContext;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Value;

/**
 * Phase 7: Assign register numbers back to LIR.
 */
public class LinearScanAssignLocationsPhase extends LinearScanAllocationPhase {

    protected final LinearScan allocator;

    public LinearScanAssignLocationsPhase(LinearScan allocator) {
        this.allocator = allocator;
    }

    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, AllocationContext context) {
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
    protected Value colorLirOperand(LIRInstruction op, Variable operand, OperandMode mode) {
        int opId = op.id();
        Interval interval = allocator.intervalFor(operand);
        assert interval != null : "interval must exist";

        if (opId != -1) {
            if (allocator.detailedAsserts) {
                AbstractBlockBase<?> block = allocator.blockForId(opId);
                if (block.getSuccessorCount() <= 1 && opId == allocator.getLastLirInstructionId(block)) {
                    /*
                     * Check if spill moves could have been appended at the end of this block, but
                     * before the branch instruction. So the split child information for this branch
                     * would be incorrect.
                     */
                    LIRInstruction instr = allocator.getLIR().getLIRforBlock(block).get(allocator.getLIR().getLIRforBlock(block).size() - 1);
                    if (instr instanceof StandardOp.JumpOp) {
                        if (allocator.getBlockData(block).liveOut.get(allocator.operandNumber(operand))) {
                            assert false : String.format(
                                            "can't get split child for the last branch of a block because the information would be incorrect (moves are inserted before the branch in resolveDataFlow) block=%s, instruction=%s, operand=%s",
                                            block, instr, operand);
                        }
                    }
                }
            }

            /*
             * Operands are not changed when an interval is split during allocation, so search the
             * right interval here.
             */
            interval = allocator.splitChildAtOpId(interval, opId, mode);
        }

        if (isIllegal(interval.location()) && interval.canMaterialize()) {
            assert mode != OperandMode.DEF;
            return new ConstantValue(interval.kind(), interval.getMaterializedValue());
        }
        return interval.location();
    }

    private Value debugInfoProcedure(LIRInstruction op, Value operand) {
        if (isVirtualStackSlot(operand)) {
            return operand;
        }
        int tempOpId = op.id();
        OperandMode mode = OperandMode.USE;
        AbstractBlockBase<?> block = allocator.blockForId(tempOpId);
        if (block.getSuccessorCount() == 1 && tempOpId == allocator.getLastLirInstructionId(block)) {
            /*
             * Generating debug information for the last instruction of a block. If this instruction
             * is a branch, spill moves are inserted before this branch and so the wrong operand
             * would be returned (spill moves at block boundaries are not considered in the live
             * ranges of intervals).
             *
             * Solution: use the first opId of the branch target block instead.
             */
            final LIRInstruction instr = allocator.getLIR().getLIRforBlock(block).get(allocator.getLIR().getLIRforBlock(block).size() - 1);
            if (instr instanceof StandardOp.JumpOp) {
                if (allocator.getBlockData(block).liveOut.get(allocator.operandNumber(operand))) {
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
        assert !allocator.hasCall(tempOpId) || isStackSlotValue(result) || isJavaConstant(result) || !allocator.isCallerSave(result) : "cannot have caller-save register operands at calls";
        return result;
    }

    private void assignLocations(ArrayList<LIRInstruction> instructions) {
        int numInst = instructions.size();
        boolean hasDead = false;

        for (int j = 0; j < numInst; j++) {
            final LIRInstruction op = instructions.get(j);
            if (op == null) {
                /*
                 * this can happen when spill-moves are removed in eliminateSpillMoves
                 */
                hasDead = true;
            } else if (assignLocations(op)) {
                instructions.set(j, null);
                hasDead = true;
            }
        }

        if (hasDead) {
            // Remove null values from the list.
            instructions.removeAll(Collections.singleton(null));
        }
    }

    private final InstructionValueProcedure assignProc = new InstructionValueProcedure() {
        @Override
        public Value doValue(LIRInstruction instruction, Value value, OperandMode mode, EnumSet<OperandFlag> flags) {
            if (isVariable(value)) {
                return colorLirOperand(instruction, (Variable) value, mode);
            }
            return value;
        }
    };
    private final InstructionValueProcedure debugInfoProc = new InstructionValueProcedure() {
        @Override
        public Value doValue(LIRInstruction instruction, Value value, OperandMode mode, EnumSet<OperandFlag> flags) {
            return debugInfoProcedure(instruction, value);
        }
    };

    /**
     * Assigns the operand of an {@link LIRInstruction}.
     *
     * @param op The {@link LIRInstruction} that should be colored.
     * @return {@code true} if the instruction should be deleted.
     */
    protected boolean assignLocations(LIRInstruction op) {
        assert op != null;

        // remove useless moves
        if (MoveOp.isMoveOp(op)) {
            AllocatableValue result = MoveOp.asMoveOp(op).getResult();
            if (isVariable(result) && allocator.isMaterialized(result, op.id(), OperandMode.DEF)) {
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
        if (ValueMoveOp.isValueMoveOp(op)) {
            ValueMoveOp move = ValueMoveOp.asValueMoveOp(op);
            if (move.getInput().equals(move.getResult())) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("try")
    private void assignLocations() {
        DebugContext debug = allocator.getDebug();
        try (Indent indent = debug.logAndIndent("assign locations")) {
            for (AbstractBlockBase<?> block : allocator.sortedBlocks()) {
                try (Indent indent2 = debug.logAndIndent("assign locations in block B%d", block.getId())) {
                    assignLocations(allocator.getLIR().getLIRforBlock(block));
                }
            }
        }
    }
}
