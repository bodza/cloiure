package giraaff.hotspot.meta;

import jdk.vm.ci.code.Register;

// @class HotSpotRegisters
public final class HotSpotRegisters implements HotSpotRegistersProvider
{
    // @field
    private final Register threadRegister;
    // @field
    private final Register heapBaseRegister;
    // @field
    private final Register stackPointerRegister;

    // @cons
    public HotSpotRegisters(Register __threadRegister, Register __heapBaseRegister, Register __stackPointerRegister)
    {
        super();
        this.threadRegister = __threadRegister;
        this.heapBaseRegister = __heapBaseRegister;
        this.stackPointerRegister = __stackPointerRegister;
    }

    @Override
    public Register getThreadRegister()
    {
        return threadRegister;
    }

    @Override
    public Register getHeapBaseRegister()
    {
        return heapBaseRegister;
    }

    @Override
    public Register getStackPointerRegister()
    {
        return stackPointerRegister;
    }
}
