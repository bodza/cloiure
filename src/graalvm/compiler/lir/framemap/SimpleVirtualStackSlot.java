package graalvm.compiler.lir.framemap;

import jdk.vm.ci.meta.ValueKind;

import graalvm.compiler.lir.VirtualStackSlot;

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
