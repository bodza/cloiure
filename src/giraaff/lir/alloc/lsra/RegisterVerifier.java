package giraaff.lir.alloc.lsra;

import java.util.ArrayList;
import java.util.EnumSet;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.Value;

import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.core.common.cfg.BlockMap;
import giraaff.debug.GraalError;
import giraaff.lir.InstructionValueConsumer;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstruction.OperandMode;

/**
 */
final class RegisterVerifier
{
    LinearScan allocator;
    ArrayList<AbstractBlockBase<?>> workList; // all blocks that must be processed
    BlockMap<Interval[]> savedStates; // saved information of previous check

    // simplified access to methods of LinearScan
    Interval intervalAt(Value operand)
    {
        return allocator.intervalFor(operand);
    }

    // currently, only registers are processed
    int stateSize()
    {
        return allocator.maxRegisterNumber() + 1;
    }

    // accessors
    Interval[] stateForBlock(AbstractBlockBase<?> block)
    {
        return savedStates.get(block);
    }

    void setStateForBlock(AbstractBlockBase<?> block, Interval[] savedState)
    {
        savedStates.put(block, savedState);
    }

    void addToWorkList(AbstractBlockBase<?> block)
    {
        if (!workList.contains(block))
        {
            workList.add(block);
        }
    }

    RegisterVerifier(LinearScan allocator)
    {
        this.allocator = allocator;
        workList = new ArrayList<>(16);
        this.savedStates = new BlockMap<>(allocator.getLIR().getControlFlowGraph());
    }

    private void processBlock(AbstractBlockBase<?> block)
    {
        // must copy state because it is modified
        Interval[] inputState = copy(stateForBlock(block));

        // process all operations of the block
        processOperations(block, inputState);

        // iterate all successors
        for (AbstractBlockBase<?> succ : block.getSuccessors())
        {
            processSuccessor(succ, inputState);
        }
    }

    private void processSuccessor(AbstractBlockBase<?> block, Interval[] inputState)
    {
        Interval[] savedState = stateForBlock(block);

        if (savedState != null)
        {
            // this block was already processed before.
            // check if new inputState is consistent with savedState

            boolean savedStateCorrect = true;
            for (int i = 0; i < stateSize(); i++)
            {
                if (inputState[i] != savedState[i])
                {
                    // current inputState and previous savedState assume a different
                    // interval in this register . assume that this register is invalid
                    if (savedState[i] != null)
                    {
                        // invalidate old calculation only if it assumed that
                        // register was valid. when the register was already invalid,
                        // then the old calculation was correct.
                        savedStateCorrect = false;
                        savedState[i] = null;
                    }
                }
            }

            if (savedStateCorrect)
            {
                // already processed block with correct inputState
            }
            else
            {
                // must re-visit this block
                addToWorkList(block);
            }
        }
        else
        {
            // block was not processed before, so set initial inputState

            setStateForBlock(block, copy(inputState));
            addToWorkList(block);
        }
    }

    static Interval[] copy(Interval[] inputState)
    {
        return inputState.clone();
    }

    static void statePut(Interval[] inputState, Value location, Interval interval)
    {
        if (location != null && ValueUtil.isRegister(location))
        {
            Register reg = ValueUtil.asRegister(location);
            int regNum = reg.number;

            inputState[regNum] = interval;
        }
    }

    static boolean checkState(AbstractBlockBase<?> block, LIRInstruction op, Interval[] inputState, Value operand, Value reg, Interval interval)
    {
        if (reg != null && ValueUtil.isRegister(reg))
        {
            if (inputState[ValueUtil.asRegister(reg).number] != interval)
            {
                throw new GraalError("Error in register allocation: operation (%s) in block %s expected register %s (operand %s) to contain the value of interval %s but data-flow says it contains interval %s", op, block, reg, operand, interval, inputState[ValueUtil.asRegister(reg).number]);
            }
        }
        return true;
    }

    void processOperations(AbstractBlockBase<?> block, final Interval[] inputState)
    {
        ArrayList<LIRInstruction> ops = allocator.getLIR().getLIRforBlock(block);
        InstructionValueConsumer useConsumer = new InstructionValueConsumer()
        {
            @Override
            public void visitValue(LIRInstruction op, Value operand, OperandMode mode, EnumSet<OperandFlag> flags)
            {
                // we skip spill moves inserted by the spill position optimization
                if (LinearScan.isVariableOrRegister(operand) && allocator.isProcessed(operand) && op.id() != LinearScan.DOMINATOR_SPILL_MOVE_ID)
                {
                    Interval interval = intervalAt(operand);
                    if (op.id() != -1)
                    {
                        interval = interval.getSplitChildAtOpId(op.id(), mode, allocator);
                    }
                }
            }
        };

        InstructionValueConsumer defConsumer = (op, operand, mode, flags) ->
        {
            if (LinearScan.isVariableOrRegister(operand) && allocator.isProcessed(operand))
            {
                Interval interval = intervalAt(operand);
                if (op.id() != -1)
                {
                    interval = interval.getSplitChildAtOpId(op.id(), mode, allocator);
                }

                statePut(inputState, interval.location(), interval.splitParent());
            }
        };

        // visit all instructions of the block
        for (int i = 0; i < ops.size(); i++)
        {
            final LIRInstruction op = ops.get(i);

            // check if input operands are correct
            op.visitEachInput(useConsumer);
            // invalidate all caller save registers at calls
            if (op.destroysCallerSavedRegisters())
            {
                for (Register r : allocator.getRegisterAllocationConfig().getRegisterConfig().getCallerSaveRegisters())
                {
                    statePut(inputState, r.asValue(), null);
                }
            }
            op.visitEachAlive(useConsumer);
            // set temp operands (some operations use temp operands also as output operands, so
            // can't set them null)
            op.visitEachTemp(defConsumer);
            // set output operands
            op.visitEachOutput(defConsumer);
        }
    }
}
