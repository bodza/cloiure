package giraaff.hotspot.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;

import giraaff.asm.amd64.AMD64Address;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.LIROpcode;
import giraaff.lir.amd64.AMD64LIRInstruction;
import giraaff.lir.asm.CompilationResultBuilder;

///
// Patch the return address of the current frame.
///
@LIROpcode
// @class AMD64HotSpotPatchReturnAddressOp
final class AMD64HotSpotPatchReturnAddressOp extends AMD64LIRInstruction
{
    // @def
    public static final LIRInstructionClass<AMD64HotSpotPatchReturnAddressOp> TYPE = LIRInstructionClass.create(AMD64HotSpotPatchReturnAddressOp.class);

    @LIRInstruction.Use(LIRInstruction.OperandFlag.REG)
    // @field
    AllocatableValue ___address;

    // @cons AMD64HotSpotPatchReturnAddressOp
    AMD64HotSpotPatchReturnAddressOp(AllocatableValue __address)
    {
        super(TYPE);
        this.___address = __address;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        int __frameSize = __crb.___frameMap.frameSize();
        __masm.movq(new AMD64Address(AMD64.rsp, __frameSize), ValueUtil.asRegister(this.___address));
    }
}
