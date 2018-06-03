package giraaff.lir.stackslotalloc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Deque;
import java.util.EnumSet;

import jdk.vm.ci.meta.Value;

import org.graalvm.collections.EconomicSet;
import org.graalvm.collections.Equivalence;

import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.core.common.cfg.BlockMap;
import giraaff.lir.InstructionValueConsumer;
import giraaff.lir.InstructionValueProcedure;
import giraaff.lir.LIR;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstruction.OperandMode;
import giraaff.lir.LIRValueUtil;
import giraaff.lir.VirtualStackSlot;

/**
 * Calculates the stack intervals using a worklist-based backwards data-flow analysis.
 */
// @class FixPointIntervalBuilder
final class FixPointIntervalBuilder
{
    // @field
    private final LIR lir;
    // @field
    private final StackInterval[] stackSlotMap;
    // @field
    private final int maxOpId;
    // @field
    private final BlockMap<BitSet> liveInMap;
    // @field
    private final BlockMap<BitSet> liveOutMap;
    // @field
    private final EconomicSet<LIRInstruction> usePos = EconomicSet.create(Equivalence.IDENTITY);

    // @cons
    FixPointIntervalBuilder(LIR __lir, StackInterval[] __stackSlotMap, int __maxOpId)
    {
        super();
        this.lir = __lir;
        this.stackSlotMap = __stackSlotMap;
        this.maxOpId = __maxOpId;
        this.liveInMap = new BlockMap<>(__lir.getControlFlowGraph());
        this.liveOutMap = new BlockMap<>(__lir.getControlFlowGraph());
    }

    /**
     * Builds the lifetime intervals for {@link VirtualStackSlot virtual stack slots}, sets up
     * {@link #stackSlotMap} and returns a set of use positions, i.e. instructions that contain
     * virtual stack slots.
     */
    EconomicSet<LIRInstruction> build()
    {
        Deque<AbstractBlockBase<?>> __worklist = new ArrayDeque<>();
        AbstractBlockBase<?>[] __blocks = this.lir.getControlFlowGraph().getBlocks();
        for (int __i = __blocks.length - 1; __i >= 0; __i--)
        {
            __worklist.add(__blocks[__i]);
        }
        for (AbstractBlockBase<?> __block : this.lir.getControlFlowGraph().getBlocks())
        {
            this.liveInMap.put(__block, new BitSet(this.stackSlotMap.length));
        }
        while (!__worklist.isEmpty())
        {
            AbstractBlockBase<?> __block = __worklist.poll();
            processBlock(__block, __worklist);
        }
        return this.usePos;
    }

    /**
     * Merge outSet with in-set of successors.
     */
    private boolean updateOutBlock(AbstractBlockBase<?> __block)
    {
        BitSet __union = new BitSet(this.stackSlotMap.length);
        for (AbstractBlockBase<?> __succ : __block.getSuccessors())
        {
            __union.or(this.liveInMap.get(__succ));
        }
        BitSet __outSet = this.liveOutMap.get(__block);
        // check if changed
        if (__outSet == null || !__union.equals(__outSet))
        {
            this.liveOutMap.put(__block, __union);
            return true;
        }
        return false;
    }

    private void processBlock(AbstractBlockBase<?> __block, Deque<AbstractBlockBase<?>> __worklist)
    {
        if (updateOutBlock(__block))
        {
            ArrayList<LIRInstruction> __instructions = this.lir.getLIRforBlock(__block);
            // get out set and mark intervals
            BitSet __outSet = this.liveOutMap.get(__block);
            markOutInterval(__outSet, getBlockEnd(__instructions));

            // process instructions
            BlockClosure __closure = new BlockClosure((BitSet) __outSet.clone());
            for (int __i = __instructions.size() - 1; __i >= 0; __i--)
            {
                LIRInstruction __inst = __instructions.get(__i);
                __closure.processInstructionBottomUp(__inst);
            }

            // add predecessors to work list
            for (AbstractBlockBase<?> __b : __block.getPredecessors())
            {
                __worklist.add(__b);
            }
            // set in set and mark intervals
            BitSet __inSet = __closure.getCurrentSet();
            this.liveInMap.put(__block, __inSet);
            markInInterval(__inSet, getBlockBegin(__instructions));
        }
    }

    private void markOutInterval(BitSet __outSet, int __blockEndOpId)
    {
        for (int __i = __outSet.nextSetBit(0); __i >= 0; __i = __outSet.nextSetBit(__i + 1))
        {
            StackInterval __interval = getIntervalFromStackId(__i);
            __interval.addTo(__blockEndOpId);
        }
    }

    private void markInInterval(BitSet __inSet, int __blockFirstOpId)
    {
        for (int __i = __inSet.nextSetBit(0); __i >= 0; __i = __inSet.nextSetBit(__i + 1))
        {
            StackInterval __interval = getIntervalFromStackId(__i);
            __interval.addFrom(__blockFirstOpId);
        }
    }

