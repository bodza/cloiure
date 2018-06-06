package giraaff.lir.amd64;

import java.util.BitSet;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.Value;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.asm.CompilationResultBuilder;

// @class AMD64VZeroUpper
public final class AMD64VZeroUpper extends AMD64LIRInstruction
{
    // @def
    public static final LIRInstructionClass<AMD64VZeroUpper> TYPE = LIRInstructionClass.create(AMD64VZeroUpper.class);

    @LIRInstruction.Temp
    // @field
    protected final RegisterValue[] ___xmmRegisters;

    // @cons AMD64VZeroUpper
    public AMD64VZeroUpper(Value[] __exclude)
    {
        super(TYPE);
        this.___xmmRegisters = initRegisterValues(__exclude);
    }

    private static RegisterValue[] initRegisterValues(Value[] __exclude)
    {
        BitSet __skippedRegs = new BitSet();
        int __numSkipped = 0;
        if (__exclude != null)
        {
            for (Value __value : __exclude)
            {
                if (ValueUtil.isRegister(__value) && ValueUtil.asRegister(__value).getRegisterCategory().equals(AMD64.XMM))
                {
                    __skippedRegs.set(ValueUtil.asRegister(__value).number);
                    __numSkipped++;
                }
            }
        }
        RegisterValue[] __regs = new RegisterValue[AMD64.xmmRegistersAVX512.length - __numSkipped];
        for (int __i = 0, j = 0; __i < AMD64.xmmRegistersAVX512.length; __i++)
        {
            Register __reg = AMD64.xmmRegistersAVX512[__i];
            if (!__skippedRegs.get(__reg.number))
            {
                __regs[j++] = __reg.asValue();
            }
        }
        return __regs;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __asm)
    {
        __asm.vzeroupper();
    }
}
