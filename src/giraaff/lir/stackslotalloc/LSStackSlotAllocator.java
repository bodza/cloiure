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
import giraaff.lir.LIRValueUtil;
import giraaff.lir.ValueProcedure;
import giraaff.lir.VirtualStackSlot;
import giraaff.lir.framemap.FrameMapBuilderTool;
import giraaff.lir.framemap.SimpleVirtualStackSlot;
import giraaff.lir.framemap.VirtualStackSlotRange;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.phases.AllocationPhase;
import giraaff.lir.phases.LIRPhase;

///
// Linear Scan stack slot allocator.
//
// Remark: The analysis works under the assumption that a stack slot is no longer live after its last usage.
// If an {@link LIRInstruction instruction} transfers the raw address of the stack slot to another location, e.g.
// a registers, and this location is referenced later on, the {@link giraaff.lir.LIRInstruction.Use usage} of the
// stack slot must be marked with the {@link LIRInstruction.OperandFlag#UNINITIALIZED}. Otherwise the stack slot
// might be reused and its content destroyed.
///
// @class LSStackSlotAllocator
public final class LSStackSlotAllocator extends AllocationPhase
{
    @Override
    protected void run(TargetDescription __target, LIRGenerationResult __lirGenRes, AllocationPhase.AllocationContext __context)
    {
        allocateStackSlots((FrameMapBuilderTool) __lirGenRes.getFrameMapBuilder(), __lirGenRes);
        __lirGenRes.buildFrameMap();
    }

    public static void allocateStackSlots(FrameMapBuilderTool __builder, LIRGenerationResult __res)
    {
        if (__builder.getNumberOfStackSlots() > 0)
        {
            new LSStackSlotAllocator.Allocator(__res.getLIR(), __builder).allocate();
        }
    }

    // @class LSStackSlotAllocator.Allocator
    private static final class Allocator
    {
        // @field
        private final LIR ___lir;
        // @field
        private final FrameMapBuilderTool ___frameMapBuilder;
        // @field
        private final StackInterval[] ___stackSlotMap;
        // @field
        private final PriorityQueue<StackInterval> ___unhandled;
        // @field
        private final PriorityQueue<StackInterval> ___active;
        // @field
        private final AbstractBlockBase<?>[] ___sortedBlocks;
        // @field
        private final int ___maxOpId;

        // @cons LSStackSlotAllocator.Allocator
        private Allocator(LIR __lir, FrameMapBuilderTool __frameMapBuilder)
        {
            super();
            this.___lir = __lir;
            this.___frameMapBuilder = __frameMapBuilder;
            this.___stackSlotMap = new StackInterval[__frameMapBuilder.getNumberOfStackSlots()];
            this.___sortedBlocks = __lir.getControlFlowGraph().getBlocks();

            // insert by from
            this.___unhandled = new PriorityQueue<>((__a, __b) -> __a.from() - __b.from());
            // insert by to
            this.___active = new PriorityQueue<>((__a, __b) -> __a.to() - __b.to());

            // step 1: number instructions
            this.___maxOpId = numberInstructions(__lir, this.___sortedBlocks);
        }

        private void allocate()
        {
            // step 2: build intervals
            EconomicSet<LIRInstruction> __usePos = buildIntervals();
            // step 3: verify intervals
            // step 4: allocate stack slots
            allocateStackSlots();
            // step 5: assign stack slots
            assignStackSlots(__usePos);
        }

        // ====================
        // step 1: number instructions
        // ====================

        ///
        // Numbers all instructions in all blocks.
        //
        // @return The id of the last operation.
        ///
        private static int numberInstructions(LIR __lir, AbstractBlockBase<?>[] __sortedBlocks)
        {
            int __opId = 0;
            int __index = 0;
            for (AbstractBlockBase<?> __block : __sortedBlocks)
            {
                ArrayList<LIRInstruction> __instructions = __lir.getLIRforBlock(__block);

                int __numInst = __instructions.size();
                for (int __j = 0; __j < __numInst; __j++)
                {
                    LIRInstruction __op = __instructions.get(__j);
                    __op.setId(__opId);

                    __index++;
                    __opId += 2; // numbering of lirOps by two
                }
            }
            return __opId - 2;
        }

        // ====================
        // step 2: build intervals
        // ====================

        private EconomicSet<LIRInstruction> buildIntervals()
        {
            return new FixPointIntervalBuilder(this.___lir, this.___stackSlotMap, maxOpId()).build();
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
            for (StackInterval __interval : this.___stackSlotMap)
            {
                if (__interval != null)
                {
                    this.___unhandled.add(__interval);
                }
            }

            for (StackInterval __current = activateNext(); __current != null; __current = activateNext())
            {
                allocateSlot(__current);
            }
        }