    // @class FixPointIntervalBuilder.BlockClosure
    // @closure
    private final class BlockClosure
    {
        // @field
        private final BitSet currentSet;

        // @cons
        private BlockClosure(BitSet __set)
        {
            super();
            currentSet = __set;
        }

        private BitSet getCurrentSet()
        {
            return currentSet;
        }

        /**
         * Process all values of an instruction bottom-up, i.e. definitions before usages.
         * Values that start or end at the current operation are not included.
         */
        private void processInstructionBottomUp(LIRInstruction __op)
        {
            __op.visitEachTemp(defConsumer);
            __op.visitEachOutput(defConsumer);

            __op.visitEachAlive(useConsumer);
            __op.visitEachInput(useConsumer);
        }

        // @closure
        InstructionValueConsumer useConsumer = new InstructionValueConsumer()
        {
            @Override
            public void visitValue(LIRInstruction __inst, Value __operand, OperandMode __mode, EnumSet<OperandFlag> __flags)
            {
                if (LIRValueUtil.isVirtualStackSlot(__operand))
                {
                    VirtualStackSlot __vslot = LIRValueUtil.asVirtualStackSlot(__operand);
                    addUse(__vslot, __inst, __flags);
                    addRegisterHint(__inst, __vslot, __mode, __flags, false);
                    FixPointIntervalBuilder.this.usePos.add(__inst);
                    currentSet.set(__vslot.getId());
                }
            }
        };

        // @closure
        InstructionValueConsumer defConsumer = new InstructionValueConsumer()
        {
            @Override
            public void visitValue(LIRInstruction __inst, Value __operand, OperandMode __mode, EnumSet<OperandFlag> __flags)
            {
                if (LIRValueUtil.isVirtualStackSlot(__operand))
                {
                    VirtualStackSlot __vslot = LIRValueUtil.asVirtualStackSlot(__operand);
                    addDef(__vslot, __inst);
                    addRegisterHint(__inst, __vslot, __mode, __flags, true);
                    FixPointIntervalBuilder.this.usePos.add(__inst);
                    currentSet.clear(__vslot.getId());
                }
            }
        };

        private void addUse(VirtualStackSlot __stackSlot, LIRInstruction __inst, EnumSet<OperandFlag> __flags)
        {
            StackInterval __interval = getOrCreateInterval(__stackSlot);
            if (__flags.contains(OperandFlag.UNINITIALIZED))
            {
                // Stack slot is marked uninitialized so we have to assume it is live all the time.
                __interval.addFrom(0);
                __interval.addTo(FixPointIntervalBuilder.this.maxOpId);
            }
            else
            {
                __interval.addTo(__inst.id());
            }
        }

        private void addDef(VirtualStackSlot __stackSlot, LIRInstruction __inst)
        {
            StackInterval __interval = getOrCreateInterval(__stackSlot);
            __interval.addFrom(__inst.id());
        }

        void addRegisterHint(final LIRInstruction __op, VirtualStackSlot __targetValue, OperandMode __mode, EnumSet<OperandFlag> __flags, final boolean __hintAtDef)
        {
            if (__flags.contains(OperandFlag.HINT))
            {
                // @closure
                InstructionValueProcedure proc = new InstructionValueProcedure()
                {
                    @Override
                    public Value doValue(LIRInstruction __instruction, Value __registerHint, OperandMode __vaueMode, EnumSet<OperandFlag> __valueFlags)
                    {
                        if (LIRValueUtil.isVirtualStackSlot(__registerHint))
                        {
                            StackInterval __from = getOrCreateInterval((VirtualStackSlot) __registerHint);
                            StackInterval __to = getOrCreateInterval(__targetValue);

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
                    }
                };
                __op.forEachRegisterHint(__targetValue, __mode, proc);
            }
        }
    }

    private StackInterval get(VirtualStackSlot __stackSlot)
    {
        return this.stackSlotMap[__stackSlot.getId()];
    }

    private void put(VirtualStackSlot __stackSlot, StackInterval __interval)
    {
        this.stackSlotMap[__stackSlot.getId()] = __interval;
    }

    private StackInterval getOrCreateInterval(VirtualStackSlot __stackSlot)
    {
        StackInterval __interval = get(__stackSlot);
        if (__interval == null)
        {
            __interval = new StackInterval(__stackSlot, __stackSlot.getValueKind());
            put(__stackSlot, __interval);
        }
        return __interval;
    }

    private StackInterval getIntervalFromStackId(int __id)
    {
        return this.stackSlotMap[__id];
    }

    private static int getBlockBegin(ArrayList<LIRInstruction> __instructions)
    {
        return __instructions.get(0).id();
    }

    private static int getBlockEnd(ArrayList<LIRInstruction> __instructions)
    {
        return __instructions.get(__instructions.size() - 1).id() + 1;
    }
}
