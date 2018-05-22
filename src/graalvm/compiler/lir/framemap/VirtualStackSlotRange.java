package graalvm.compiler.lir.framemap;

import java.util.BitSet;

import jdk.vm.ci.code.TargetDescription;

import graalvm.compiler.core.common.LIRKind;
import graalvm.compiler.lir.VirtualStackSlot;

/**
 * Represents a {@link #getSlots() numbered} range of {@link VirtualStackSlot virtual stack slot} of
 * size {@link TargetDescription#wordSize}.
 */
public class VirtualStackSlotRange extends VirtualStackSlot
{
    private final BitSet objects;
    private final int slots;

    public VirtualStackSlotRange(int id, int slots, BitSet objects, LIRKind kind)
    {
        super(id, kind);
        this.slots = slots;
        this.objects = (BitSet) objects.clone();
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
