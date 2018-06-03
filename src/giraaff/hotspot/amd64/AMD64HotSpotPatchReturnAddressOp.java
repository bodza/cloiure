package giraaff.hotspot.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;

import giraaff.asm.amd64.AMD64Address;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.amd64.AMD64LIRInstruction;
import giraaff.lir.asm.CompilationResultBuilder;

/**
 * Patch the return address of the current frame.
 */
@Opcode
// @class AMD64HotSpotPatchReturnAddressOp
final class AMD64HotSpotPatchReturnAddressOp extends AMD64LIRInstruction
{
    // @def
    public static final LIRInstructionClass<AMD64HotSpotPatchReturnAddressOp> TYPE = LIRInstructionClass.create(AMD64HotSpotPatchReturnAddressOp.class);

    @Use(OperandFlag.REG)
    // @field
    AllocatableValue address;

    // @cons
    AMD64HotSpotPatchReturnAddressOp(AllocatableValue __address)
    {
        super(TYPE);
        this.address = __address;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        int __frameSize = __crb.frameMap.frameSize();
        __masm.movq(new AMD64Address(AMD64.rsp, __frameSize), ValueUtil.asRegister(address));
    }
}