        private void allocateSlot(StackInterval __current)
        {
            VirtualStackSlot __virtualSlot = __current.getOperand();
            final StackSlot __location;
            if (__virtualSlot instanceof VirtualStackSlotRange)
            {
                // No reuse of ranges (yet).
                VirtualStackSlotRange __slotRange = (VirtualStackSlotRange) __virtualSlot;
                __location = this.___frameMapBuilder.getFrameMap().allocateStackSlots(__slotRange.getSlots(), __slotRange.getObjects());
            }
            else
            {
                StackSlot __slot = findFreeSlot((SimpleVirtualStackSlot) __virtualSlot);
                if (__slot != null)
                {
                    // Free stack slot available. Note that we create a new one because the kind might not match.
                    __location = StackSlot.get(__current.kind(), __slot.getRawOffset(), __slot.getRawAddFrameSize());
                }
                else
                {
                    // Allocate new stack slot.
                    __location = this.___frameMapBuilder.getFrameMap().allocateSpillSlot(__virtualSlot.getValueKind());
                }
            }
            __current.setLocation(__location);
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

        private LSStackSlotAllocator.Allocator.SlotSize forKind(ValueKind<?> __kind)
        {
            switch (this.___frameMapBuilder.getFrameMap().spillSlotSize(__kind))
            {
                case 1:
                    return LSStackSlotAllocator.Allocator.SlotSize.Size1;
                case 2:
                    return LSStackSlotAllocator.Allocator.SlotSize.Size2;
                case 4:
                    return LSStackSlotAllocator.Allocator.SlotSize.Size4;
                case 8:
                    return LSStackSlotAllocator.Allocator.SlotSize.Size8;
                default:
                    return LSStackSlotAllocator.Allocator.SlotSize.Illegal;
            }
        }

        // @field
        private EnumMap<LSStackSlotAllocator.Allocator.SlotSize, Deque<StackSlot>> ___freeSlots;

        ///
        // @return The list of free stack slots for {@code size} or {@code null} if there is none.
        ///
        private Deque<StackSlot> getOrNullFreeSlots(LSStackSlotAllocator.Allocator.SlotSize __size)
        {
            if (this.___freeSlots == null)
            {
                return null;
            }
            return this.___freeSlots.get(__size);
        }

        ///
        // @return the list of free stack slots for {@code size}. If there is none a list is created.
        ///
        private Deque<StackSlot> getOrInitFreeSlots(LSStackSlotAllocator.Allocator.SlotSize __size)
        {
            Deque<StackSlot> __freeList;
            if (this.___freeSlots != null)
            {
                __freeList = this.___freeSlots.get(__size);
            }
            else
            {
                this.___freeSlots = new EnumMap<>(LSStackSlotAllocator.Allocator.SlotSize.class);
                __freeList = null;
            }
            if (__freeList == null)
            {
                __freeList = new ArrayDeque<>();
                this.___freeSlots.put(__size, __freeList);
            }
            return __freeList;
        }

        ///
        // Gets a free stack slot for {@code slot} or {@code null} if there is none.
        ///
        private StackSlot findFreeSlot(SimpleVirtualStackSlot __slot)
        {
            LSStackSlotAllocator.Allocator.SlotSize __size = forKind(__slot.getValueKind());
            if (__size == LSStackSlotAllocator.Allocator.SlotSize.Illegal)
            {
                return null;
            }
            Deque<StackSlot> __freeList = getOrNullFreeSlots(__size);
            if (__freeList == null)
            {
                return null;
            }
            return __freeList.pollLast();
        }

        ///
        // Adds a stack slot to the list of free slots.
        ///
        private void freeSlot(StackSlot __slot)
        {
            LSStackSlotAllocator.Allocator.SlotSize __size = forKind(__slot.getValueKind());
            if (__size == LSStackSlotAllocator.Allocator.SlotSize.Illegal)
            {
                return;
            }
            getOrInitFreeSlots(__size).addLast(__slot);
        }

        ///
        // Gets the next unhandled interval and finishes handled intervals.
        ///
        private StackInterval activateNext()
        {
            if (this.___unhandled.isEmpty())
            {
                return null;
            }
            StackInterval __next = this.___unhandled.poll();
            // finish handled intervals
            for (int __id = __next.from(); activePeekId() < __id; )
            {
                finished(this.___active.poll());
            }
            this.___active.add(__next);
            return __next;
        }

        ///
        // Gets the lowest {@link StackInterval#to() end position} of all active intervals. If there
        // is none {@link Integer#MAX_VALUE} is returned.
        ///
        private int activePeekId()
        {
            StackInterval __first = this.___active.peek();
            if (__first == null)
            {
                return Integer.MAX_VALUE;
            }
            return __first.to();
        }

        ///
        // Finishes {@code interval} by adding its location to the list of free stack slots.
        ///
        private void finished(StackInterval __interval)
        {
            StackSlot __location = __interval.location();
            freeSlot(__location);
        }

        // ====================
        // step 5: assign stack slots
        // ====================

        private void assignStackSlots(EconomicSet<LIRInstruction> __usePos)
        {
            for (LIRInstruction __op : __usePos)
            {
                __op.forEachInput(assignSlot);
                __op.forEachAlive(assignSlot);

                __op.forEachTemp(assignSlot);
                __op.forEachOutput(assignSlot);
            }
        }

        // @closure
        ValueProcedure assignSlot = new ValueProcedure()
        {
            @Override
            public Value doValue(Value __value, LIRInstruction.OperandMode __mode, EnumSet<LIRInstruction.OperandFlag> __flags)
            {
                if (LIRValueUtil.isVirtualStackSlot(__value))
                {
                    VirtualStackSlot __slot = LIRValueUtil.asVirtualStackSlot(__value);
                    StackInterval __interval = get(__slot);
                    return __interval.location();
                }
                return __value;
            }
        };

        ///
        // Gets the highest instruction id.
        ///
        private int maxOpId()
        {
            return this.___maxOpId;
        }

        private StackInterval get(VirtualStackSlot __stackSlot)
        {
            return this.___stackSlotMap[__stackSlot.getId()];
        }
    }
}
