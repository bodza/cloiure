package giraaff.lir.framemap;

import jdk.vm.ci.meta.ValueKind;

import giraaff.lir.VirtualStackSlot;

/**
 * Represents a {@link VirtualStackSlot virtual stack slot} for a specific {@link ValueKind kind}.
 */
// @class SimpleVirtualStackSlot
public final class SimpleVirtualStackSlot extends VirtualStackSlot
{
    // @cons
    public SimpleVirtualStackSlot(int __id, ValueKind<?> __kind)
    {
        super(__id, __kind);
    }
}
