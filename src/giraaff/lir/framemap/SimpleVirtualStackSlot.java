package giraaff.lir.framemap;

import jdk.vm.ci.meta.ValueKind;

import giraaff.lir.VirtualStackSlot;

/**
 * Represents a {@link VirtualStackSlot virtual stack slot} for a specific {@link ValueKind kind}.
 */
public class SimpleVirtualStackSlot extends VirtualStackSlot
{
    public SimpleVirtualStackSlot(int id, ValueKind<?> kind)
    {
        super(id, kind);
    }
}
