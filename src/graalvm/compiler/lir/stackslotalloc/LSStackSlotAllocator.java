package graalvm.compiler.lir.stackslotalloc;

import static graalvm.compiler.debug.DebugContext.BASIC_LEVEL;
import static graalvm.compiler.lir.LIRValueUtil.asVirtualStackSlot;
import static graalvm.compiler.lir.LIRValueUtil.isVirtualStackSlot;
import static graalvm.compiler.lir.phases.LIRPhase.Options.LIROptimization;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.PriorityQueue;

import org.graalvm.collections.EconomicSet;
import graalvm.compiler.core.common.cfg.AbstractBlockBase;
import graalvm.compiler.debug.DebugCloseable;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.debug.Indent;
import graalvm.compiler.debug.TimerKey;
import graalvm.compiler.lir.LIR;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.LIRInstruction.OperandFlag;
import graalvm.compiler.lir.LIRInstruction.OperandMode;
import graalvm.compiler.lir.ValueProcedure;
import graalvm.compiler.lir.VirtualStackSlot;
import graalvm.compiler.lir.framemap.FrameMapBuilderTool;
import graalvm.compiler.lir.framemap.SimpleVirtualStackSlot;
import graalvm.compiler.lir.framemap.VirtualStackSlotRange;
import graalvm.compiler.lir.gen.LIRGenerationResult;
import graalvm.compiler.lir.phases.AllocationPhase;
import graalvm.compiler.options.NestedBooleanOptionKey;
import graalvm.compiler.options.Option;
import graalvm.compiler.options.OptionType;

import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.Value;
import jdk.vm.ci.meta.ValueKind;

/**
 * Linear Scan {@link StackSlotAllocatorUtil stack slot allocator}.
 * <p>
 * <b>Remark:</b> The analysis works under the assumption that a stack slot is no longer live after
 * its last usage. If an {@link LIRInstruction instruction} transfers the raw address of the stack
 * slot to another location, e.g. a registers, and this location is referenced later on, the
 * {@link graalvm.compiler.lir.LIRInstruction.Use usage} of the stack slot must be marked with
 * the {@link OperandFlag#UNINITIALIZED}. Otherwise the stack slot might be reused and its content
 * destroyed.
 */
public final class LSStackSlotAllocator extends AllocationPhase
{
    public static class Options
    {
        @Option(help = "Use linear scan stack slot allocation.", type = OptionType.Debug)
        public static final NestedBooleanOptionKey LIROptLSStackSlotAllocator = new NestedBooleanOptionKey(LIROptimization, true);
    }

