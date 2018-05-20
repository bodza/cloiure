package graalvm.compiler.lir.alloc.trace.lsra;

import static jdk.vm.ci.code.ValueUtil.isIllegal;
import static jdk.vm.ci.code.ValueUtil.isRegister;
import static graalvm.compiler.lir.LIRValueUtil.asVariable;
import static graalvm.compiler.lir.LIRValueUtil.isConstantValue;
import static graalvm.compiler.lir.LIRValueUtil.isStackSlotValue;
import static graalvm.compiler.lir.LIRValueUtil.isVariable;
import static graalvm.compiler.lir.LIRValueUtil.isVirtualStackSlot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;

import graalvm.compiler.core.common.alloc.RegisterAllocationConfig;
import graalvm.compiler.core.common.alloc.Trace;
import graalvm.compiler.core.common.alloc.TraceBuilderResult;
import graalvm.compiler.core.common.cfg.AbstractBlockBase;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.lir.ConstantValue;
import graalvm.compiler.lir.InstructionValueProcedure;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.LIRInstruction.OperandFlag;
import graalvm.compiler.lir.LIRInstruction.OperandMode;
import graalvm.compiler.lir.StandardOp;
import graalvm.compiler.lir.StandardOp.LabelOp;
import graalvm.compiler.lir.StandardOp.MoveOp;
import graalvm.compiler.lir.StandardOp.ValueMoveOp;
import graalvm.compiler.lir.Variable;
import graalvm.compiler.lir.alloc.trace.GlobalLivenessInfo;
import graalvm.compiler.lir.alloc.trace.ShadowedRegisterValue;
import graalvm.compiler.lir.alloc.trace.lsra.TraceLinearScanPhase.TraceLinearScan;
import graalvm.compiler.lir.gen.LIRGenerationResult;
import graalvm.compiler.lir.gen.LIRGeneratorTool.MoveFactory;

import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Value;

/**
 * Specialization of {@link graalvm.compiler.lir.alloc.lsra.LinearScanAssignLocationsPhase} that
 * inserts {@link ShadowedRegisterValue}s to describe {@link RegisterValue}s that are also available
 * on the {@link StackSlot stack}.
 */
