package giraaff.lir.stackslotalloc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.PriorityQueue;

import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.Value;
import jdk.vm.ci.meta.ValueKind;

import org.graalvm.collections.EconomicSet;

import giraaff.core.common.GraalOptions;
import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.lir.LIR;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstruction.OperandMode;
import giraaff.lir.LIRValueUtil;
import giraaff.lir.ValueProcedure;
import giraaff.lir.VirtualStackSlot;
import giraaff.lir.framemap.FrameMapBuilderTool;
import giraaff.lir.framemap.SimpleVirtualStackSlot;
import giraaff.lir.framemap.VirtualStackSlotRange;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.phases.AllocationPhase;
import giraaff.lir.phases.LIRPhase;

/**
 * Linear Scan stack slot allocator.
 *
 * Remark: The analysis works under the assumption that a stack slot is no longer live after its last usage.
 * If an {@link LIRInstruction instruction} transfers the raw address of the stack slot to another location, e.g.
 * a registers, and this location is referenced later on, the {@link giraaff.lir.LIRInstruction.Use usage} of the
 * stack slot must be marked with the {@link OperandFlag#UNINITIALIZED}. Otherwise the stack slot might be reused
 * and its content destroyed.
 */
// @class LSStackSlotAllocator
public final class LSStackSlotAllocator extends AllocationPhase
{
    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, AllocationContext context)
    {
        allocateStackSlots((FrameMapBuilderTool) lirGenRes.getFrameMapBuilder(), lirGenRes);
        lirGenRes.buildFrameMap();
    }

    public static void allocateStackSlots(FrameMapBuilderTool builder, LIRGenerationResult res)
    {
        if (builder.getNumberOfStackSlots() > 0)
        {
            new Allocator(res.getLIR(), builder).allocate();
        }
    }

    // @class LSStackSlotAllocator.Allocator
    private static final class Allocator
    {
        private final LIR lir;
        private final FrameMapBuilderTool frameMapBuilder;
        private final StackInterval[] stackSlotMap;
        private final PriorityQueue<StackInterval> unhandled;
        private final PriorityQueue<StackInterval> active;
        private final AbstractBlockBase<?>[] sortedBlocks;
        private final int maxOpId;

        // @cons
        private Allocator(LIR lir, FrameMapBuilderTool frameMapBuilder)
        {
            super();
            this.lir = lir;
            this.frameMapBuilder = frameMapBuilder;
            this.stackSlotMap = new StackInterval[frameMapBuilder.getNumberOfStackSlots()];
            this.sortedBlocks = lir.getControlFlowGraph().getBlocks();

            // insert by from
            this.unhandled = new PriorityQueue<>((a, b) -> a.from() - b.from());
            // insert by to
            this.active = new PriorityQueue<>((a, b) -> a.to() - b.to());

            // step 1: number instructions
            this.maxOpId = numberInstructions(lir, sortedBlocks);
        }

        private void allocate()
        {
            // step 2: build intervals
            EconomicSet<LIRInstruction> usePos = buildIntervals();
            // step 3: verify intervals
            // step 4: allocate stack slots
            allocateStackSlots();
            // step 5: assign stack slots
            assignStackSlots(usePos);
        }

        // ====================
        // step 1: number instructions
        // ====================

        /**
         * Numbers all instructions in all blocks.
         *
         * @return The id of the last operation.
         */
        private static int numberInstructions(LIR lir, AbstractBlockBase<?>[] sortedBlocks)
        {
            int opId = 0;
            int index = 0;
            for (AbstractBlockBase<?> block : sortedBlocks)
            {
                ArrayList<LIRInstruction> instructions = lir.getLIRforBlock(block);

                int numInst = instructions.size();
                for (int j = 0; j < numInst; j++)
                {
                    LIRInstruction op = instructions.get(j);
                    op.setId(opId);

                    index++;
                    opId += 2; // numbering of lirOps by two
                }
            }
            return opId - 2;
        }

        // ====================
        // step 2: build intervals
        // ====================

        private EconomicSet<LIRInstruction> buildIntervals()
        {
            return new FixPointIntervalBuilder(lir, stackSlotMap, maxOpId()).build();
        }

        // ====================
        // step 3: verify intervals
        // ====================

        // ====================
        // step 4: allocate stack slots
        // ====================

        private void allocateStackSlots()
        {
            // create unhandled lists
            for (StackInterval interval : stackSlotMap)
            {
                if (interval != null)
                {
                    unhandled.add(interval);
                }
            }

            for (StackInterval current = activateNext(); current != null; current = activateNext())
            {
                allocateSlot(current);
            }
        }

        private void allocateSlot(StackInterval current)
        {
            VirtualStackSlot virtualSlot = current.getOperand();
            final StackSlot location;
            if (virtualSlot instanceof VirtualStackSlotRange)
            {
                // No reuse of ranges (yet).
                VirtualStackSlotRange slotRange = (VirtualStackSlotRange) virtualSlot;
                location = frameMapBuilder.getFrameMap().allocateStackSlots(slotRange.getSlots(), slotRange.getObjects());
            }
            else
            {
                StackSlot slot = findFreeSlot((SimpleVirtualStackSlot) virtualSlot);
                if (slot != null)
                {
                    // Free stack slot available. Note that we create a new one because the kind might not match.
                    location = StackSlot.get(current.kind(), slot.getRawOffset(), slot.getRawAddFrameSize());
                }
                else
                {
                    // Allocate new stack slot.
                    location = frameMapBuilder.getFrameMap().allocateSpillSlot(virtualSlot.getValueKind());
                }
            }
            current.setLocation(location);
        }

        // @enum LSStackSlotAllocator.Allocator.SlotSize
        private enum SlotSize
        {
            Size1,
            Size2,
            Size4,
            Size8,
            Illegal;
        }

        private SlotSize forKind(ValueKind<?> kind)
        {
            switch (frameMapBuilder.getFrameMap().spillSlotSize(kind))
            {
                case 1:
                    return SlotSize.Size1;
                case 2:
                    return SlotSize.Size2;
                case 4:
                    return SlotSize.Size4;
                case 8:
                    return SlotSize.Size8;
                default:
                    return SlotSize.Illegal;
            }
        }

        private EnumMap<SlotSize, Deque<StackSlot>> freeSlots;

        /**
         * @return The list of free stack slots for {@code size} or {@code null} if there is none.
         */
        private Deque<StackSlot> getOrNullFreeSlots(SlotSize size)
        {
            if (freeSlots == null)
            {
                return null;
            }
            return freeSlots.get(size);
        }

        /**
         * @return the list of free stack slots for {@code size}. If there is none a list is created.
         */
        private Deque<StackSlot> getOrInitFreeSlots(SlotSize size)
        {
            Deque<StackSlot> freeList;
            if (freeSlots != null)
            {
                freeList = freeSlots.get(size);
            }
            else
            {
                freeSlots = new EnumMap<>(SlotSize.class);
                freeList = null;
            }
            if (freeList == null)
            {
                freeList = new ArrayDeque<>();
                freeSlots.put(size, freeList);
            }
            return freeList;
        }

        /**
         * Gets a free stack slot for {@code slot} or {@code null} if there is none.
         */
        private StackSlot findFreeSlot(SimpleVirtualStackSlot slot)
        {
            SlotSize size = forKind(slot.getValueKind());
            if (size == SlotSize.Illegal)
            {
                return null;
            }
            Deque<StackSlot> freeList = getOrNullFreeSlots(size);
            if (freeList == null)
            {
                return null;
            }
            return freeList.pollLast();
        }

        /**
         * Adds a stack slot to the list of free slots.
         */
        private void freeSlot(StackSlot slot)
        {
            SlotSize size = forKind(slot.getValueKind());
            if (size == SlotSize.Illegal)
            {
                return;
            }
            getOrInitFreeSlots(size).addLast(slot);
        }

        /**
         * Gets the next unhandled interval and finishes handled intervals.
         */
        private StackInterval activateNext()
        {
            if (unhandled.isEmpty())
            {
                return null;
            }
            StackInterval next = unhandled.poll();
            // finish handled intervals
            for (int id = next.from(); activePeekId() < id; )
            {
                finished(active.poll());
            }
            active.add(next);
            return next;
        }

        /**
         * Gets the lowest {@link StackInterval#to() end position} of all active intervals. If there
         * is none {@link Integer#MAX_VALUE} is returned.
         */
        private int activePeekId()
        {
            StackInterval first = active.peek();
            if (first == null)
            {
                return Integer.MAX_VALUE;
            }
            return first.to();
        }

        /**
         * Finishes {@code interval} by adding its location to the list of free stack slots.
         */
        private void finished(StackInterval interval)
        {
            StackSlot location = interval.location();
            freeSlot(location);
        }

        // ====================
        // step 5: assign stack slots
        // ====================

        private void assignStackSlots(EconomicSet<LIRInstruction> usePos)
        {
            for (LIRInstruction op : usePos)
            {
                op.forEachInput(assignSlot);
                op.forEachAlive(assignSlot);

                op.forEachTemp(assignSlot);
                op.forEachOutput(assignSlot);
            }
        }

        // @closure
        ValueProcedure assignSlot = new ValueProcedure()
        {
            @Override
            public Value doValue(Value value, OperandMode mode, EnumSet<OperandFlag> flags)
            {
                if (LIRValueUtil.isVirtualStackSlot(value))
                {
                    VirtualStackSlot slot = LIRValueUtil.asVirtualStackSlot(value);
                    StackInterval interval = get(slot);
                    return interval.location();
                }
                return value;
            }
        };

        /**
         * Gets the highest instruction id.
         */
        private int maxOpId()
        {
            return maxOpId;
        }

        private StackInterval get(VirtualStackSlot stackSlot)
        {
            return stackSlotMap[stackSlot.getId()];
        }
    }
}
