package graalvm.compiler.lir.amd64;

import static graalvm.compiler.lir.LIRInstruction.OperandFlag.ILLEGAL;
import static graalvm.compiler.lir.LIRInstruction.OperandFlag.REG;
import static graalvm.compiler.lir.LIRInstruction.OperandFlag.STACK;
import static jdk.vm.ci.code.ValueUtil.asRegister;
import static jdk.vm.ci.code.ValueUtil.isRegister;

import graalvm.compiler.asm.amd64.AMD64Address;
import graalvm.compiler.asm.amd64.AMD64Assembler.AMD64MOp;
import graalvm.compiler.asm.amd64.AMD64Assembler.OperandSize;
import graalvm.compiler.asm.amd64.AMD64MacroAssembler;
import graalvm.compiler.core.common.LIRKind;
import graalvm.compiler.lir.LIRFrameState;
import graalvm.compiler.lir.LIRInstructionClass;
import graalvm.compiler.lir.Opcode;
import graalvm.compiler.lir.asm.CompilationResultBuilder;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Value;

/**
 * AMD64 mul/div operation. This operation has a single operand for the second input. The first
 * input must be in RAX for mul and in RDX:RAX for div. The result is in RDX:RAX.
 */
public class AMD64MulDivOp extends AMD64LIRInstruction
{
    public static final LIRInstructionClass<AMD64MulDivOp> TYPE = LIRInstructionClass.create(AMD64MulDivOp.class);

    @Opcode private final AMD64MOp opcode;
    private final OperandSize size;

    @Def({REG}) protected AllocatableValue highResult;
    @Def({REG}) protected AllocatableValue lowResult;

    @Use({REG, ILLEGAL}) protected AllocatableValue highX;
    @Use({REG}) protected AllocatableValue lowX;

    @Use({REG, STACK}) protected AllocatableValue y;

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
        if (isRegister(y))
        {
            opcode.emit(masm, size, asRegister(y));
        }
        else
        {
            opcode.emit(masm, size, (AMD64Address) crb.asAddress(y));
        }
    }
}
