package giraaff.lir.framemap;

import java.util.BitSet;

import jdk.vm.ci.code.TargetDescription;

import giraaff.core.common.LIRKind;
import giraaff.lir.VirtualStackSlot;

/**
 * Represents a {@link #getSlots() numbered} range of {@link VirtualStackSlot virtual stack slot} of
 * size {@link TargetDescription#wordSize}.
 */
// @class VirtualStackSlotRange
public final class VirtualStackSlotRange extends VirtualStackSlot
{
    // @field
    private final BitSet objects;
    // @field
    private final int slots;

    // @cons
    public VirtualStackSlotRange(int __id, int __slots, BitSet __objects, LIRKind __kind)
    {
        super(__id, __kind);
        this.slots = __slots;
        this.objects = (BitSet) __objects.clone();
    }

    public int getSlots()
    {
        return slots;
    }

    public BitSet getObjects()
    {
        return (BitSet) objects.clone();
    }
}
