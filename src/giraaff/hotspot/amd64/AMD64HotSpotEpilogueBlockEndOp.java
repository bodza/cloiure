package giraaff.hotspot.amd64;

import jdk.vm.ci.meta.AllocatableValue;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.amd64.AMD64BlockEndOp;
import giraaff.lir.asm.CompilationResultBuilder;

///
// @see AMD64HotSpotEpilogueOp
///
// @class AMD64HotSpotEpilogueBlockEndOp
abstract class AMD64HotSpotEpilogueBlockEndOp extends AMD64BlockEndOp implements AMD64HotSpotRestoreRbpOp
{
    // @cons
    protected AMD64HotSpotEpilogueBlockEndOp(LIRInstructionClass<? extends AMD64HotSpotEpilogueBlockEndOp> __c)
    {
        super(__c);
    }

    @Use({OperandFlag.REG, OperandFlag.STACK})
    // @field
    protected AllocatableValue ___savedRbp = AMD64HotSpotRestoreRbpOp.PLACEHOLDER;

    protected void leaveFrameAndRestoreRbp(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        AMD64HotSpotEpilogueOp.leaveFrameAndRestoreRbp(this.___savedRbp, __crb, __masm);
    }

    @Override
    public void setSavedRbp(AllocatableValue __value)
    {
        this.___savedRbp = __value;
    }
}
