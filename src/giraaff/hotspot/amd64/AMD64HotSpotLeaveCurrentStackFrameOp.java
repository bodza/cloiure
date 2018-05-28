package giraaff.hotspot.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterConfig;
import jdk.vm.ci.code.RegisterSaveLayout;
import jdk.vm.ci.meta.JavaKind;

import giraaff.asm.amd64.AMD64Address;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.StandardOp.SaveRegistersOp;
import giraaff.lir.asm.CompilationResultBuilder;
import giraaff.lir.framemap.FrameMap;

/**
 * Pops the current frame off the stack including the return address and restores the return
 * registers stored on the stack.
 */
@Opcode
final class AMD64HotSpotLeaveCurrentStackFrameOp extends AMD64HotSpotEpilogueOp
{
    public static final LIRInstructionClass<AMD64HotSpotLeaveCurrentStackFrameOp> TYPE = LIRInstructionClass.create(AMD64HotSpotLeaveCurrentStackFrameOp.class);

    private final SaveRegistersOp saveRegisterOp;

    AMD64HotSpotLeaveCurrentStackFrameOp(SaveRegistersOp saveRegisterOp)
    {
        super(TYPE);
        this.saveRegisterOp = saveRegisterOp;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
    {
        FrameMap frameMap = crb.frameMap;
        RegisterConfig registerConfig = frameMap.getRegisterConfig();
        RegisterSaveLayout registerSaveLayout = saveRegisterOp.getMap(frameMap);
        Register stackPointer = registerConfig.getFrameRegister();

        // Restore integer result register.
        final int stackSlotSize = frameMap.getTarget().wordSize;
        Register integerResultRegister = registerConfig.getReturnRegister(JavaKind.Long);
        masm.movptr(integerResultRegister, new AMD64Address(stackPointer, registerSaveLayout.registerToSlot(integerResultRegister) * stackSlotSize));
        masm.movptr(AMD64.rdx, new AMD64Address(stackPointer, registerSaveLayout.registerToSlot(AMD64.rdx) * stackSlotSize));

        // Restore float result register.
        Register floatResultRegister = registerConfig.getReturnRegister(JavaKind.Double);
        masm.movdbl(floatResultRegister, new AMD64Address(stackPointer, registerSaveLayout.registerToSlot(floatResultRegister) * stackSlotSize));

        // All of the register save area will be popped of the stack. Only the return address remains.
        leaveFrameAndRestoreRbp(crb, masm);

        // Remove return address.
        masm.addq(stackPointer, crb.target.arch.getReturnAddressSize());
    }
}
