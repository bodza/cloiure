package giraaff.lir;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterArray;
import jdk.vm.ci.code.RegisterConfig;
import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.Value;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;

import giraaff.core.common.LIRKind;
import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.lir.LIRInstruction;
import giraaff.lir.StandardOp;
import giraaff.lir.framemap.FrameMap;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.phases.PostAllocationOptimizationPhase;

///
// Removes move instructions, where the destination value is already in place.
///
// @class RedundantMoveElimination
public final class RedundantMoveElimination extends PostAllocationOptimizationPhase
{
    @Override
    protected void run(TargetDescription __target, LIRGenerationResult __lirGenRes, PostAllocationOptimizationPhase.PostAllocationOptimizationContext __context)
    {
        RedundantMoveElimination.RMEOptimization __redundantMoveElimination = new RedundantMoveElimination.RMEOptimization(__lirGenRes.getFrameMap());
        __redundantMoveElimination.doOptimize(__lirGenRes.getLIR());
    }

    ///
    // Holds the entry and exit states for each block for dataflow analysis. The state is an array
    // with an element for each relevant location (register or stack slot). Each element holds the
    // global number of the location's definition. A location definition is simply an output of an
    // instruction. Note that because instructions can have multiple outputs it is not possible to
    // use the instruction id for value numbering. In addition, the result of merging at block
    // entries (= phi values) get unique value numbers.
    //
    // The value numbers also contain information if it is an object kind value or not: if the
    // number is negative it is an object kind value.
    ///
    // @class RedundantMoveElimination.BlockStates
    private static final class BlockStates
    {
        // The state at block entry for global dataflow analysis. It contains a global value number
        // for each location to optimize.

        // @field
        int[] ___entryState;

        // The state at block exit for global dataflow analysis. It contains a global value number
        // for each location to optimize.

        // @field
        int[] ___exitState;

        // The starting number for global value numbering in this block.

        // @field
        int ___entryValueNum;

        // @cons RedundantMoveElimination.BlockStates
        BlockStates(int __stateSize)
        {
            super();
            this.___entryState = new int[__stateSize];
            this.___exitState = new int[__stateSize];
        }
    }

    // @class RedundantMoveElimination.RMEOptimization
    private static final class RMEOptimization
    {
        // @field
        EconomicMap<AbstractBlockBase<?>, RedundantMoveElimination.BlockStates> ___blockData = EconomicMap.create(Equivalence.IDENTITY);

        // @field
        RegisterArray ___callerSaveRegs;

        ///
        // Contains the register number for registers which can be optimized and -1 for the others.
        ///
        // @field
        int[] ___eligibleRegs;

        ///
        // A map from the {@link StackSlot} {@link #getOffset offset} to an index into the state.
        // StackSlots of different kinds that map to the same location will map to the same index.
        ///
        // @field
        EconomicMap<Integer, Integer> ___stackIndices = EconomicMap.create(Equivalence.DEFAULT);

        // @field
        int ___numRegs;

        // @field
        private final FrameMap ___frameMap;

        // Pseudo value for a not yet assigned location.
        // @def
        static final int INIT_VALUE = 0;

        // @cons RedundantMoveElimination.RMEOptimization
        RMEOptimization(FrameMap __frameMap)
        {
            super();
            this.___frameMap = __frameMap;
        }

        ///
        // The main method doing the elimination of redundant moves.
        ///
        private void doOptimize(LIR __lir)
        {
            RegisterConfig __registerConfig = this.___frameMap.getRegisterConfig();
            this.___callerSaveRegs = __registerConfig.getCallerSaveRegisters();

            initBlockData(__lir);

            // Compute a table of the registers which are eligible for move optimization.
            // Unallocatable registers should never be optimized.
            this.___eligibleRegs = new int[this.___numRegs];
            Arrays.fill(this.___eligibleRegs, -1);
            for (Register __reg : __registerConfig.getAllocatableRegisters())
            {
                if (__reg.number < this.___numRegs)
                {
                    this.___eligibleRegs[__reg.number] = __reg.number;
                }
            }

            if (!solveDataFlow(__lir))
            {
                return;
            }

            eliminateMoves(__lir);
        }

