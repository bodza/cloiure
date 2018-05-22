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
final class FixPointIntervalBuilder
{
    private final BlockMap<BitSet> liveInMap;
    private final BlockMap<BitSet> liveOutMap;
    private final LIR lir;
    private final int maxOpId;
    private final StackInterval[] stackSlotMap;
    private final EconomicSet<LIRInstruction> usePos;

    FixPointIntervalBuilder(LIR lir, StackInterval[] stackSlotMap, int maxOpId)
    {
        this.lir = lir;
        this.stackSlotMap = stackSlotMap;
        this.maxOpId = maxOpId;
        liveInMap = new BlockMap<>(lir.getControlFlowGraph());
        liveOutMap = new BlockMap<>(lir.getControlFlowGraph());
        this.usePos = EconomicSet.create(Equivalence.IDENTITY);
    }

    /**
     * Builds the lifetime intervals for {@link VirtualStackSlot virtual stack slots}, sets up
     * {@link #stackSlotMap} and returns a set of use positions, i.e. instructions that contain
     * virtual stack slots.
     */
    EconomicSet<LIRInstruction> build()
    {
        Deque<AbstractBlockBase<?>> worklist = new ArrayDeque<>();
        AbstractBlockBase<?>[] blocks = lir.getControlFlowGraph().getBlocks();
        for (int i = blocks.length - 1; i >= 0; i--)
        {
            worklist.add(blocks[i]);
        }
        for (AbstractBlockBase<?> block : lir.getControlFlowGraph().getBlocks())
        {
            liveInMap.put(block, new BitSet(stackSlotMap.length));
        }
        while (!worklist.isEmpty())
        {
            AbstractBlockBase<?> block = worklist.poll();
            processBlock(block, worklist);
        }
        return usePos;
    }

    /**
     * Merge outSet with in-set of successors.
     */
    private boolean updateOutBlock(AbstractBlockBase<?> block)
    {
        BitSet union = new BitSet(stackSlotMap.length);
        for (AbstractBlockBase<?> succ : block.getSuccessors())
        {
            union.or(liveInMap.get(succ));
        }
        BitSet outSet = liveOutMap.get(block);
        // check if changed
        if (outSet == null || !union.equals(outSet))
        {
            liveOutMap.put(block, union);
            return true;
        }
        return false;
    }

    private void processBlock(AbstractBlockBase<?> block, Deque<AbstractBlockBase<?>> worklist)
    {
        if (updateOutBlock(block))
        {
            ArrayList<LIRInstruction> instructions = lir.getLIRforBlock(block);
            // get out set and mark intervals
            BitSet outSet = liveOutMap.get(block);
            markOutInterval(outSet, getBlockEnd(instructions));

            // process instructions
            BlockClosure closure = new BlockClosure((BitSet) outSet.clone());
            for (int i = instructions.size() - 1; i >= 0; i--)
            {
                LIRInstruction inst = instructions.get(i);
                closure.processInstructionBottomUp(inst);
            }

            // add predecessors to work list
            for (AbstractBlockBase<?> b : block.getPredecessors())
            {
                worklist.add(b);
            }
            // set in set and mark intervals
            BitSet inSet = closure.getCurrentSet();
            liveInMap.put(block, inSet);
            markInInterval(inSet, getBlockBegin(instructions));
        }
    }

