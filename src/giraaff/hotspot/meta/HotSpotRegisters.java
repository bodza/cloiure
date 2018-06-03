package giraaff.hotspot.meta;

import jdk.vm.ci.code.Register;

// @class HotSpotRegisters
public final class HotSpotRegisters implements HotSpotRegistersProvider
{
    // @field
    private final Register ___threadRegister;
    // @field
    private final Register ___heapBaseRegister;
    // @field
    private final Register ___stackPointerRegister;

    // @cons
    public HotSpotRegisters(Register __threadRegister, Register __heapBaseRegister, Register __stackPointerRegister)
    {
        super();
        this.___threadRegister = __threadRegister;
        this.___heapBaseRegister = __heapBaseRegister;
        this.___stackPointerRegister = __stackPointerRegister;
    }

    @Override
    public Register getThreadRegister()
    {
        return this.___threadRegister;
    }

    @Override
    public Register getHeapBaseRegister()
    {
        return this.___heapBaseRegister;
    }

    @Override
    public Register getStackPointerRegister()
    {
        return this.___stackPointerRegister;
    }
}
