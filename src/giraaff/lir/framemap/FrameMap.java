package giraaff.lir.framemap;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import jdk.vm.ci.code.BailoutException;
import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.code.CodeCacheProvider;
import jdk.vm.ci.code.RegisterConfig;
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.Value;
import jdk.vm.ci.meta.ValueKind;

import giraaff.core.common.LIRKind;
import giraaff.core.common.NumUtil;

/**
 * This class is used to build the stack frame layout for a compiled method. A {@link StackSlot} is
 * used to index slots of the frame relative to the stack pointer. The frame size is only fixed
 * after register allocation when all spill slots have been allocated. Both the outgoing argument
 * area and the spill are can grow until then. Therefore, outgoing arguments are indexed from the
 * stack pointer, while spill slots are indexed from the beginning of the frame (and the total frame
 * size has to be added to get the actual offset from the stack pointer).
 */
// @class FrameMap
public abstract class FrameMap
{
    // @field
    private final TargetDescription target;
    // @field
    private final RegisterConfig registerConfig;

    /**
     * The final frame size, not including the size of the
     * {@link Architecture#getReturnAddressSize() return address slot}. The value is only set after
     * register allocation is complete, i.e., after all spill slots have been allocated.
     */
    // @field
    private int frameSize;

    /**
     * Initial size of the area occupied by spill slots and other stack-allocated memory blocks.
     */
    // @field
    protected int initialSpillSize;

    /**
     * Size of the area occupied by spill slots and other stack-allocated memory blocks.
     */
    // @field
    protected int spillSize;

    /**
     * Size of the area occupied by outgoing overflow arguments. This value is adjusted as calling
     * conventions for outgoing calls are retrieved. On some platforms, there is a minimum outgoing
     * size even if no overflow arguments are on the stack.
     */
    // @field
    protected int outgoingSize;

    /**
     * Determines if this frame has values on the stack for outgoing calls.
     */
    // @field
    protected boolean hasOutgoingStackArguments;

    /**
     * The list of stack slots allocated in this frame that are present in every reference map.
     */
    // @field
    private final List<StackSlot> objectStackSlots;

    /**
     * Records whether an offset to an incoming stack argument was ever returned by
     * {@link #offsetForStackSlot(StackSlot)}.
     */
    // @field
    private boolean accessesCallerFrame;

    /**
     * Creates a new frame map for the specified method. The given registerConfig is optional, in
     * case null is passed the default RegisterConfig from the CodeCacheProvider will be used.
     */
    // @cons
    public FrameMap(CodeCacheProvider __codeCache, RegisterConfig __registerConfig)
    {
        super();
        this.target = __codeCache.getTarget();
        this.registerConfig = __registerConfig == null ? __codeCache.getRegisterConfig() : __registerConfig;
        this.frameSize = -1;
        this.outgoingSize = __codeCache.getMinimumOutgoingSize();
        this.objectStackSlots = new ArrayList<>();
    }

    public RegisterConfig getRegisterConfig()
    {
        return registerConfig;
    }

    public TargetDescription getTarget()
    {
        return target;
    }

    protected int returnAddressSize()
    {
        return getTarget().arch.getReturnAddressSize();
    }

    /**
     * Determines if an offset to an incoming stack argument was ever returned by
     * {@link #offsetForStackSlot(StackSlot)}.
     */
    public boolean accessesCallerFrame()
    {
        return accessesCallerFrame;
    }

    /**
     * Gets the frame size of the compiled frame, not including the size of the
     * {@link Architecture#getReturnAddressSize() return address slot}.
     *
     * @return The size of the frame (in bytes).
     */
    public int frameSize()
    {
        return frameSize;
    }

    public int outgoingSize()
    {
        return outgoingSize;
    }

    /**
     * Determines if any space is used in the frame apart from the
     * {@link Architecture#getReturnAddressSize() return address slot}.
     */
    public boolean frameNeedsAllocating()
    {
        int __unalignedFrameSize = spillSize - returnAddressSize();
        return hasOutgoingStackArguments || __unalignedFrameSize != 0;
    }

    /**
     * Gets the total frame size of the compiled frame, including the size of the
     * {@link Architecture#getReturnAddressSize() return address slot}.
     *
     * @return The total size of the frame (in bytes).
     */
    public abstract int totalFrameSize();

    /**
     * Gets the current size of this frame. This is the size that would be returned by
     * {@link #frameSize()} if {@link #finish()} were called now.
     */
    public abstract int currentFrameSize();

    /**
     * Aligns the given frame size to the stack alignment size and return the aligned size.
     *
     * @param size the initial frame size to be aligned
     * @return the aligned frame size
     */
    protected int alignFrameSize(int __size)
    {
        return NumUtil.roundUp(__size, getTarget().stackAlignment);
    }