final class TraceLinearScanAssignLocationsPhase extends TraceLinearScanAllocationPhase
{
    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, Trace trace, MoveFactory spillMoveFactory, RegisterAllocationConfig registerAllocationConfig, TraceBuilderResult traceBuilderResult, TraceLinearScan allocator)
    {
        new Assigner(allocator, spillMoveFactory).assign();
    }

    private static final class Assigner
    {
        private final TraceLinearScan allocator;
        private final MoveFactory spillMoveFactory;

        private Assigner(TraceLinearScan allocator, MoveFactory spillMoveFactory)
        {
            this.allocator = allocator;
            this.spillMoveFactory = spillMoveFactory;
        }

        /**
         * Assigns the allocated location for an LIR instruction operand back into the instruction.
         *
         * @param op current {@link LIRInstruction}
         * @param operand an LIR instruction operand
         * @param mode the usage mode for {@code operand} by the instruction
         * @return the location assigned for the operand
         */
        private Value colorLirOperand(LIRInstruction op, Variable operand, OperandMode mode)
        {
            int opId = op.id();
            TraceInterval interval = allocator.intervalFor(operand);

            if (opId != -1)
            {
                /*
                 * Operands are not changed when an interval is split during allocation, so search
                 * the right interval here.
                 */
                interval = allocator.splitChildAtOpId(interval, opId, mode);
            }

            return getLocation(op, interval, mode);
        }

        private Value getLocation(LIRInstruction op, TraceInterval interval, OperandMode mode)
        {
            if (isIllegal(interval.location()) && interval.canMaterialize())
            {
                if (op instanceof LabelOp)
                {
                    /*
                     * Spilled materialized value in a LabelOp (i.e. incoming): no need for move
                     * resolution so we can ignore it.
                     */
                    return Value.ILLEGAL;
                }
                return new ConstantValue(allocator.getKind(interval), interval.getMaterializedValue());
            }
            return interval.location();
        }

        /**
         * @see InstructionValueProcedure#doValue(LIRInstruction, Value, OperandMode, EnumSet)
         */
        private Value debugInfoProcedure(LIRInstruction op, Value operand, OperandMode valueMode, EnumSet<OperandFlag> flags)
        {
            if (isVirtualStackSlot(operand))
            {
                return operand;
            }
            int tempOpId = op.id();
            OperandMode mode = OperandMode.USE;
            AbstractBlockBase<?> block = allocator.blockForId(tempOpId);
            if (block.getSuccessorCount() == 1 && tempOpId == allocator.getLastLirInstructionId(block))
            {
                /*
                 * Generating debug information for the last instruction of a block. If this
                 * instruction is a branch, spill moves are inserted before this branch and so the
                 * wrong operand would be returned (spill moves at block boundaries are not
                 * considered in the live ranges of intervals).
                 *
                 * Solution: use the first opId of the branch target block instead.
                 */
                final LIRInstruction instr = allocator.getLIR().getLIRforBlock(block).get(allocator.getLIR().getLIRforBlock(block).size() - 1);
                if (instr instanceof StandardOp.JumpOp)
                {
                    throw GraalError.unimplemented("DebugInfo on jumps are not supported!");
                }
            }

            /*
             * Get current location of operand. The operand must be live because debug information
             * is considered when building the intervals if the interval is not live,
             * colorLirOperand will cause an assert on failure.
             */
            Value result = colorLirOperand(op, (Variable) operand, mode);
            return result;
        }

        private void assignBlock(AbstractBlockBase<?> block)
        {
            ArrayList<LIRInstruction> instructions = allocator.getLIR().getLIRforBlock(block);
            handleBlockBegin(block, instructions);
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
                else if (assignLocations(op, instructions, j))
                {
                    hasDead = true;
                }
            }
            handleBlockEnd(block, instructions);

            if (hasDead)
            {
                // Remove null values from the list.
                instructions.removeAll(Collections.singleton(null));
            }
        }

        private void handleBlockBegin(AbstractBlockBase<?> block, ArrayList<LIRInstruction> instructions)
        {
            if (allocator.hasInterTracePredecessor(block))
            {
                /* Only materialize the locations array if there is an incoming inter-trace edge. */
                GlobalLivenessInfo li = allocator.getGlobalLivenessInfo();
                LIRInstruction instruction = instructions.get(0);
                OperandMode mode = OperandMode.DEF;
                int[] live = li.getBlockIn(block);
                Value[] values = calculateBlockBoundaryValues(instruction, live, mode);
                li.setInLocations(block, values);
            }
        }

        private void handleBlockEnd(AbstractBlockBase<?> block, ArrayList<LIRInstruction> instructions)
        {
            if (allocator.hasInterTraceSuccessor(block))
            {
                /* Only materialize the locations array if there is an outgoing inter-trace edge. */
                GlobalLivenessInfo li = allocator.getGlobalLivenessInfo();
                LIRInstruction instruction = instructions.get(instructions.size() - 1);
                OperandMode mode = OperandMode.USE;
                int[] live = li.getBlockOut(block);
                Value[] values = calculateBlockBoundaryValues(instruction, live, mode);
                li.setOutLocations(block, values);
            }
        }

        private Value[] calculateBlockBoundaryValues(LIRInstruction instruction, int[] live, OperandMode mode)
        {
            Value[] values = new Value[live.length];
            for (int i = 0; i < live.length; i++)
            {
                TraceInterval interval = allocator.intervalFor(live[i]);
                Value val = valueAtBlockBoundary(instruction, interval, mode);
                values[i] = val;
            }
            return values;
        }

        private Value valueAtBlockBoundary(LIRInstruction instruction, TraceInterval interval, OperandMode mode)
        {
            if (mode == OperandMode.DEF && interval == null)
            {
                // not needed in this trace
                return Value.ILLEGAL;
            }
            TraceInterval splitInterval = interval.getSplitChildAtOpIdOrNull(instruction.id(), mode);
            if (splitInterval == null)
            {
                // not needed in this branch
                return Value.ILLEGAL;
            }

            if (splitInterval.inMemoryAt(instruction.id()) && isRegister(splitInterval.location()))
            {
                return new ShadowedRegisterValue((RegisterValue) splitInterval.location(), splitInterval.spillSlot());
            }
            return getLocation(instruction, splitInterval, mode);
        }

        private final InstructionValueProcedure assignProc = new InstructionValueProcedure()
        {
            @Override
            public Value doValue(LIRInstruction instruction, Value value, OperandMode mode, EnumSet<OperandFlag> flags)
            {
                if (isVariable(value))
                {
                    return colorLirOperand(instruction, (Variable) value, mode);
                }
                return value;
            }
        };
        private final InstructionValueProcedure debugInfoValueProc = new InstructionValueProcedure()
        {
            @Override
            public Value doValue(LIRInstruction instruction, Value value, OperandMode mode, EnumSet<OperandFlag> flags)
            {
                return debugInfoProcedure(instruction, value, mode, flags);
            }
        };

        /**
         * Assigns the operand of an {@link LIRInstruction}.
         *
         * @param op The {@link LIRInstruction} that should be colored.
         * @param j The index of {@code op} in the {@code instructions} list.
         * @param instructions The instructions of the current block.
         * @return {@code true} if the instruction was deleted.
         */
        private boolean assignLocations(LIRInstruction op, ArrayList<LIRInstruction> instructions, int j)
        {
            // remove useless moves
            if (MoveOp.isMoveOp(op))
            {
                AllocatableValue result = MoveOp.asMoveOp(op).getResult();
                if (isVariable(result) && allocator.isMaterialized(asVariable(result), op.id(), OperandMode.DEF))
                {
                    /*
                     * This happens if a materializable interval is originally not spilled but then
                     * kicked out in LinearScanWalker.splitForSpilling(). When kicking out such an
                     * interval this move operation was already generated.
                     */
                    instructions.set(j, null);
                    return true;
                }
            }

            op.forEachInput(assignProc);
            op.forEachAlive(assignProc);
            op.forEachTemp(assignProc);
            op.forEachOutput(assignProc);

            // compute reference map and debug information
            op.forEachState(debugInfoValueProc);

            // remove useless moves
            if (ValueMoveOp.isValueMoveOp(op))
            {
                ValueMoveOp move = ValueMoveOp.asValueMoveOp(op);
                if (move.getInput().equals(move.getResult()))
                {
                    instructions.set(j, null);
                    return true;
                }
                if (isStackSlotValue(move.getInput()) && isStackSlotValue(move.getResult()))
                {
                    // rewrite stack to stack moves
                    instructions.set(j, spillMoveFactory.createStackMove(move.getResult(), move.getInput()));
                }
            }
            return false;
        }

        private void assign()
        {
            for (AbstractBlockBase<?> block : allocator.sortedBlocks())
            {
                assignBlock(block);
            }
        }
    }
}