    private String liveSetToString(BitSet liveSet)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = liveSet.nextSetBit(0); i >= 0; i = liveSet.nextSetBit(i + 1))
        {
            StackInterval interval = getIntervalFromStackId(i);
            sb.append(interval.getOperand()).append(" ");
        }
        return sb.toString();
    }

    private void markOutInterval(BitSet outSet, int blockEndOpId)
    {
        for (int i = outSet.nextSetBit(0); i >= 0; i = outSet.nextSetBit(i + 1))
        {
            StackInterval interval = getIntervalFromStackId(i);
            interval.addTo(blockEndOpId);
        }
    }

    private void markInInterval(BitSet inSet, int blockFirstOpId)
    {
        for (int i = inSet.nextSetBit(0); i >= 0; i = inSet.nextSetBit(i + 1))
        {
            StackInterval interval = getIntervalFromStackId(i);
            interval.addFrom(blockFirstOpId);
        }
    }

    private final class BlockClosure
    {
        private final BitSet currentSet;

        private BlockClosure(BitSet set)
        {
            currentSet = set;
        }

        private BitSet getCurrentSet()
        {
            return currentSet;
        }

        /**
         * Process all values of an instruction bottom-up, i.e. definitions before usages. Values
         * that start or end at the current operation are not included.
         */
        private void processInstructionBottomUp(LIRInstruction op)
        {
            // kills
            op.visitEachTemp(defConsumer);
            op.visitEachOutput(defConsumer);

            // gen - values that are considered alive for this state
            op.visitEachAlive(useConsumer);
            op.visitEachState(useConsumer);
            // mark locations
            // gen
            op.visitEachInput(useConsumer);
        }

        InstructionValueConsumer useConsumer = new InstructionValueConsumer()
        {
            @Override
            public void visitValue(LIRInstruction inst, Value operand, OperandMode mode, EnumSet<OperandFlag> flags)
            {
                if (LIRValueUtil.isVirtualStackSlot(operand))
                {
                    VirtualStackSlot vslot = LIRValueUtil.asVirtualStackSlot(operand);
                    addUse(vslot, inst, flags);
                    addRegisterHint(inst, vslot, mode, flags, false);
                    usePos.add(inst);
                    currentSet.set(vslot.getId());
                }
            }
        };

        InstructionValueConsumer defConsumer = new InstructionValueConsumer()
        {
            @Override
            public void visitValue(LIRInstruction inst, Value operand, OperandMode mode, EnumSet<OperandFlag> flags)
            {
                if (LIRValueUtil.isVirtualStackSlot(operand))
                {
                    VirtualStackSlot vslot = LIRValueUtil.asVirtualStackSlot(operand);
                    addDef(vslot, inst);
                    addRegisterHint(inst, vslot, mode, flags, true);
                    usePos.add(inst);
                    currentSet.clear(vslot.getId());
                }
            }
        };

        private void addUse(VirtualStackSlot stackSlot, LIRInstruction inst, EnumSet<OperandFlag> flags)
        {
            StackInterval interval = getOrCreateInterval(stackSlot);
            if (flags.contains(OperandFlag.UNINITIALIZED))
            {
                // Stack slot is marked uninitialized so we have to assume it is live all the time.
                interval.addFrom(0);
                interval.addTo(maxOpId);
            }
            else
            {
                interval.addTo(inst.id());
            }
        }

        private void addDef(VirtualStackSlot stackSlot, LIRInstruction inst)
        {
            StackInterval interval = getOrCreateInterval(stackSlot);
            interval.addFrom(inst.id());
        }

        void addRegisterHint(final LIRInstruction op, VirtualStackSlot targetValue, OperandMode mode, EnumSet<OperandFlag> flags, final boolean hintAtDef)
        {
            if (flags.contains(OperandFlag.HINT))
            {
                InstructionValueProcedure proc = new InstructionValueProcedure()
                {
                    @Override
                    public Value doValue(LIRInstruction instruction, Value registerHint, OperandMode vaueMode, EnumSet<OperandFlag> valueFlags)
                    {
                        if (LIRValueUtil.isVirtualStackSlot(registerHint))
                        {
                            StackInterval from = getOrCreateInterval((VirtualStackSlot) registerHint);
                            StackInterval to = getOrCreateInterval(targetValue);

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
                    }
                };
                op.forEachRegisterHint(targetValue, mode, proc);
            }
        }
    }

    private StackInterval get(VirtualStackSlot stackSlot)
    {
        return stackSlotMap[stackSlot.getId()];
    }

    private void put(VirtualStackSlot stackSlot, StackInterval interval)
    {
        stackSlotMap[stackSlot.getId()] = interval;
    }

    private StackInterval getOrCreateInterval(VirtualStackSlot stackSlot)
    {
        StackInterval interval = get(stackSlot);
        if (interval == null)
        {
            interval = new StackInterval(stackSlot, stackSlot.getValueKind());
            put(stackSlot, interval);
        }
        return interval;
    }

    private StackInterval getIntervalFromStackId(int id)
    {
        return stackSlotMap[id];
    }

    private static int getBlockBegin(ArrayList<LIRInstruction> instructions)
    {
        return instructions.get(0).id();
    }

    private static int getBlockEnd(ArrayList<LIRInstruction> instructions)
    {
        return instructions.get(instructions.size() - 1).id() + 1;
    }
}
