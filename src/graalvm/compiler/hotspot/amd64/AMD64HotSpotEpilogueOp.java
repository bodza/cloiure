package graalvm.compiler.hotspot.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;

import graalvm.compiler.asm.amd64.AMD64Address;
import graalvm.compiler.asm.amd64.AMD64MacroAssembler;
import graalvm.compiler.lir.LIRInstruction.OperandFlag;
import graalvm.compiler.lir.LIRInstructionClass;
import graalvm.compiler.lir.amd64.AMD64LIRInstruction;
import graalvm.compiler.lir.asm.CompilationResultBuilder;

/**
 * Superclass for operations that use the value of RBP saved in a method's prologue.
 */
abstract class AMD64HotSpotEpilogueOp extends AMD64LIRInstruction implements AMD64HotSpotRestoreRbpOp
{
    protected AMD64HotSpotEpilogueOp(LIRInstructionClass<? extends AMD64HotSpotEpilogueOp> c)
    {
        super(c);
    }

    @Use({OperandFlag.REG, OperandFlag.STACK}) private AllocatableValue savedRbp = PLACEHOLDER;

    protected void leaveFrameAndRestoreRbp(CompilationResultBuilder crb, AMD64MacroAssembler masm)
    {
        leaveFrameAndRestoreRbp(savedRbp, crb, masm);
    }

    static void leaveFrameAndRestoreRbp(AllocatableValue savedRbp, CompilationResultBuilder crb, AMD64MacroAssembler masm)
    {
        if (ValueUtil.isStackSlot(savedRbp))
        {
            // Restoring RBP from the stack must be done before the frame is removed
            masm.movq(AMD64.rbp, (AMD64Address) crb.asAddress(savedRbp));
        }
        else
        {
            Register framePointer = ValueUtil.asRegister(savedRbp);
            if (!framePointer.equals(AMD64.rbp))
            {
                masm.movq(AMD64.rbp, framePointer);
            }
        }
        crb.frameContext.leave(crb);
    }

    @Override
    public void setSavedRbp(AllocatableValue value)
    {
        savedRbp = value;
    }
}
