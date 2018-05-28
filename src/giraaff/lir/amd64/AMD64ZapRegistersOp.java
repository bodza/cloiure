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
public final class AMD64ZapRegistersOp extends AMD64LIRInstruction implements SaveRegistersOp
{
    public static final LIRInstructionClass<AMD64ZapRegistersOp> TYPE = LIRInstructionClass.create(AMD64ZapRegistersOp.class);

    /**
     * The registers that are zapped.
     */
    protected final Register[] zappedRegisters;

    /**
     * The garbage values that are written to the registers.
     */
    protected final JavaConstant[] zapValues;

    public AMD64ZapRegistersOp(Register[] zappedRegisters, JavaConstant[] zapValues)
    {
        super(TYPE);
        this.zappedRegisters = zappedRegisters;
        this.zapValues = zapValues;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
    {
        for (int i = 0; i < zappedRegisters.length; i++)
        {
            Register reg = zappedRegisters[i];
            if (reg != null)
            {
                AMD64Move.const2reg(crb, masm, reg, zapValues[i]);
            }
        }
    }

    @Override
    public boolean supportsRemove()
    {
        return true;
    }

    @Override
    public int remove(EconomicSet<Register> doNotSave)
    {
        return AMD64SaveRegistersOp.prune(doNotSave, zappedRegisters);
    }

    @Override
    public RegisterSaveLayout getMap(FrameMap frameMap)
    {
        return new RegisterSaveLayout(new Register[0], new int[0]);
    }
}