        ///
        // The maximum number of locations * blocks. This is a complexity limit for the inner loop
        // in {@link #mergeState} (assuming a small number of iterations in {@link #solveDataFlow}.
        ///
        // @def
        private static final int COMPLEXITY_LIMIT = 30000;

        private void initBlockData(LIR __lir)
        {
            AbstractBlockBase<?>[] __blocks = __lir.linearScanOrder();
            this.___numRegs = 0;

            int __maxStackLocations = COMPLEXITY_LIMIT / __blocks.length;

            // Search for relevant locations which can be optimized. These are register or stack
            // slots which occur as destinations of move instructions.
            for (AbstractBlockBase<?> __block : __blocks)
            {
                ArrayList<LIRInstruction> __instructions = __lir.getLIRforBlock(__block);
                for (LIRInstruction __op : __instructions)
                {
                    if (isEligibleMove(__op))
                    {
                        Value __dest = StandardOp.MoveOp.asMoveOp(__op).getResult();
                        if (ValueUtil.isRegister(__dest))
                        {
                            int __regNum = ((RegisterValue) __dest).getRegister().number;
                            if (__regNum >= this.___numRegs)
                            {
                                this.___numRegs = __regNum + 1;
                            }
                        }
                        else if (ValueUtil.isStackSlot(__dest))
                        {
                            StackSlot __stackSlot = (StackSlot) __dest;
                            Integer __offset = getOffset(__stackSlot);
                            if (!this.___stackIndices.containsKey(__offset) && this.___stackIndices.size() < __maxStackLocations)
                            {
                                this.___stackIndices.put(__offset, this.___stackIndices.size());
                            }
                        }
                    }
                }
            }

            // Now we know the number of locations to optimize, so we can allocate the block states.
            int __numLocations = this.___numRegs + this.___stackIndices.size();
            for (AbstractBlockBase<?> __block : __blocks)
            {
                RedundantMoveElimination.BlockStates __data = new RedundantMoveElimination.BlockStates(__numLocations);
                this.___blockData.put(__block, __data);
            }
        }

        private int getOffset(StackSlot __stackSlot)
        {
            return __stackSlot.getOffset(this.___frameMap.totalFrameSize());
        }

        ///
        // Calculates the entry and exit states for all basic blocks.
        //
        // @return Returns true on success and false if the control flow is too complex.
        ///
        private boolean solveDataFlow(LIR __lir)
        {
            AbstractBlockBase<?>[] __blocks = __lir.linearScanOrder();

            int __numIter = 0;

            // Iterate until there are no more changes.
            int __currentValueNum = 1;
            boolean __firstRound = true;
            boolean __changed;
            do
            {
                __changed = false;
                for (AbstractBlockBase<?> __block : __blocks)
                {
                    RedundantMoveElimination.BlockStates __data = this.___blockData.get(__block);
                    // Initialize the number for global value numbering for this block.
                    // It is essential that the starting number for a block is consistent
                    // at all iterations and also in eliminateMoves().
                    if (__firstRound)
                    {
                        __data.___entryValueNum = __currentValueNum;
                    }
                    int __valueNum = __data.___entryValueNum;
                    boolean __newState = false;

                    if (__block == __blocks[0] || __block.isExceptionEntry())
                    {
                        // The entry block has undefined values. And also exception handler blocks:
                        // the LinearScan can insert moves at the end of an exception handler predecessor
                        // block (after the invoke, which throws the exception),
                        // and in reality such moves are not in the control flow in case of an exception.
                        // So we assume a save default for exception handler blocks.
                        clearValues(__data.___entryState, __valueNum);
                    }
                    else
                    {
                        // Merge the states of predecessor blocks.
                        for (AbstractBlockBase<?> __predecessor : __block.getPredecessors())
                        {
                            RedundantMoveElimination.BlockStates __predData = this.___blockData.get(__predecessor);
                            __newState |= mergeState(__data.___entryState, __predData.___exitState, __valueNum);
                        }
                    }
                    // Advance by the value numbers which are "consumed" by clearValues and mergeState.
                    __valueNum += __data.___entryState.length;

                    if (__newState || __firstRound)
                    {
                        // Derive the exit state from the entry state by iterating
                        // through all instructions of the block.
                        int[] __iterState = __data.___exitState;
                        copyState(__iterState, __data.___entryState);
                        ArrayList<LIRInstruction> __instructions = __lir.getLIRforBlock(__block);

                        for (LIRInstruction __op : __instructions)
                        {
                            __valueNum = updateState(__iterState, __op, __valueNum);
                        }
                        __changed = true;
                    }
                    if (__firstRound)
                    {
                        __currentValueNum = __valueNum;
                    }
                }
                __firstRound = false;
                __numIter++;

                if (__numIter > 5)
                {
                    // This is _very_ seldom.
                    return false;
                }
            } while (__changed);

            return true;
        }

