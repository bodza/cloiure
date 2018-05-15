package graalvm.compiler.hotspot.amd64;

import static graalvm.compiler.lir.LIRInstruction.OperandFlag.REG;
import static jdk.vm.ci.amd64.AMD64.rsp;
import static jdk.vm.ci.code.ValueUtil.asRegister;

import graalvm.compiler.asm.amd64.AMD64Address;
import graalvm.compiler.asm.amd64.AMD64MacroAssembler;
import graalvm.compiler.lir.LIRInstructionClass;
import graalvm.compiler.lir.Opcode;
import graalvm.compiler.lir.amd64.AMD64LIRInstruction;
import graalvm.compiler.lir.asm.CompilationResultBuilder;

import jdk.vm.ci.meta.AllocatableValue;

/**
 * Patch the return address of the current frame.
 */
@Opcode("PATCH_RETURN")
final class AMD64HotSpotPatchReturnAddressOp extends AMD64LIRInstruction {
    public static final LIRInstructionClass<AMD64HotSpotPatchReturnAddressOp> TYPE = LIRInstructionClass.create(AMD64HotSpotPatchReturnAddressOp.class);

    @Use(REG) AllocatableValue address;

    AMD64HotSpotPatchReturnAddressOp(AllocatableValue address) {
        super(TYPE);
        this.address = address;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm) {
        int frameSize = crb.frameMap.frameSize();
        masm.movq(new AMD64Address(rsp, frameSize), asRegister(address));
    }
}
