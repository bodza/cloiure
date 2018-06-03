package giraaff.lir.alloc.lsra;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.EnumSet;

import jdk.vm.ci.code.BailoutException;
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

// @class LinearScanLifetimeAnalysisPhase
public class LinearScanLifetimeAnalysisPhase extends LinearScanAllocationPhase
{
    // @field
    protected final LinearScan allocator;

    // @cons
    protected LinearScanLifetimeAnalysisPhase(LinearScan __linearScan)
    {
        super();
        allocator = __linearScan;
    }

    @Override
    protected void run(TargetDescription __target, LIRGenerationResult __lirGenRes, AllocationContext __context)
    {
        numberInstructions();
        computeLocalLiveSets();
        computeGlobalLiveSets();
        buildIntervals();
    }

    /**
     * Bit set for each variable that is contained in each loop.
     */
    // @field
    private BitMap2D intervalInLoop;

    boolean isIntervalInLoop(int __interval, int __loop)
    {
        return intervalInLoop.at(__interval, __loop);
    }

    /**
     * Numbers all instructions in all blocks. The numbering follows the
     * {@linkplain ComputeBlockOrder linear scan order}.
     */
    protected void numberInstructions()
    {
        allocator.initIntervals();

        ValueConsumer __setVariableConsumer = (__value, __mode, __flags) ->
        {
            if (LIRValueUtil.isVariable(__value))
            {
                allocator.getOrCreateInterval(LIRValueUtil.asVariable(__value));
            }
        };

        // Assign IDs to LIR nodes and build a mapping, lirOps, from ID to LIRInstruction node.
        int __numInstructions = 0;
        for (AbstractBlockBase<?> __block : allocator.sortedBlocks())
        {
            __numInstructions += allocator.getLIR().getLIRforBlock(__block).size();
        }

        // initialize with correct length
        allocator.initOpIdMaps(__numInstructions);

        int __opId = 0;
        int __index = 0;
        for (AbstractBlockBase<?> __block : allocator.sortedBlocks())
        {
            allocator.initBlockData(__block);

            ArrayList<LIRInstruction> __instructions = allocator.getLIR().getLIRforBlock(__block);

            int __numInst = __instructions.size();
            for (int __j = 0; __j < __numInst; __j++)
            {
                LIRInstruction __op = __instructions.get(__j);
                __op.setId(__opId);

                allocator.putOpIdMaps(__index, __op, __block);

                __op.visitEachTemp(__setVariableConsumer);
                __op.visitEachOutput(__setVariableConsumer);

                __index++;
                __opId += 2; // numbering of lirOps by two
            }
        }
    }

    /**
     * Computes local live sets (i.e. {@link BlockData#liveGen} and {@link BlockData#liveKill})
     * separately for each block.
     */
    void computeLocalLiveSets()
    {
        int __liveSize = allocator.liveSetSize();

        intervalInLoop = new BitMap2D(allocator.operandSize(), allocator.numLoops());

        try
        {
            final BitSet __liveGenScratch = new BitSet(__liveSize);
            final BitSet __liveKillScratch = new BitSet(__liveSize);
            // iterate all blocks
            for (final AbstractBlockBase<?> __block : allocator.sortedBlocks())
            {
                __liveGenScratch.clear();
                __liveKillScratch.clear();

                ArrayList<LIRInstruction> __instructions = allocator.getLIR().getLIRforBlock(__block);
                int __numInst = __instructions.size();

                ValueConsumer __useConsumer = (__operand, __mode, __flags) ->
                {
                    if (LIRValueUtil.isVariable(__operand))
                    {
                        int __operandNum = allocator.operandNumber(__operand);
                        if (!__liveKillScratch.get(__operandNum))
                        {
                            __liveGenScratch.set(__operandNum);
                        }
                        if (__block.getLoop() != null)
                        {
                            intervalInLoop.setBit(__operandNum, __block.getLoop().getIndex());
                        }
                    }
                };
                ValueConsumer __defConsumer = (__operand, __mode, __flags) ->
                {
                    if (LIRValueUtil.isVariable(__operand))
                    {
                        int __varNum = allocator.operandNumber(__operand);
                        __liveKillScratch.set(__varNum);
                        if (__block.getLoop() != null)
                        {
                            intervalInLoop.setBit(__varNum, __block.getLoop().getIndex());
                        }
                    }
                };

                // iterate all instructions of the block
                for (int __j = 0; __j < __numInst; __j++)
                {
                    final LIRInstruction __op = __instructions.get(__j);

                    __op.visitEachInput(__useConsumer);
                    __op.visitEachAlive(__useConsumer);

                    __op.visitEachTemp(__defConsumer);
                    __op.visitEachOutput(__defConsumer);
                }

                BlockData __blockSets = allocator.getBlockData(__block);
                __blockSets.liveGen = trimClone(__liveGenScratch);
                __blockSets.liveKill = trimClone(__liveKillScratch);
                // sticky size, will get non-sticky in computeGlobalLiveSets
                __blockSets.liveIn = new BitSet(0);
                __blockSets.liveOut = new BitSet(0);
            }
        }
        catch (OutOfMemoryError __oom)
        {
            throw new BailoutException(__oom, "out-of-memory during live set allocation of size %d", __liveSize);
        }
    }

