package giraaff.lir.amd64;

import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.meta.JavaConstant;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.asm.CompilationResultBuilder;

///
// Writes well known garbage values to stack slots.
///
@Opcode
// @class AMD64ZapStackOp
public final class AMD64ZapStackOp extends AMD64LIRInstruction
{
    // @def
    public static final LIRInstructionClass<AMD64ZapStackOp> TYPE = LIRInstructionClass.create(AMD64ZapStackOp.class);

    ///
    // The stack slots that are zapped.
    ///
    @Def(OperandFlag.STACK)
    // @field
    protected final StackSlot[] ___zappedStack;

    ///
    // The garbage values that are written to the stack.
    ///
    // @field
    protected final JavaConstant[] ___zapValues;

    // @cons
    public AMD64ZapStackOp(StackSlot[] __zappedStack, JavaConstant[] __zapValues)
    {
        super(TYPE);
        this.___zappedStack = __zappedStack;
        this.___zapValues = __zapValues;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        for (int __i = 0; __i < this.___zappedStack.length; __i++)
        {
            StackSlot __slot = this.___zappedStack[__i];
            if (__slot != null)
            {
                AMD64Move.const2stack(__crb, __masm, __slot, this.___zapValues[__i]);
            }
        }
    }
}