        ///
        // Deletes all move instructions where the target location already contains the source value.
        ///
        private void eliminateMoves(LIR __lir)
        {
            AbstractBlockBase<?>[] __blocks = __lir.linearScanOrder();

            for (AbstractBlockBase<?> __block : __blocks)
            {
                ArrayList<LIRInstruction> __instructions = __lir.getLIRforBlock(__block);
                RedundantMoveElimination.BlockStates __data = this.___blockData.get(__block);
                boolean __hasDead = false;

                // reuse the entry state for iteration, we don't need it later
                int[] __iterState = __data.___entryState;

                // add the values which are "consumed" by clearValues and mergeState in solveDataFlow
                int __valueNum = __data.___entryValueNum + __data.___entryState.length;

                int __numInsts = __instructions.size();
                for (int __idx = 0; __idx < __numInsts; __idx++)
                {
                    LIRInstruction __op = __instructions.get(__idx);
                    if (isEligibleMove(__op))
                    {
                        StandardOp.ValueMoveOp __moveOp = StandardOp.ValueMoveOp.asValueMoveOp(__op);
                        int __sourceIdx = getStateIdx(__moveOp.getInput());
                        int __destIdx = getStateIdx(__moveOp.getResult());
                        if (__sourceIdx >= 0 && __destIdx >= 0 && __iterState[__sourceIdx] == __iterState[__destIdx])
                        {
                            __instructions.set(__idx, null);
                            __hasDead = true;
                        }
                    }
                    // it doesn't harm if updateState is also called for a deleted move
                    __valueNum = updateState(__iterState, __op, __valueNum);
                }
                if (__hasDead)
                {
                    __instructions.removeAll(Collections.singleton(null));
                }
            }
        }

        ///
        // Updates the state for one instruction.
        ///
        private int updateState(final int[] __state, LIRInstruction __op, int __initValueNum)
        {
            if (isEligibleMove(__op))
            {
                // handle the special case of a move instruction
                StandardOp.ValueMoveOp __moveOp = StandardOp.ValueMoveOp.asValueMoveOp(__op);
                int __sourceIdx = getStateIdx(__moveOp.getInput());
                int __destIdx = getStateIdx(__moveOp.getResult());
                if (__sourceIdx >= 0 && __destIdx >= 0)
                {
                    __state[__destIdx] = __state[__sourceIdx];
                    return __initValueNum;
                }
            }

            int __valueNum = __initValueNum;

            if (__op.destroysCallerSavedRegisters())
            {
                for (Register __reg : this.___callerSaveRegs)
                {
                    if (__reg.number < this.___numRegs)
                    {
                        // Kind.Object is the save default
                        __state[__reg.number] = encodeValueNum(__valueNum++, true);
                    }
                }
            }

            // Value procedure for the instruction's output and temp values.
            // @class RedundantMoveElimination.RMEOptimization.*OutputValueConsumer
            // @closure
            final class OutputValueConsumer implements ValueConsumer
            {
                // @field
                int ___opValueNum;

                // @cons RedundantMoveElimination.RMEOptimization.*OutputValueConsumer
                OutputValueConsumer(int __opValueNum)
                {
                    super();
                    this.___opValueNum = __opValueNum;
                }

                @Override
                public void visitValue(Value __operand, LIRInstruction.OperandMode __mode, EnumSet<LIRInstruction.OperandFlag> __flags)
                {
                    int __stateIdx = RedundantMoveElimination.RMEOptimization.this.getStateIdx(__operand);
                    if (__stateIdx >= 0)
                    {
                        // Assign a unique number to the output or temp location.
                        __state[__stateIdx] = encodeValueNum(this.___opValueNum++, !LIRKind.isValue(__operand));
                    }
                }
            }

            OutputValueConsumer __outputValueConsumer = new OutputValueConsumer(__valueNum);

            __op.visitEachTemp(__outputValueConsumer);
            // Semantically the output values are written _after_ the temp values.
            __op.visitEachOutput(__outputValueConsumer);

            return __outputValueConsumer.___opValueNum;
        }