    private static final TimerKey MainTimer = DebugContext.timer("LSStackSlotAllocator");
    private static final TimerKey NumInstTimer = DebugContext.timer("LSStackSlotAllocator[NumberInstruction]");
    private static final TimerKey BuildIntervalsTimer = DebugContext.timer("LSStackSlotAllocator[BuildIntervals]");
    private static final TimerKey VerifyIntervalsTimer = DebugContext.timer("LSStackSlotAllocator[VerifyIntervals]");
    private static final TimerKey AllocateSlotsTimer = DebugContext.timer("LSStackSlotAllocator[AllocateSlots]");
    private static final TimerKey AssignSlotsTimer = DebugContext.timer("LSStackSlotAllocator[AssignSlots]");

    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, AllocationContext context)
    {
        allocateStackSlots((FrameMapBuilderTool) lirGenRes.getFrameMapBuilder(), lirGenRes);
        lirGenRes.buildFrameMap();
    }

    @SuppressWarnings("try")
    public static void allocateStackSlots(FrameMapBuilderTool builder, LIRGenerationResult res)
    {
        if (builder.getNumberOfStackSlots() > 0)
        {
            try (DebugCloseable t = MainTimer.start(res.getLIR().getDebug()))
            {
                new Allocator(res.getLIR(), builder).allocate();
            }
        }
    }

    private static final class Allocator
    {
        private final LIR lir;
        private final DebugContext debug;
        private final FrameMapBuilderTool frameMapBuilder;
        private final StackInterval[] stackSlotMap;
        private final PriorityQueue<StackInterval> unhandled;
        private final PriorityQueue<StackInterval> active;
        private final AbstractBlockBase<?>[] sortedBlocks;
        private final int maxOpId;

        @SuppressWarnings("try")
        private Allocator(LIR lir, FrameMapBuilderTool frameMapBuilder)
        {
            this.lir = lir;
            this.debug = lir.getDebug();
            this.frameMapBuilder = frameMapBuilder;
            this.stackSlotMap = new StackInterval[frameMapBuilder.getNumberOfStackSlots()];
            this.sortedBlocks = lir.getControlFlowGraph().getBlocks();

            // insert by from
            this.unhandled = new PriorityQueue<>((a, b) -> a.from() - b.from());
            // insert by to
            this.active = new PriorityQueue<>((a, b) -> a.to() - b.to());

            try (DebugCloseable t = NumInstTimer.start(debug))
            {
                // step 1: number instructions
                this.maxOpId = numberInstructions(lir, sortedBlocks);
            }
        }

        @SuppressWarnings("try")
        private void allocate()
        {
            debug.dump(DebugContext.VERBOSE_LEVEL, lir, "After StackSlot numbering");

            boolean allocationFramesizeEnabled = StackSlotAllocatorUtil.allocatedFramesize.isEnabled(debug);
            long currentFrameSize = allocationFramesizeEnabled ? frameMapBuilder.getFrameMap().currentFrameSize() : 0;
            EconomicSet<LIRInstruction> usePos;
            // step 2: build intervals
            try (DebugContext.Scope s = debug.scope("StackSlotAllocationBuildIntervals"); Indent indent = debug.logAndIndent("BuildIntervals"); DebugCloseable t = BuildIntervalsTimer.start(debug))
            {
                usePos = buildIntervals();
            }
            // step 3: verify intervals
            if (debug.areScopesEnabled())
            {
                try (DebugCloseable t = VerifyIntervalsTimer.start(debug))
                {
                    assert verifyIntervals();
                }
            }
            if (debug.isDumpEnabled(DebugContext.VERBOSE_LEVEL))
            {
                dumpIntervals("Before stack slot allocation");
            }
            // step 4: allocate stack slots
            try (DebugCloseable t = AllocateSlotsTimer.start(debug))
            {
                allocateStackSlots();
            }
            if (debug.isDumpEnabled(DebugContext.VERBOSE_LEVEL))
            {
                dumpIntervals("After stack slot allocation");
            }

            // step 5: assign stack slots
            try (DebugCloseable t = AssignSlotsTimer.start(debug))
            {
                assignStackSlots(usePos);
            }
            if (allocationFramesizeEnabled)
            {
                StackSlotAllocatorUtil.allocatedFramesize.add(debug, frameMapBuilder.getFrameMap().currentFrameSize() - currentFrameSize);
            }
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
            assert (index << 1) == opId : "must match: " + (index << 1);
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

        private boolean verifyIntervals()
        {
            for (StackInterval interval : stackSlotMap)
            {
                if (interval != null)
                {
                    assert interval.verify(maxOpId());
                }
            }
            return true;
        }

        // ====================
        // step 4: allocate stack slots
        // ====================

        @SuppressWarnings("try")
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
                try (Indent indent = debug.logAndIndent("allocate %s", current))
                {
                    allocateSlot(current);
                }
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
                StackSlotAllocatorUtil.virtualFramesize.add(debug, frameMapBuilder.getFrameMap().spillSlotRangeSize(slotRange.getSlots()));
                StackSlotAllocatorUtil.allocatedSlots.increment(debug);
            }
            else
            {
                assert virtualSlot instanceof SimpleVirtualStackSlot : "Unexpected VirtualStackSlot type: " + virtualSlot;
                StackSlot slot = findFreeSlot((SimpleVirtualStackSlot) virtualSlot);
                if (slot != null)
                {
                    /*
                     * Free stack slot available. Note that we create a new one because the kind
                     * might not match.
                     */
                    location = StackSlot.get(current.kind(), slot.getRawOffset(), slot.getRawAddFrameSize());
                    StackSlotAllocatorUtil.reusedSlots.increment(debug);
                    debug.log(BASIC_LEVEL, "Reuse stack slot %s (reallocated from %s) for virtual stack slot %s", location, slot, virtualSlot);
                }
                else
                {
                    // Allocate new stack slot.
                    location = frameMapBuilder.getFrameMap().allocateSpillSlot(virtualSlot.getValueKind());
                    StackSlotAllocatorUtil.virtualFramesize.add(debug, frameMapBuilder.getFrameMap().spillSlotSize(virtualSlot.getValueKind()));
                    StackSlotAllocatorUtil.allocatedSlots.increment(debug);
                    debug.log(BASIC_LEVEL, "New stack slot %s for virtual stack slot %s", location, virtualSlot);
                }
            }
            debug.log("Allocate location %s for interval %s", location, current);
            current.setLocation(location);
        }

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
         * @return the list of free stack slots for {@code size}. If there is none a list is
         *         created.
         */
        private Deque<StackSlot> getOrInitFreeSlots(SlotSize size)
        {
            assert size != SlotSize.Illegal;
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
            assert freeList != null;
            return freeList;
        }

        /**
         * Gets a free stack slot for {@code slot} or {@code null} if there is none.
         */
        private StackSlot findFreeSlot(SimpleVirtualStackSlot slot)
        {
            assert slot != null;
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
            for (int id = next.from(); activePeekId() < id;)
            {
                finished(active.poll());
            }
            debug.log("active %s", next);
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
            debug.log("finished %s (freeing %s)", interval, location);
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
                op.forEachState(assignSlot);

                op.forEachTemp(assignSlot);
                op.forEachOutput(assignSlot);
            }
        }

        ValueProcedure assignSlot = new ValueProcedure()
        {
            @Override
            public Value doValue(Value value, OperandMode mode, EnumSet<OperandFlag> flags)
            {
                if (isVirtualStackSlot(value))
                {
                    VirtualStackSlot slot = asVirtualStackSlot(value);
                    StackInterval interval = get(slot);
                    assert interval != null;
                    return interval.location();
                }
                return value;
            }
        };

        // ====================
        //
        // ====================

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

        private void dumpIntervals(String label)
        {
            debug.dump(DebugContext.VERBOSE_LEVEL, new StackIntervalDumper(Arrays.copyOf(stackSlotMap, stackSlotMap.length)), label);
        }
    }
}
