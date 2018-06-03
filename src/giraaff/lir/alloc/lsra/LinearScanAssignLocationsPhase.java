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
import giraaff.lir.StandardOp.MoveOp;
import giraaff.lir.StandardOp.ValueMoveOp;
import giraaff.lir.Variable;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.phases.AllocationPhase.AllocationContext;

///
// Phase 7: Assign register numbers back to LIR.
///
// @class LinearScanAssignLocationsPhase
public final class LinearScanAssignLocationsPhase extends LinearScanAllocationPhase
{
    // @field
    protected final LinearScan ___allocator;

    // @cons
    public LinearScanAssignLocationsPhase(LinearScan __allocator)
    {
        super();
        this.___allocator = __allocator;
    }

    @Override
    protected void run(TargetDescription __target, LIRGenerationResult __lirGenRes, AllocationContext __context)
    {
        assignLocations();
    }

    ///
    // Assigns the allocated location for an LIR instruction operand back into the instruction.
    //
    // @param op current {@link LIRInstruction}
    // @param operand an LIR instruction operand
    // @param mode the usage mode for {@code operand} by the instruction
    // @return the location assigned for the operand
    ///
    protected Value colorLirOperand(LIRInstruction __op, Variable __operand, OperandMode __mode)
    {
        int __opId = __op.id();
        Interval __interval = this.___allocator.intervalFor(__operand);

        if (__opId != -1)
        {
            // Operands are not changed when an interval is split during allocation, so search the right interval here.
            __interval = this.___allocator.splitChildAtOpId(__interval, __opId, __mode);
        }

        if (ValueUtil.isIllegal(__interval.location()) && __interval.canMaterialize())
        {
            return new ConstantValue(__interval.kind(), __interval.getMaterializedValue());
        }
        return __interval.location();
    }

    private void assignLocations(ArrayList<LIRInstruction> __instructions)
    {
        int __numInst = __instructions.size();
        boolean __hasDead = false;

        for (int __j = 0; __j < __numInst; __j++)
        {
            final LIRInstruction __op = __instructions.get(__j);
            if (__op == null)
            {
                // this can happen when spill-moves are removed in eliminateSpillMoves
                __hasDead = true;
            }
            else if (assignLocations(__op))
            {
                __instructions.set(__j, null);
                __hasDead = true;
            }
        }

        if (__hasDead)
        {
            // Remove null values from the list.
            __instructions.removeAll(Collections.singleton(null));
        }
    }

    // @closure
    private final InstructionValueProcedure assignProc = new InstructionValueProcedure()
    {
        @Override
        public Value doValue(LIRInstruction __instruction, Value __value, OperandMode __mode, EnumSet<OperandFlag> __flags)
        {
            if (LIRValueUtil.isVariable(__value))
            {
                return colorLirOperand(__instruction, (Variable) __value, __mode);
            }
            return __value;
        }
    };

    ///
    // Assigns the operand of an {@link LIRInstruction}.
    //
    // @param op The {@link LIRInstruction} that should be colored.
    // @return {@code true} if the instruction should be deleted.
    ///
    protected boolean assignLocations(LIRInstruction __op)
    {
        // remove useless moves
        if (MoveOp.isMoveOp(__op))
        {
            AllocatableValue __result = MoveOp.asMoveOp(__op).getResult();
            if (LIRValueUtil.isVariable(__result) && this.___allocator.isMaterialized(__result, __op.id(), OperandMode.DEF))
            {
                // This happens if a materializable interval is originally not spilled but then
                // kicked out in LinearScanWalker.splitForSpilling(). When kicking out such an
                // interval this move operation was already generated.
                return true;
            }
        }

        __op.forEachInput(assignProc);
        __op.forEachAlive(assignProc);
        __op.forEachTemp(assignProc);
        __op.forEachOutput(assignProc);

        // remove useless moves
        if (ValueMoveOp.isValueMoveOp(__op))
        {
            ValueMoveOp __move = ValueMoveOp.asValueMoveOp(__op);
            if (__move.getInput().equals(__move.getResult()))
            {
                return true;
            }
        }
        return false;
    }

    private void assignLocations()
    {
        for (AbstractBlockBase<?> __block : this.___allocator.sortedBlocks())
        {
            assignLocations(this.___allocator.getLIR().getLIRforBlock(__block));
        }
    }
}