        ///
        // The state merge function for dataflow joins.
        ///
        private static boolean mergeState(int[] __dest, int[] __source, int __defNum)
        {
            boolean __changed = false;
            for (int __idx = 0; __idx < __source.length; __idx++)
            {
                int __phiNum = __defNum + __idx;
                int __dst = __dest[__idx];
                int __src = __source[__idx];
                if (__dst != __src && __src != INIT_VALUE && __dst != encodeValueNum(__phiNum, isObjectValue(__dst)))
                {
                    if (__dst != INIT_VALUE)
                    {
                        __dst = encodeValueNum(__phiNum, isObjectValue(__dst) || isObjectValue(__src));
                    }
                    else
                    {
                        __dst = __src;
                    }
                    __dest[__idx] = __dst;
                    __changed = true;
                }
            }
            return __changed;
        }

        private static void copyState(int[] __dest, int[] __source)
        {
            for (int __idx = 0; __idx < __source.length; __idx++)
            {
                __dest[__idx] = __source[__idx];
            }
        }

        private static void clearValues(int[] __state, int __defNum)
        {
            for (int __idx = 0; __idx < __state.length; __idx++)
            {
                int __phiNum = __defNum + __idx;
                // Let the killed values assume to be object references: it's the save default.
                __state[__idx] = encodeValueNum(__phiNum, true);
            }
        }

        private static void clearValuesOfKindObject(int[] __state, int __defNum)
        {
            for (int __idx = 0; __idx < __state.length; __idx++)
            {
                int __phiNum = __defNum + __idx;
                if (isObjectValue(__state[__idx]))
                {
                    __state[__idx] = encodeValueNum(__phiNum, true);
                }
            }
        }

        ///
        // Returns the index to the state arrays in RedundantMoveElimination.BlockStates for a specific location.
        ///
        private int getStateIdx(Value __location)
        {
            if (ValueUtil.isRegister(__location))
            {
                int __regNum = ((RegisterValue) __location).getRegister().number;
                if (__regNum < this.___numRegs)
                {
                    return this.___eligibleRegs[__regNum];
                }
                return -1;
            }
            if (ValueUtil.isStackSlot(__location))
            {
                StackSlot __slot = (StackSlot) __location;
                Integer __index = this.___stackIndices.get(getOffset(__slot));
                if (__index != null)
                {
                    return __index.intValue() + this.___numRegs;
                }
            }
            return -1;
        }

        ///
        // Encodes a value number + the is-object information to a number to be stored in a state.
        ///
        private static int encodeValueNum(int __valueNum, boolean __isObjectKind)
        {
            if (__isObjectKind)
            {
                return -__valueNum;
            }
            return __valueNum;
        }

        ///
        // Returns true if an encoded value number (which is stored in a state) refers to an object reference.
        ///
        private static boolean isObjectValue(int __encodedValueNum)
        {
            return __encodedValueNum < 0;
        }

        ///
        // Returns true for a move instruction which is a candidate for elimination.
        ///
        private static boolean isEligibleMove(LIRInstruction __op)
        {
            if (StandardOp.ValueMoveOp.isValueMoveOp(__op))
            {
                StandardOp.ValueMoveOp __moveOp = StandardOp.ValueMoveOp.asValueMoveOp(__op);
                Value __source = __moveOp.getInput();
                Value __dest = __moveOp.getResult();
                // Moves with mismatching kinds are not moves, but memory loads/stores!
                return __source.getValueKind().equals(__dest.getValueKind());
            }
            return false;
        }
    }
}