    /**
     * Computes the final size of this frame. After this method has been called, methods that change
     * the frame size cannot be called anymore, e.g. no more spill slots or outgoing arguments can
     * be requested.
     */
    public void finish()
    {
        frameSize = currentFrameSize();
        if (frameSize > getRegisterConfig().getMaximumFrameSize())
        {
            throw new BailoutException("frame size (%d) exceeded maximum allowed frame size (%d)", frameSize, getRegisterConfig().getMaximumFrameSize());
        }
    }

    /**
     * Computes the offset of a stack slot relative to the frame register.
     *
     * @param slot a stack slot
     * @return the offset of the stack slot
     */
    public int offsetForStackSlot(StackSlot __slot)
    {
        if (__slot.isInCallerFrame())
        {
            accessesCallerFrame = true;
        }
        return __slot.getOffset(totalFrameSize());
    }

    /**
     * Informs the frame map that the compiled code calls a particular method, which may need stack
     * space for outgoing arguments.
     *
     * @param cc The calling convention for the called method.
     */
    public void callsMethod(CallingConvention __cc)
    {
        reserveOutgoing(__cc.getStackSize());
    }

    /**
     * Reserves space for stack-based outgoing arguments.
     *
     * @param argsSize The amount of space (in bytes) to reserve for stack-based outgoing arguments.
     */
    public void reserveOutgoing(int __argsSize)
    {
        outgoingSize = Math.max(outgoingSize, __argsSize);
        hasOutgoingStackArguments = hasOutgoingStackArguments || __argsSize > 0;
    }

    /**
     * Reserves a new spill slot in the frame of the method being compiled. The returned slot is
     * aligned on its natural alignment, i.e., an 8-byte spill slot is aligned at an 8-byte boundary.
     *
     * @param kind The kind of the spill slot to be reserved.
     * @return A spill slot denoting the reserved memory area.
     */
    protected StackSlot allocateNewSpillSlot(ValueKind<?> __kind, int __additionalOffset)
    {
        return StackSlot.get(__kind, -spillSize + __additionalOffset, true);
    }

    /**
     * Returns the spill slot size for the given {@link ValueKind}. The default value is the size in
     * bytes for the target architecture.
     *
     * @param kind the {@link ValueKind} to be stored in the spill slot.
     * @return the size in bytes
     */
    public int spillSlotSize(ValueKind<?> __kind)
    {
        return __kind.getPlatformKind().getSizeInBytes();
    }

    /**
     * Reserves a spill slot in the frame of the method being compiled. The returned slot is aligned
     * on its natural alignment, i.e., an 8-byte spill slot is aligned at an 8-byte boundary, unless
     * overridden by a subclass.
     *
     * @param kind The kind of the spill slot to be reserved.
     * @return A spill slot denoting the reserved memory area.
     */
    public StackSlot allocateSpillSlot(ValueKind<?> __kind)
    {
        int __size = spillSlotSize(__kind);
        spillSize = NumUtil.roundUp(spillSize + __size, __size);
        return allocateNewSpillSlot(__kind, 0);
    }

    /**
     * Returns the size of the stack slot range for {@code slots} objects.
     *
     * @param slots The number of slots.
     * @return The size in byte
     */
    public int spillSlotRangeSize(int __slots)
    {
        return __slots * getTarget().wordSize;
    }

    /**
     * Reserves a number of contiguous slots in the frame of the method being compiled. If the
     * requested number of slots is 0, this method returns {@code null}.
     *
     * @param slots the number of slots to reserve
     * @param objects specifies the indexes of the object pointer slots. The caller is responsible
     *            for guaranteeing that each such object pointer slot is initialized before any
     *            instruction that uses a reference map. Without this guarantee, the garbage
     *            collector could see garbage object values.
     * @return the first reserved stack slot (i.e., at the lowest address)
     */
    public StackSlot allocateStackSlots(int __slots, BitSet __objects)
    {
        if (__slots == 0)
        {
            return null;
        }
        spillSize += spillSlotRangeSize(__slots);

        if (!__objects.isEmpty())
        {
            StackSlot __result = null;
            for (int __slotIndex = 0; __slotIndex < __slots; __slotIndex++)
            {
                StackSlot __objectSlot = null;
                if (__objects.get(__slotIndex))
                {
                    __objectSlot = allocateNewSpillSlot(LIRKind.reference(getTarget().arch.getWordKind()), __slotIndex * getTarget().wordSize);
                    addObjectStackSlot(__objectSlot);
                }
                if (__slotIndex == 0)
                {
                    if (__objectSlot != null)
                    {
                        __result = __objectSlot;
                    }
                    else
                    {
                        __result = allocateNewSpillSlot(LIRKind.value(getTarget().arch.getWordKind()), 0);
                    }
                }
            }
            return __result;
        }
        else
        {
            return allocateNewSpillSlot(LIRKind.value(getTarget().arch.getWordKind()), 0);
        }
    }

    protected void addObjectStackSlot(StackSlot __objectSlot)
    {
        objectStackSlots.add(__objectSlot);
    }
}
