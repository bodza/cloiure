package graalvm.compiler.hotspot.amd64;

import static graalvm.compiler.lir.LIRInstruction.OperandFlag.REG;
import static graalvm.compiler.lir.LIRInstruction.OperandFlag.STACK;
import static jdk.vm.ci.amd64.AMD64.rbp;
import static jdk.vm.ci.code.ValueUtil.asRegister;
import static jdk.vm.ci.code.ValueUtil.isStackSlot;

import graalvm.compiler.asm.amd64.AMD64Address;
import graalvm.compiler.asm.amd64.AMD64MacroAssembler;
import graalvm.compiler.lir.LIRInstructionClass;
import graalvm.compiler.lir.amd64.AMD64LIRInstruction;
import graalvm.compiler.lir.asm.CompilationResultBuilder;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.meta.AllocatableValue;

/**
 * Superclass for operations that use the value of RBP saved in a method's prologue.
 */
abstract class AMD64HotSpotEpilogueOp extends AMD64LIRInstruction implements AMD64HotSpotRestoreRbpOp
{
    protected AMD64HotSpotEpilogueOp(LIRInstructionClass<? extends AMD64HotSpotEpilogueOp> c)
    {
        super(c);
    }

    @Use({REG, STACK}) private AllocatableValue savedRbp = PLACEHOLDER;

    protected void leaveFrameAndRestoreRbp(CompilationResultBuilder crb, AMD64MacroAssembler masm)
    {
        leaveFrameAndRestoreRbp(savedRbp, crb, masm);
    }

    static void leaveFrameAndRestoreRbp(AllocatableValue savedRbp, CompilationResultBuilder crb, AMD64MacroAssembler masm)
    {
        if (isStackSlot(savedRbp))
        {
            // Restoring RBP from the stack must be done before the frame is removed
            masm.movq(rbp, (AMD64Address) crb.asAddress(savedRbp));
        }
        else
        {
            Register framePointer = asRegister(savedRbp);
            if (!framePointer.equals(rbp))
            {
                masm.movq(rbp, framePointer);
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