    /**
     * Performs a backward dataflow analysis to compute global live sets (i.e.
     * {@link BlockData#liveIn} and {@link BlockData#liveOut}) for each block.
     */
    protected void computeGlobalLiveSets()
    {
        int __numBlocks = allocator.blockCount();
        boolean __changeOccurred;
        boolean __changeOccurredInBlock;
        int __iterationCount = 0;
        BitSet __scratch = new BitSet(allocator.liveSetSize()); // scratch set for calculations

        /*
         * Perform a backward dataflow analysis to compute liveOut and liveIn for each block.
         * The loop is executed until a fixpoint is reached (no changes in an iteration).
         */
        do
        {
            __changeOccurred = false;

            // iterate all blocks in reverse order
            for (int __i = __numBlocks - 1; __i >= 0; __i--)
            {
                AbstractBlockBase<?> __block = allocator.blockAt(__i);
                BlockData __blockSets = allocator.getBlockData(__block);

                __changeOccurredInBlock = false;

                // liveOut(block) is the union of liveIn(sux), for successors sux of block.
                int __n = __block.getSuccessorCount();
                if (__n > 0)
                {
                    __scratch.clear();
                    // block has successors
                    if (__n > 0)
                    {
                        for (AbstractBlockBase<?> __successor : __block.getSuccessors())
                        {
                            __scratch.or(allocator.getBlockData(__successor).liveIn);
                        }
                    }

                    if (!__blockSets.liveOut.equals(__scratch))
                    {
                        __blockSets.liveOut = trimClone(__scratch);

                        __changeOccurred = true;
                        __changeOccurredInBlock = true;
                    }
                }

                if (__iterationCount == 0 || __changeOccurredInBlock)
                {
                    /*
                     * liveIn(block) is the union of liveGen(block) with (liveOut(block) & !liveKill(block)).
                     *
                     * Note: liveIn has to be computed only in first iteration or if liveOut has changed!
                     * Note: liveIn set can only grow, never shrink. No need to clear it.
                     */
                    BitSet __liveIn = __blockSets.liveIn;
                    /*
                     * BitSet#or will call BitSet#ensureSize (since the bit set is of length
                     * 0 initially) and set sticky to false
                     */
                    __liveIn.or(__blockSets.liveOut);
                    __liveIn.andNot(__blockSets.liveKill);
                    __liveIn.or(__blockSets.liveGen);

                    __liveIn.clone(); // trimToSize()
                }
            }
            __iterationCount++;

            if (__changeOccurred && __iterationCount > 50)
            {
                // Very unlikely, should never happen: If it happens we cannot guarantee it won't happen again.
                throw new BailoutException("too many iterations in computeGlobalLiveSets");
            }
        } while (__changeOccurred);

        // check that the liveIn set of the first block is empty
        AbstractBlockBase<?> __startBlock = allocator.getLIR().getControlFlowGraph().getStartBlock();
        if (allocator.getBlockData(__startBlock).liveIn.cardinality() != 0)
        {
            // bailout if this occurs in product mode.
            throw new GraalError("liveIn set of first block must be empty: " + allocator.getBlockData(__startBlock).liveIn);
        }
    }

