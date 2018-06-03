package giraaff.hotspot.amd64;

import java.util.ArrayList;
import java.util.BitSet;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterArray;
import jdk.vm.ci.code.RegisterConfig;

import giraaff.core.common.alloc.RegisterAllocationConfig;

// @class AMD64HotSpotRegisterAllocationConfig
final class AMD64HotSpotRegisterAllocationConfig extends RegisterAllocationConfig
{
    ///
    // Specify priority of register selection within phases of register allocation. Highest priority
    // is first. A useful heuristic is to give registers a low priority when they are required by
    // machine instructions, like EAX and EDX on I486, and choose no-save registers before save-on-call
    // and save-on-call before save-on-entry. Registers which participate in fixed calling sequences
    // should come last. Registers which are used as pairs must fall on an even boundary.
    //
    // Adopted from x86_64.ad.
    ///
    static final Register[] registerAllocationOrder =
    {
        AMD64.r10, AMD64.r11, AMD64.r8, AMD64.r9, AMD64.r12,
        AMD64.rcx, AMD64.rbx, AMD64.rdi, AMD64.rdx, AMD64.rsi, AMD64.rax, AMD64.rbp,
        AMD64.r13, AMD64.r14, // AMD64.r15, AMD64.rsp,
        AMD64.xmm0, AMD64.xmm1, AMD64.xmm2,  AMD64.xmm3,  AMD64.xmm4,  AMD64.xmm5,  AMD64.xmm6,  AMD64.xmm7,
        AMD64.xmm8, AMD64.xmm9, AMD64.xmm10, AMD64.xmm11, AMD64.xmm12, AMD64.xmm13, AMD64.xmm14, AMD64.xmm15
    };

    // @cons
    AMD64HotSpotRegisterAllocationConfig(RegisterConfig __registerConfig)
    {
        super(__registerConfig);
    }

    @Override
    protected RegisterArray initAllocatable(RegisterArray __registers)
    {
        BitSet __regMap = new BitSet(this.___registerConfig.getAllocatableRegisters().size());
        for (Register __reg : __registers)
        {
            __regMap.set(__reg.number);
        }

        ArrayList<Register> __allocatableRegisters = new ArrayList<>(__registers.size());
        for (Register __reg : registerAllocationOrder)
        {
            if (__regMap.get(__reg.number))
            {
                __allocatableRegisters.add(__reg);
            }
        }

        return super.initAllocatable(new RegisterArray(__allocatableRegisters));
    }
}
