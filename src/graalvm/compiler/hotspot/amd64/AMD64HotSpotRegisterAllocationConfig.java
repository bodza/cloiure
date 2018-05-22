package graalvm.compiler.hotspot.amd64;

import java.util.ArrayList;
import java.util.BitSet;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterArray;
import jdk.vm.ci.code.RegisterConfig;

import graalvm.compiler.core.common.alloc.RegisterAllocationConfig;

class AMD64HotSpotRegisterAllocationConfig extends RegisterAllocationConfig
{
    /**
     * Specify priority of register selection within phases of register allocation. Highest priority
     * is first. A useful heuristic is to give registers a low priority when they are required by
     * machine instructions, like EAX and EDX on I486, and choose no-save registers before
     * save-on-call, & save-on-call before save-on-entry. Registers which participate in fixed
     * calling sequences should come last. Registers which are used as pairs must fall on an even
     * boundary.
     *
     * Adopted from x86_64.ad.
     */
    static final Register[] registerAllocationOrder =
    {
        AMD64.r10, AMD64.r11, AMD64.r8, AMD64.r9, AMD64.r12, AMD64.rcx, AMD64.rbx, AMD64.rdi, AMD64.rdx, AMD64.rsi, AMD64.rax, AMD64.rbp, AMD64.r13, AMD64.r14, /*r15,*/ /*rsp,*/
        AMD64.xmm0, AMD64.xmm1, AMD64.xmm2,  AMD64.xmm3,  AMD64.xmm4,  AMD64.xmm5,  AMD64.xmm6,  AMD64.xmm7,
        AMD64.xmm8, AMD64.xmm9, AMD64.xmm10, AMD64.xmm11, AMD64.xmm12, AMD64.xmm13, AMD64.xmm14, AMD64.xmm15
    };

    AMD64HotSpotRegisterAllocationConfig(RegisterConfig registerConfig, String[] allocationRestrictedTo)
    {
        super(registerConfig, allocationRestrictedTo);
    }

    @Override
    protected RegisterArray initAllocatable(RegisterArray registers)
    {
        BitSet regMap = new BitSet(registerConfig.getAllocatableRegisters().size());
        for (Register reg : registers)
        {
            regMap.set(reg.number);
        }

        ArrayList<Register> allocatableRegisters = new ArrayList<>(registers.size());
        for (Register reg : registerAllocationOrder)
        {
            if (regMap.get(reg.number))
            {
                allocatableRegisters.add(reg);
            }
        }

        return super.initAllocatable(new RegisterArray(allocatableRegisters));
    }
}