    /**
     * Creates a trimmed copy a bit set.
     *
     * {@link BitSet#clone()} cannot be used since it will not {@linkplain BitSet#trimToSize trim}
     * the array if the bit set is {@linkplain BitSet#sizeIsSticky sticky}.
     */
    @SuppressWarnings("javadoc")
    private static BitSet trimClone(BitSet __set)
    {
        BitSet __trimmedSet = new BitSet(0); // zero-length words array, sticky
        __trimmedSet.or(__set); // words size ensured to be words-in-use of set,
                            // also makes it non-sticky
        return __trimmedSet;
    }

    protected void addUse(AllocatableValue __operand, int __from, int __to, RegisterPriority __registerPriority, ValueKind<?> __kind)
    {
        if (!allocator.isProcessed(__operand))
        {
            return;
        }

        Interval __interval = allocator.getOrCreateInterval(__operand);
        if (!__kind.equals(LIRKind.Illegal))
        {
            __interval.setKind(__kind);
        }

        __interval.addRange(__from, __to);

        // Register use position at even instruction id.
        __interval.addUsePos(__to & ~1, __registerPriority);
    }

    protected void addTemp(AllocatableValue __operand, int __tempPos, RegisterPriority __registerPriority, ValueKind<?> __kind)
    {
        if (!allocator.isProcessed(__operand))
        {
            return;
        }

        Interval __interval = allocator.getOrCreateInterval(__operand);
        if (!__kind.equals(LIRKind.Illegal))
        {
            __interval.setKind(__kind);
        }

        __interval.addRange(__tempPos, __tempPos + 1);
        __interval.addUsePos(__tempPos, __registerPriority);
        __interval.addMaterializationValue(null);
    }

    protected void addDef(AllocatableValue __operand, LIRInstruction __op, RegisterPriority __registerPriority, ValueKind<?> __kind)
    {
        if (!allocator.isProcessed(__operand))
        {
            return;
        }
        int __defPos = __op.id();

        Interval __interval = allocator.getOrCreateInterval(__operand);
        if (!__kind.equals(LIRKind.Illegal))
        {
            __interval.setKind(__kind);
        }

        Range __r = __interval.first();
        if (__r.from <= __defPos)
        {
            /*
             * Update the starting point (when a range is first created for a use, its start is the
             * beginning of the current block until a def is encountered).
             */
            __r.from = __defPos;
            __interval.addUsePos(__defPos, __registerPriority);
        }
        else
        {
            // dead value - make vacuous interval also add register priority for dead intervals
            __interval.addRange(__defPos, __defPos + 1);
            __interval.addUsePos(__defPos, __registerPriority);
        }

        changeSpillDefinitionPos(__op, __operand, __interval, __defPos);
        if (__registerPriority == RegisterPriority.None && __interval.spillState().ordinal() <= SpillState.StartInMemory.ordinal() && ValueUtil.isStackSlot(__operand))
        {
            // detection of method-parameters and roundfp-results
            __interval.setSpillState(SpillState.StartInMemory);
        }
        __interval.addMaterializationValue(getMaterializedValue(__op, __operand, __interval));
    }

    /**
     * Optimizes moves related to incoming stack based arguments. The interval for the destination
     * of such moves is assigned the stack slot (which is in the caller's frame) as its spill slot.
     */
    protected void handleMethodArguments(LIRInstruction __op)
    {
        if (ValueMoveOp.isValueMoveOp(__op))
        {
            ValueMoveOp __move = ValueMoveOp.asValueMoveOp(__op);
            if (optimizeMethodArgument(__move.getInput()))
            {
                StackSlot __slot = ValueUtil.asStackSlot(__move.getInput());

                Interval __interval = allocator.intervalFor(__move.getResult());
                __interval.setSpillSlot(__slot);
                __interval.assignLocation(__slot);
            }
        }
    }

