package giraaff.hotspot.meta;

import jdk.vm.ci.code.Register;

// @class HotSpotRegisters
public final class HotSpotRegisters implements HotSpotRegistersProvider
{
    private final Register threadRegister;
    private final Register heapBaseRegister;
    private final Register stackPointerRegister;

    // @cons
    public HotSpotRegisters(Register threadRegister, Register heapBaseRegister, Register stackPointerRegister)
    {
        super();
        this.threadRegister = threadRegister;
        this.heapBaseRegister = heapBaseRegister;
        this.stackPointerRegister = stackPointerRegister;
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
