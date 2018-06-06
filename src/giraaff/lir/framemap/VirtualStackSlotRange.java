package giraaff.lir.framemap;

import java.util.BitSet;

import jdk.vm.ci.code.TargetDescription;

import giraaff.core.common.LIRKind;
import giraaff.lir.VirtualStackSlot;

///
// Represents a {@link #getSlots() numbered} range of {@link VirtualStackSlot virtual stack slot} of
// size {@link TargetDescription#wordSize}.
///
// @class VirtualStackSlotRange
public final class VirtualStackSlotRange extends VirtualStackSlot
{
    // @field
    private final BitSet ___objects;
    // @field
    private final int ___slots;

    // @cons VirtualStackSlotRange
    public VirtualStackSlotRange(int __id, int __slots, BitSet __objects, LIRKind __kind)
    {
        super(__id, __kind);
        this.___slots = __slots;
        this.___objects = (BitSet) __objects.clone();
    }

    public int getSlots()
    {
        return this.___slots;
    }

    public BitSet getObjects()
    {
        return (BitSet) this.___objects.clone();
    }
}
