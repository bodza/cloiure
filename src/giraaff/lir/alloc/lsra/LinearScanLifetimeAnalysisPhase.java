package giraaff.lir.alloc.lsra;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.EnumSet;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterArray;
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.Value;
import jdk.vm.ci.meta.ValueKind;

import giraaff.core.common.LIRKind;
import giraaff.core.common.PermanentBailoutException;
import giraaff.core.common.alloc.ComputeBlockOrder;
import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.core.common.util.BitMap2D;
import giraaff.lir.InstructionValueConsumer;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstruction.OperandMode;
import giraaff.lir.LIRValueUtil;
import giraaff.lir.StandardOp.LoadConstantOp;
import giraaff.lir.StandardOp.ValueMoveOp;
import giraaff.lir.ValueConsumer;
import giraaff.lir.alloc.lsra.Interval.RegisterPriority;
import giraaff.lir.alloc.lsra.Interval.SpillState;
import giraaff.lir.alloc.lsra.LinearScan.BlockData;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.phases.AllocationPhase.AllocationContext;
import giraaff.util.GraalError;

public class LinearScanLifetimeAnalysisPhase extends LinearScanAllocationPhase
{
    protected final LinearScan allocator;

    protected LinearScanLifetimeAnalysisPhase(LinearScan linearScan)
    {
        allocator = linearScan;
    }

    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, AllocationContext context)
    {
        numberInstructions();
        computeLocalLiveSets();
        computeGlobalLiveSets();
        buildIntervals();
    }

    /**
     * Bit set for each variable that is contained in each loop.
     */
    private BitMap2D intervalInLoop;

    boolean isIntervalInLoop(int interval, int loop)
    {
        return intervalInLoop.at(interval, loop);
    }

    /**
     * Numbers all instructions in all blocks. The numbering follows the
     * {@linkplain ComputeBlockOrder linear scan order}.
     */
    protected void numberInstructions()
    {
        allocator.initIntervals();

        ValueConsumer setVariableConsumer = (value, mode, flags) ->
        {
            if (LIRValueUtil.isVariable(value))
            {
                allocator.getOrCreateInterval(LIRValueUtil.asVariable(value));
            }
        };

        // Assign IDs to LIR nodes and build a mapping, lirOps, from ID to LIRInstruction node.
        int numInstructions = 0;
        for (AbstractBlockBase<?> block : allocator.sortedBlocks())
        {
            numInstructions += allocator.getLIR().getLIRforBlock(block).size();
        }

        // initialize with correct length
        allocator.initOpIdMaps(numInstructions);

        int opId = 0;
        int index = 0;
        for (AbstractBlockBase<?> block : allocator.sortedBlocks())
        {
            allocator.initBlockData(block);

            ArrayList<LIRInstruction> instructions = allocator.getLIR().getLIRforBlock(block);

            int numInst = instructions.size();
            for (int j = 0; j < numInst; j++)
            {
                LIRInstruction op = instructions.get(j);
                op.setId(opId);

                allocator.putOpIdMaps(index, op, block);

                op.visitEachTemp(setVariableConsumer);
                op.visitEachOutput(setVariableConsumer);

                index++;
                opId += 2; // numbering of lirOps by two
            }
        }
    }

    /**
     * Computes local live sets (i.e. {@link BlockData#liveGen} and {@link BlockData#liveKill})
     * separately for each block.
     */
    void computeLocalLiveSets()
    {
        int liveSize = allocator.liveSetSize();

        intervalInLoop = new BitMap2D(allocator.operandSize(), allocator.numLoops());

        try
        {
            final BitSet liveGenScratch = new BitSet(liveSize);
            final BitSet liveKillScratch = new BitSet(liveSize);
            // iterate all blocks
            for (final AbstractBlockBase<?> block : allocator.sortedBlocks())
            {
                liveGenScratch.clear();
                liveKillScratch.clear();

                ArrayList<LIRInstruction> instructions = allocator.getLIR().getLIRforBlock(block);
                int numInst = instructions.size();

                ValueConsumer useConsumer = (operand, mode, flags) ->
                {
                    if (LIRValueUtil.isVariable(operand))
                    {
                        int operandNum = allocator.operandNumber(operand);
                        if (!liveKillScratch.get(operandNum))
                        {
                            liveGenScratch.set(operandNum);
                        }
                        if (block.getLoop() != null)
                        {
                            intervalInLoop.setBit(operandNum, block.getLoop().getIndex());
                        }
                    }
                };
                ValueConsumer defConsumer = (operand, mode, flags) ->
                {
                    if (LIRValueUtil.isVariable(operand))
                    {
                        int varNum = allocator.operandNumber(operand);
                        liveKillScratch.set(varNum);
                        if (block.getLoop() != null)
                        {
                            intervalInLoop.setBit(varNum, block.getLoop().getIndex());
                        }
                    }
                };

                // iterate all instructions of the block
                for (int j = 0; j < numInst; j++)
                {
                    final LIRInstruction op = instructions.get(j);

                    op.visitEachInput(useConsumer);
                    op.visitEachAlive(useConsumer);

                    op.visitEachTemp(defConsumer);
                    op.visitEachOutput(defConsumer);
                }

                BlockData blockSets = allocator.getBlockData(block);
                blockSets.liveGen = trimClone(liveGenScratch);
                blockSets.liveKill = trimClone(liveKillScratch);
                // sticky size, will get non-sticky in computeGlobalLiveSets
                blockSets.liveIn = new BitSet(0);
                blockSets.liveOut = new BitSet(0);
            }
        }
        catch (OutOfMemoryError oom)
        {
            throw new PermanentBailoutException(oom, "Out-of-memory during live set allocation of size %d", liveSize);
        }
    }

    /**
     * Performs a backward dataflow analysis to compute global live sets (i.e.
     * {@link BlockData#liveIn} and {@link BlockData#liveOut}) for each block.
     */
    protected void computeGlobalLiveSets()
    {
        int numBlocks = allocator.blockCount();
        boolean changeOccurred;
        boolean changeOccurredInBlock;
        int iterationCount = 0;
        BitSet scratch = new BitSet(allocator.liveSetSize()); // scratch set for calculations

        /*
         * Perform a backward dataflow analysis to compute liveOut and liveIn for each block.
         * The loop is executed until a fixpoint is reached (no changes in an iteration).
         */
        do
        {
            changeOccurred = false;

            // iterate all blocks in reverse order
            for (int i = numBlocks - 1; i >= 0; i--)
            {
                AbstractBlockBase<?> block = allocator.blockAt(i);
                BlockData blockSets = allocator.getBlockData(block);

                changeOccurredInBlock = false;

                // liveOut(block) is the union of liveIn(sux), for successors sux of block.
                int n = block.getSuccessorCount();
                if (n > 0)
                {
                    scratch.clear();
                    // block has successors
                    if (n > 0)
                    {
                        for (AbstractBlockBase<?> successor : block.getSuccessors())
                        {
                            scratch.or(allocator.getBlockData(successor).liveIn);
                        }
                    }

                    if (!blockSets.liveOut.equals(scratch))
                    {
                        blockSets.liveOut = trimClone(scratch);

                        changeOccurred = true;
                        changeOccurredInBlock = true;
                    }
                }

                if (iterationCount == 0 || changeOccurredInBlock)
                {
                    /*
                     * liveIn(block) is the union of liveGen(block) with (liveOut(block) & !liveKill(block)).
                     *
                     * Note: liveIn has to be computed only in first iteration or if liveOut has changed!
                     * Note: liveIn set can only grow, never shrink. No need to clear it.
                     */
                    BitSet liveIn = blockSets.liveIn;
                    /*
                     * BitSet#or will call BitSet#ensureSize (since the bit set is of length
                     * 0 initially) and set sticky to false
                     */
                    liveIn.or(blockSets.liveOut);
                    liveIn.andNot(blockSets.liveKill);
                    liveIn.or(blockSets.liveGen);

                    liveIn.clone(); // trimToSize()
                }
            }
            iterationCount++;

            if (changeOccurred && iterationCount > 50)
            {
                // Very unlikely, should never happen: If it happens we cannot guarantee it won't happen again.
                throw new PermanentBailoutException("too many iterations in computeGlobalLiveSets");
            }
        } while (changeOccurred);

        // check that the liveIn set of the first block is empty
        AbstractBlockBase<?> startBlock = allocator.getLIR().getControlFlowGraph().getStartBlock();
        if (allocator.getBlockData(startBlock).liveIn.cardinality() != 0)
        {
            // bailout if this occurs in product mode.
            throw new GraalError("liveIn set of first block must be empty: " + allocator.getBlockData(startBlock).liveIn);
        }
    }

    /**
     * Creates a trimmed copy a bit set.
     *
     * {@link BitSet#clone()} cannot be used since it will not {@linkplain BitSet#trimToSize trim}
     * the array if the bit set is {@linkplain BitSet#sizeIsSticky sticky}.
     */
    @SuppressWarnings("javadoc")
    private static BitSet trimClone(BitSet set)
    {
        BitSet trimmedSet = new BitSet(0); // zero-length words array, sticky
        trimmedSet.or(set); // words size ensured to be words-in-use of set,
                            // also makes it non-sticky
        return trimmedSet;
    }

    protected void addUse(AllocatableValue operand, int from, int to, RegisterPriority registerPriority, ValueKind<?> kind)
    {
        if (!allocator.isProcessed(operand))
        {
            return;
        }

        Interval interval = allocator.getOrCreateInterval(operand);
        if (!kind.equals(LIRKind.Illegal))
        {
            interval.setKind(kind);
        }

        interval.addRange(from, to);

        // Register use position at even instruction id.
        interval.addUsePos(to & ~1, registerPriority);
    }

    protected void addTemp(AllocatableValue operand, int tempPos, RegisterPriority registerPriority, ValueKind<?> kind)
    {
        if (!allocator.isProcessed(operand))
        {
            return;
        }

        Interval interval = allocator.getOrCreateInterval(operand);
        if (!kind.equals(LIRKind.Illegal))
        {
            interval.setKind(kind);
        }

        interval.addRange(tempPos, tempPos + 1);
        interval.addUsePos(tempPos, registerPriority);
        interval.addMaterializationValue(null);
    }

    protected void addDef(AllocatableValue operand, LIRInstruction op, RegisterPriority registerPriority, ValueKind<?> kind)
    {
        if (!allocator.isProcessed(operand))
        {
            return;
        }
        int defPos = op.id();

        Interval interval = allocator.getOrCreateInterval(operand);
        if (!kind.equals(LIRKind.Illegal))
        {
            interval.setKind(kind);
        }

        Range r = interval.first();
        if (r.from <= defPos)
        {
            /*
             * Update the starting point (when a range is first created for a use, its start is the
             * beginning of the current block until a def is encountered).
             */
            r.from = defPos;
            interval.addUsePos(defPos, registerPriority);
        }
        else
        {
            // Dead value - make vacuous interval also add register priority for dead intervals
            interval.addRange(defPos, defPos + 1);
            interval.addUsePos(defPos, registerPriority);
        }

        changeSpillDefinitionPos(op, operand, interval, defPos);
        if (registerPriority == RegisterPriority.None && interval.spillState().ordinal() <= SpillState.StartInMemory.ordinal() && ValueUtil.isStackSlot(operand))
        {
            // detection of method-parameters and roundfp-results
            interval.setSpillState(SpillState.StartInMemory);
        }
        interval.addMaterializationValue(getMaterializedValue(op, operand, interval));
    }

    /**
     * Optimizes moves related to incoming stack based arguments. The interval for the destination
     * of such moves is assigned the stack slot (which is in the caller's frame) as its spill slot.
     */
    protected void handleMethodArguments(LIRInstruction op)
    {
        if (ValueMoveOp.isValueMoveOp(op))
        {
            ValueMoveOp move = ValueMoveOp.asValueMoveOp(op);
            if (optimizeMethodArgument(move.getInput()))
            {
                StackSlot slot = ValueUtil.asStackSlot(move.getInput());

                Interval interval = allocator.intervalFor(move.getResult());
                interval.setSpillSlot(slot);
                interval.assignLocation(slot);
            }
        }
    }

    protected void addRegisterHint(final LIRInstruction op, final Value targetValue, OperandMode mode, EnumSet<OperandFlag> flags, final boolean hintAtDef)
    {
        if (flags.contains(OperandFlag.HINT) && LinearScan.isVariableOrRegister(targetValue))
        {
            op.forEachRegisterHint(targetValue, mode, (registerHint, valueMode, valueFlags) ->
            {
                if (LinearScan.isVariableOrRegister(registerHint))
                {
                    Interval from = allocator.getOrCreateInterval((AllocatableValue) registerHint);
                    Interval to = allocator.getOrCreateInterval((AllocatableValue) targetValue);

                    // hints always point from def to use
                    if (hintAtDef)
                    {
                        to.setLocationHint(from);
                    }
                    else
                    {
                        from.setLocationHint(to);
                    }

                    return registerHint;
                }
                return null;
            });
        }
    }

    /**
     * Eliminates moves from register to stack if the stack slot is known to be correct.
     */
    protected void changeSpillDefinitionPos(LIRInstruction op, AllocatableValue operand, Interval interval, int defPos)
    {
        switch (interval.spillState())
        {
            case NoDefinitionFound:
                interval.setSpillDefinitionPos(defPos);
                interval.setSpillState(SpillState.NoSpillStore);
                break;

            case NoSpillStore:
                if (defPos < interval.spillDefinitionPos() - 2)
                {
                    // second definition found, so no spill optimization possible for this interval
                    interval.setSpillState(SpillState.NoOptimization);
                }
                else
                {
                    // two consecutive definitions (because of two-operand LIR form)
                }
                break;

            case NoOptimization:
                // nothing to do
                break;

            default:
                throw GraalError.shouldNotReachHere("other states not allowed at this time");
        }
    }

    private static boolean optimizeMethodArgument(Value value)
    {
        /*
         * Object method arguments that are passed on the stack are currently not optimized because
         * this requires that the runtime visits method arguments during stack walking.
         */
        return ValueUtil.isStackSlot(value) && ValueUtil.asStackSlot(value).isInCallerFrame() && LIRKind.isValue(value);
    }

    /**
     * Determines the register priority for an instruction's output/result operand.
     */
    protected RegisterPriority registerPriorityOfOutputOperand(LIRInstruction op)
    {
        if (ValueMoveOp.isValueMoveOp(op))
        {
            ValueMoveOp move = ValueMoveOp.asValueMoveOp(op);
            if (optimizeMethodArgument(move.getInput()))
            {
                return RegisterPriority.None;
            }
        }

        // all other operands require a register
        return RegisterPriority.MustHaveRegister;
    }

    /**
     * Determines the priority which with an instruction's input operand will be allocated a register.
     */
    protected static RegisterPriority registerPriorityOfInputOperand(EnumSet<OperandFlag> flags)
    {
        if (flags.contains(OperandFlag.STACK))
        {
            return RegisterPriority.ShouldHaveRegister;
        }
        // all other operands require a register
        return RegisterPriority.MustHaveRegister;
    }

    protected void buildIntervals()
    {
        InstructionValueConsumer outputConsumer = (op, operand, mode, flags) ->
        {
            if (LinearScan.isVariableOrRegister(operand))
            {
                addDef((AllocatableValue) operand, op, registerPriorityOfOutputOperand(op), operand.getValueKind());
                addRegisterHint(op, operand, mode, flags, true);
            }
        };

        InstructionValueConsumer tempConsumer = (op, operand, mode, flags) ->
        {
            if (LinearScan.isVariableOrRegister(operand))
            {
                addTemp((AllocatableValue) operand, op.id(), RegisterPriority.MustHaveRegister, operand.getValueKind());
                addRegisterHint(op, operand, mode, flags, false);
            }
        };

        InstructionValueConsumer aliveConsumer = (op, operand, mode, flags) ->
        {
            if (LinearScan.isVariableOrRegister(operand))
            {
                RegisterPriority p = registerPriorityOfInputOperand(flags);
                int opId = op.id();
                int blockFrom = allocator.getFirstLirInstructionId((allocator.blockForId(opId)));
                addUse((AllocatableValue) operand, blockFrom, opId + 1, p, operand.getValueKind());
                addRegisterHint(op, operand, mode, flags, false);
            }
        };

        InstructionValueConsumer inputConsumer = (op, operand, mode, flags) ->
        {
            if (LinearScan.isVariableOrRegister(operand))
            {
                int opId = op.id();
                int blockFrom = allocator.getFirstLirInstructionId((allocator.blockForId(opId)));
                RegisterPriority p = registerPriorityOfInputOperand(flags);
                addUse((AllocatableValue) operand, blockFrom, opId, p, operand.getValueKind());
                addRegisterHint(op, operand, mode, flags, false);
            }
        };

        // create a list with all caller-save registers (cpu, fpu, xmm)
        RegisterArray callerSaveRegs = allocator.getRegisterAllocationConfig().getRegisterConfig().getCallerSaveRegisters();

        // iterate all blocks in reverse order
        for (int i = allocator.blockCount() - 1; i >= 0; i--)
        {
            AbstractBlockBase<?> block = allocator.blockAt(i);
            ArrayList<LIRInstruction> instructions = allocator.getLIR().getLIRforBlock(block);
            final int blockFrom = allocator.getFirstLirInstructionId(block);
            int blockTo = allocator.getLastLirInstructionId(block);

            // Update intervals for operands live at the end of this block;
            BitSet live = allocator.getBlockData(block).liveOut;
            for (int operandNum = live.nextSetBit(0); operandNum >= 0; operandNum = live.nextSetBit(operandNum + 1))
            {
                AllocatableValue operand = allocator.intervalFor(operandNum).operand;

                addUse(operand, blockFrom, blockTo + 2, RegisterPriority.None, LIRKind.Illegal);

                /*
                 * Add special use positions for loop-end blocks when the interval is used
                 * anywhere inside this loop. It's possible that the block was part of a
                 * non-natural loop, so it might have an invalid loop index.
                 */
                if (block.isLoopEnd() && block.getLoop() != null && isIntervalInLoop(operandNum, block.getLoop().getIndex()))
                {
                    allocator.intervalFor(operandNum).addUsePos(blockTo + 1, RegisterPriority.LiveAtLoopEnd);
                }
            }

            /*
             * Iterate all instructions of the block in reverse order. definitions of
             * intervals are processed before uses.
             */
            for (int j = instructions.size() - 1; j >= 0; j--)
            {
                final LIRInstruction op = instructions.get(j);
                final int opId = op.id();

                // add a temp range for each register if operation destroys
                // caller-save registers
                if (op.destroysCallerSavedRegisters())
                {
                    for (Register r : callerSaveRegs)
                    {
                        if (allocator.attributes(r).isAllocatable())
                        {
                            addTemp(r.asValue(), opId, RegisterPriority.None, LIRKind.Illegal);
                        }
                    }
                }

                op.visitEachOutput(outputConsumer);
                op.visitEachTemp(tempConsumer);
                op.visitEachAlive(aliveConsumer);
                op.visitEachInput(inputConsumer);

                // special steps for some instructions (especially moves)
                handleMethodArguments(op);
            }
        }

        /*
         * Add the range [0, 1] to all fixed intervals. The register allocator need not handle
         * unhandled fixed intervals.
         */
        for (Interval interval : allocator.intervals())
        {
            if (interval != null && ValueUtil.isRegister(interval.operand))
            {
                interval.addRange(0, 1);
            }
        }
    }

    /**
     * Returns a value for a interval definition, which can be used for re-materialization.
     *
     * @param op An instruction which defines a value
     * @param operand The destination operand of the instruction
     * @param interval The interval for this defined value.
     * @return Returns the value which is moved to the instruction and which can be reused at all
     *         reload-locations in case the interval of this instruction is spilled. Currently this
     *         can only be a {@link JavaConstant}.
     */
    protected Constant getMaterializedValue(LIRInstruction op, Value operand, Interval interval)
    {
        if (LoadConstantOp.isLoadConstantOp(op))
        {
            LoadConstantOp move = LoadConstantOp.asLoadConstantOp(op);

            if (!allocator.neverSpillConstants())
            {
                /*
                 * Check if the interval has any uses which would accept an stack location (priority
                 * == ShouldHaveRegister). Rematerialization of such intervals can result in a
                 * degradation, because rematerialization always inserts a constant load, even if
                 * the value is not needed in a register.
                 */
                Interval.UsePosList usePosList = interval.usePosList();
                int numUsePos = usePosList.size();
                for (int useIdx = 0; useIdx < numUsePos; useIdx++)
                {
                    Interval.RegisterPriority priority = usePosList.registerPriority(useIdx);
                    if (priority == Interval.RegisterPriority.ShouldHaveRegister)
                    {
                        return null;
                    }
                }
            }
            return move.getConstant();
        }
        return null;
    }
}
