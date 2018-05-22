package giraaff.lir.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Value;

import giraaff.asm.amd64.AMD64Address;
import giraaff.asm.amd64.AMD64Assembler.AMD64MOp;
import giraaff.asm.amd64.AMD64Assembler.OperandSize;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.core.common.LIRKind;
import giraaff.lir.LIRFrameState;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.asm.CompilationResultBuilder;

/**
 * AMD64 mul/div operation. This operation has a single operand for the second input. The first
 * input must be in RAX for mul and in RDX:RAX for div. The result is in RDX:RAX.
 */
public class AMD64MulDivOp extends AMD64LIRInstruction
{
    public static final LIRInstructionClass<AMD64MulDivOp> TYPE = LIRInstructionClass.create(AMD64MulDivOp.class);

    @Opcode private final AMD64MOp opcode;
    private final OperandSize size;

    @Def({OperandFlag.REG}) protected AllocatableValue highResult;
    @Def({OperandFlag.REG}) protected AllocatableValue lowResult;

    @Use({OperandFlag.REG, OperandFlag.ILLEGAL}) protected AllocatableValue highX;
    @Use({OperandFlag.REG}) protected AllocatableValue lowX;

    @Use({OperandFlag.REG, OperandFlag.STACK}) protected AllocatableValue y;

    @State protected LIRFrameState state;

    public AMD64MulDivOp(AMD64MOp opcode, OperandSize size, LIRKind resultKind, AllocatableValue x, AllocatableValue y)
    {
        this(opcode, size, resultKind, Value.ILLEGAL, x, y, null);
    }

    public AMD64MulDivOp(AMD64MOp opcode, OperandSize size, LIRKind resultKind, AllocatableValue highX, AllocatableValue lowX, AllocatableValue y, LIRFrameState state)
    {
        super(TYPE);
        this.opcode = opcode;
        this.size = size;

        this.highResult = AMD64.rdx.asValue(resultKind);
        this.lowResult = AMD64.rax.asValue(resultKind);

        this.highX = highX;
        this.lowX = lowX;

        this.y = y;

        this.state = state;
    }

    public AllocatableValue getHighResult()
    {
        return highResult;
    }

    public AllocatableValue getLowResult()
    {
        return lowResult;
    }

    public AllocatableValue getQuotient()
    {
        return lowResult;
    }

    public AllocatableValue getRemainder()
    {
        return highResult;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
    {
        if (state != null)
        {
            crb.recordImplicitException(masm.position(), state);
        }
        if (ValueUtil.isRegister(y))
        {
            opcode.emit(masm, size, ValueUtil.asRegister(y));
        }
        else
        {
            opcode.emit(masm, size, (AMD64Address) crb.asAddress(y));
        }
    }
}