    protected void addRegisterHint(final LIRInstruction __op, final Value __targetValue, OperandMode __mode, EnumSet<OperandFlag> __flags, final boolean __hintAtDef)
    {
        if (__flags.contains(OperandFlag.HINT) && LinearScan.isVariableOrRegister(__targetValue))
        {
            __op.forEachRegisterHint(__targetValue, __mode, (__registerHint, __valueMode, __valueFlags) ->
            {
                if (LinearScan.isVariableOrRegister(__registerHint))
                {
                    Interval __from = allocator.getOrCreateInterval((AllocatableValue) __registerHint);
                    Interval __to = allocator.getOrCreateInterval((AllocatableValue) __targetValue);

                    // hints always point from def to use
                    if (__hintAtDef)
                    {
                        __to.setLocationHint(__from);
                    }
                    else
                    {
                        __from.setLocationHint(__to);
                    }

                    return __registerHint;
                }
                return null;
            });
        }
    }

    /**
     * Eliminates moves from register to stack if the stack slot is known to be correct.
     */
    protected void changeSpillDefinitionPos(LIRInstruction __op, AllocatableValue __operand, Interval __interval, int __defPos)
    {
        switch (__interval.spillState())
        {
            case NoDefinitionFound:
                __interval.setSpillDefinitionPos(__defPos);
                __interval.setSpillState(SpillState.NoSpillStore);
                break;

            case NoSpillStore:
                if (__defPos < __interval.spillDefinitionPos() - 2)
                {
                    // second definition found, so no spill optimization possible for this interval
                    __interval.setSpillState(SpillState.NoOptimization);
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

    private static boolean optimizeMethodArgument(Value __value)
    {
        /*
         * Object method arguments that are passed on the stack are currently not optimized because
         * this requires that the runtime visits method arguments during stack walking.
         */
        return ValueUtil.isStackSlot(__value) && ValueUtil.asStackSlot(__value).isInCallerFrame() && LIRKind.isValue(__value);
    }

    /**
     * Determines the register priority for an instruction's output/result operand.
     */
    protected RegisterPriority registerPriorityOfOutputOperand(LIRInstruction __op)
    {
        if (ValueMoveOp.isValueMoveOp(__op))
        {
            ValueMoveOp __move = ValueMoveOp.asValueMoveOp(__op);
            if (optimizeMethodArgument(__move.getInput()))
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
    protected static RegisterPriority registerPriorityOfInputOperand(EnumSet<OperandFlag> __flags)
    {
        if (__flags.contains(OperandFlag.STACK))
        {
            return RegisterPriority.ShouldHaveRegister;
        }
        // all other operands require a register
        return RegisterPriority.MustHaveRegister;
    }

    protected void buildIntervals()
    {
        InstructionValueConsumer __outputConsumer = (__op, __operand, __mode, __flags) ->
        {
            if (LinearScan.isVariableOrRegister(__operand))
            {
                addDef((AllocatableValue) __operand, __op, registerPriorityOfOutputOperand(__op), __operand.getValueKind());
                addRegisterHint(__op, __operand, __mode, __flags, true);
            }
        };

        InstructionValueConsumer __tempConsumer = (__op, __operand, __mode, __flags) ->
        {
            if (LinearScan.isVariableOrRegister(__operand))
            {
                addTemp((AllocatableValue) __operand, __op.id(), RegisterPriority.MustHaveRegister, __operand.getValueKind());
                addRegisterHint(__op, __operand, __mode, __flags, false);
            }
        };

        InstructionValueConsumer __aliveConsumer = (__op, __operand, __mode, __flags) ->
        {
            if (LinearScan.isVariableOrRegister(__operand))
            {
                RegisterPriority __p = registerPriorityOfInputOperand(__flags);
                int __opId = __op.id();
                int __blockFrom = allocator.getFirstLirInstructionId((allocator.blockForId(__opId)));
                addUse((AllocatableValue) __operand, __blockFrom, __opId + 1, __p, __operand.getValueKind());
                addRegisterHint(__op, __operand, __mode, __flags, false);
            }
        };

        InstructionValueConsumer __inputConsumer = (__op, __operand, __mode, __flags) ->
        {
            if (LinearScan.isVariableOrRegister(__operand))
            {
                int __opId = __op.id();
                int __blockFrom = allocator.getFirstLirInstructionId((allocator.blockForId(__opId)));
                RegisterPriority __p = registerPriorityOfInputOperand(__flags);
                addUse((AllocatableValue) __operand, __blockFrom, __opId, __p, __operand.getValueKind());
                addRegisterHint(__op, __operand, __mode, __flags, false);
            }
        };

        // create a list with all caller-save registers (cpu, fpu, xmm)
        RegisterArray __callerSaveRegs = allocator.getRegisterAllocationConfig().getRegisterConfig().getCallerSaveRegisters();

        // iterate all blocks in reverse order
        for (int __i = allocator.blockCount() - 1; __i >= 0; __i--)
        {
            AbstractBlockBase<?> __block = allocator.blockAt(__i);
            ArrayList<LIRInstruction> __instructions = allocator.getLIR().getLIRforBlock(__block);
            final int __blockFrom = allocator.getFirstLirInstructionId(__block);
            int __blockTo = allocator.getLastLirInstructionId(__block);

            // update intervals for operands live at the end of this block
            BitSet __live = allocator.getBlockData(__block).liveOut;
            for (int __operandNum = __live.nextSetBit(0); __operandNum >= 0; __operandNum = __live.nextSetBit(__operandNum + 1))
            {
                AllocatableValue __operand = allocator.intervalFor(__operandNum).operand;

                addUse(__operand, __blockFrom, __blockTo + 2, RegisterPriority.None, LIRKind.Illegal);

                /*
                 * Add special use positions for loop-end blocks when the interval is used
                 * anywhere inside this loop. It's possible that the block was part of a
                 * non-natural loop, so it might have an invalid loop index.
                 */
                if (__block.isLoopEnd() && __block.getLoop() != null && isIntervalInLoop(__operandNum, __block.getLoop().getIndex()))
                {
                    allocator.intervalFor(__operandNum).addUsePos(__blockTo + 1, RegisterPriority.LiveAtLoopEnd);
                }
            }

            /*
             * Iterate all instructions of the block in reverse order. Definitions of
             * intervals are processed before uses.
             */
            for (int __j = __instructions.size() - 1; __j >= 0; __j--)
            {
                final LIRInstruction __op = __instructions.get(__j);
                final int __opId = __op.id();

                // add a temp range for each register if operation destroys
                // caller-save registers
                if (__op.destroysCallerSavedRegisters())
                {
                    for (Register __r : __callerSaveRegs)
                    {
                        if (allocator.attributes(__r).isAllocatable())
                        {
                            addTemp(__r.asValue(), __opId, RegisterPriority.None, LIRKind.Illegal);
                        }
                    }
                }

                __op.visitEachOutput(__outputConsumer);
                __op.visitEachTemp(__tempConsumer);
                __op.visitEachAlive(__aliveConsumer);
                __op.visitEachInput(__inputConsumer);

                // special steps for some instructions (especially moves)
                handleMethodArguments(__op);
            }
        }

        /*
         * Add the range [0, 1] to all fixed intervals. The register allocator need not handle
         * unhandled fixed intervals.
         */
        for (Interval __interval : allocator.intervals())
        {
            if (__interval != null && ValueUtil.isRegister(__interval.operand))
            {
                __interval.addRange(0, 1);
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
    protected Constant getMaterializedValue(LIRInstruction __op, Value __operand, Interval __interval)
    {
        if (LoadConstantOp.isLoadConstantOp(__op))
        {
            LoadConstantOp __move = LoadConstantOp.asLoadConstantOp(__op);

            if (!allocator.neverSpillConstants())
            {
                /*
                 * Check if the interval has any uses which would accept an stack location (priority
                 * == ShouldHaveRegister). Rematerialization of such intervals can result in a
                 * degradation, because rematerialization always inserts a constant load, even if
                 * the value is not needed in a register.
                 */
                Interval.UsePosList __usePosList = __interval.usePosList();
                int __numUsePos = __usePosList.size();
                for (int __useIdx = 0; __useIdx < __numUsePos; __useIdx++)
                {
                    Interval.RegisterPriority __priority = __usePosList.registerPriority(__useIdx);
                    if (__priority == Interval.RegisterPriority.ShouldHaveRegister)
                    {
                        return null;
                    }
                }
            }
            return __move.getConstant();
        }
        return null;
    }
}
