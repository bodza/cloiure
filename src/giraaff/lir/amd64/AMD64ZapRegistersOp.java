package giraaff.lir.amd64;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterSaveLayout;
import jdk.vm.ci.meta.JavaConstant;

import org.graalvm.collections.EconomicSet;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.StandardOp.SaveRegistersOp;
import giraaff.lir.amd64.AMD64SaveRegistersOp;
import giraaff.lir.asm.CompilationResultBuilder;
import giraaff.lir.framemap.FrameMap;

/**
 * Writes well known garbage values to registers.
 */
@Opcode
// @class AMD64ZapRegistersOp
public final class AMD64ZapRegistersOp extends AMD64LIRInstruction implements SaveRegistersOp
{
    // @def
    public static final LIRInstructionClass<AMD64ZapRegistersOp> TYPE = LIRInstructionClass.create(AMD64ZapRegistersOp.class);

    /**
     * The registers that are zapped.
     */
    // @field
    protected final Register[] zappedRegisters;

    /**
     * The garbage values that are written to the registers.
     */
    // @field
    protected final JavaConstant[] zapValues;

    // @cons
    public AMD64ZapRegistersOp(Register[] __zappedRegisters, JavaConstant[] __zapValues)
    {
        super(TYPE);
        this.zappedRegisters = __zappedRegisters;
        this.zapValues = __zapValues;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        for (int __i = 0; __i < zappedRegisters.length; __i++)
        {
            Register __reg = zappedRegisters[__i];
            if (__reg != null)
            {
                AMD64Move.const2reg(__crb, __masm, __reg, zapValues[__i]);
            }
        }
    }

    @Override
    public boolean supportsRemove()
    {
        return true;
    }

    @Override
    public int remove(EconomicSet<Register> __doNotSave)
    {
        return AMD64SaveRegistersOp.prune(__doNotSave, zappedRegisters);
    }

    @Override
    public RegisterSaveLayout getMap(FrameMap __frameMap)
    {
        return new RegisterSaveLayout(new Register[0], new int[0]);
    }
}
