package giraaff.lir.amd64;

import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.meta.JavaConstant;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.asm.CompilationResultBuilder;

/**
 * Writes well known garbage values to stack slots.
 */
@Opcode("ZAP_STACK")
public final class AMD64ZapStackOp extends AMD64LIRInstruction
{
    public static final LIRInstructionClass<AMD64ZapStackOp> TYPE = LIRInstructionClass.create(AMD64ZapStackOp.class);

    /**
     * The stack slots that are zapped.
     */
    @Def(OperandFlag.STACK) protected final StackSlot[] zappedStack;

    /**
     * The garbage values that are written to the stack.
     */
    protected final JavaConstant[] zapValues;

    public AMD64ZapStackOp(StackSlot[] zappedStack, JavaConstant[] zapValues)
    {
        super(TYPE);
        this.zappedStack = zappedStack;
        this.zapValues = zapValues;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
    {
        for (int i = 0; i < zappedStack.length; i++)
        {
            StackSlot slot = zappedStack[i];
            if (slot != null)
            {
                AMD64Move.const2stack(crb, masm, slot, zapValues[i]);
            }
        }
    }
}
