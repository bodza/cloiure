package graalvm.compiler.lir.amd64;

import java.util.BitSet;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.Value;

import graalvm.compiler.asm.amd64.AMD64MacroAssembler;
import graalvm.compiler.lir.LIRInstructionClass;
import graalvm.compiler.lir.asm.CompilationResultBuilder;

public class AMD64VZeroUpper extends AMD64LIRInstruction
{
    public static final LIRInstructionClass<AMD64VZeroUpper> TYPE = LIRInstructionClass.create(AMD64VZeroUpper.class);

    @Temp protected final RegisterValue[] xmmRegisters;

    public AMD64VZeroUpper(Value[] exclude)
    {
        super(TYPE);
        xmmRegisters = initRegisterValues(exclude);
    }

    private static RegisterValue[] initRegisterValues(Value[] exclude)
    {
        BitSet skippedRegs = new BitSet();
        int numSkipped = 0;
        if (exclude != null)
        {
            for (Value value : exclude)
            {
                if (ValueUtil.isRegister(value) && ValueUtil.asRegister(value).getRegisterCategory().equals(AMD64.XMM))
                {
                    skippedRegs.set(ValueUtil.asRegister(value).number);
                    numSkipped++;
                }
            }
        }
        RegisterValue[] regs = new RegisterValue[AMD64.xmmRegistersAVX512.length - numSkipped];
        for (int i = 0, j = 0; i < AMD64.xmmRegistersAVX512.length; i++)
        {
            Register reg = AMD64.xmmRegistersAVX512[i];
            if (!skippedRegs.get(reg.number))
            {
                regs[j++] = reg.asValue();
            }
        }
        return regs;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler asm)
    {
        asm.vzeroupper();
    }
}
