package graalvm.compiler.hotspot.amd64;

import jdk.vm.ci.meta.AllocatableValue;

import graalvm.compiler.asm.amd64.AMD64MacroAssembler;
import graalvm.compiler.lir.LIRInstruction.OperandFlag;
import graalvm.compiler.lir.LIRInstructionClass;
import graalvm.compiler.lir.amd64.AMD64BlockEndOp;
import graalvm.compiler.lir.asm.CompilationResultBuilder;

/**
 * @see AMD64HotSpotEpilogueOp
 */
abstract class AMD64HotSpotEpilogueBlockEndOp extends AMD64BlockEndOp implements AMD64HotSpotRestoreRbpOp
{
    protected AMD64HotSpotEpilogueBlockEndOp(LIRInstructionClass<? extends AMD64HotSpotEpilogueBlockEndOp> c)
    {
        super(c);
    }

    @Use({OperandFlag.REG, OperandFlag.STACK}) protected AllocatableValue savedRbp = PLACEHOLDER;

    protected void leaveFrameAndRestoreRbp(CompilationResultBuilder crb, AMD64MacroAssembler masm)
    {
        AMD64HotSpotEpilogueOp.leaveFrameAndRestoreRbp(savedRbp, crb, masm);
    }

    @Override
    public void setSavedRbp(AllocatableValue value)
    {
        savedRbp = value;
    }
}
