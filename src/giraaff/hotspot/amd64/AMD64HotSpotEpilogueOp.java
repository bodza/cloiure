package giraaff.hotspot.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;

import giraaff.asm.amd64.AMD64Address;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.amd64.AMD64LIRInstruction;
import giraaff.lir.asm.CompilationResultBuilder;

///
// Superclass for operations that use the value of RBP saved in a method's prologue.
///
// @class AMD64HotSpotEpilogueOp
abstract class AMD64HotSpotEpilogueOp extends AMD64LIRInstruction implements AMD64HotSpotRestoreRbpOp
{
    // @cons
    protected AMD64HotSpotEpilogueOp(LIRInstructionClass<? extends AMD64HotSpotEpilogueOp> __c)
    {
        super(__c);
    }

    @Use({OperandFlag.REG, OperandFlag.STACK})
    // @field
    private AllocatableValue ___savedRbp = AMD64HotSpotRestoreRbpOp.PLACEHOLDER;

    protected void leaveFrameAndRestoreRbp(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        leaveFrameAndRestoreRbp(this.___savedRbp, __crb, __masm);
    }

    static void leaveFrameAndRestoreRbp(AllocatableValue __savedRbp, CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        if (ValueUtil.isStackSlot(__savedRbp))
        {
            // restoring RBP from the stack must be done before the frame is removed
            __masm.movq(AMD64.rbp, (AMD64Address) __crb.asAddress(__savedRbp));
        }
        else
        {
            Register __framePointer = ValueUtil.asRegister(__savedRbp);
            if (!__framePointer.equals(AMD64.rbp))
            {
                __masm.movq(AMD64.rbp, __framePointer);
            }
        }
        __crb.___frameContext.leave(__crb);
    }

    @Override
    public void setSavedRbp(AllocatableValue __value)
    {
        this.___savedRbp = __value;
    }
}
