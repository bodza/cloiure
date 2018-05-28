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
final class AMD64HotSpotPatchReturnAddressOp extends AMD64LIRInstruction
{
    public static final LIRInstructionClass<AMD64HotSpotPatchReturnAddressOp> TYPE = LIRInstructionClass.create(AMD64HotSpotPatchReturnAddressOp.class);

    @Use(OperandFlag.REG) AllocatableValue address;

    AMD64HotSpotPatchReturnAddressOp(AllocatableValue address)
    {
        super(TYPE);
        this.address = address;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
    {
        int frameSize = crb.frameMap.frameSize();
        masm.movq(new AMD64Address(AMD64.rsp, frameSize), ValueUtil.asRegister(address));
    }
}
