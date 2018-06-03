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

///
// Pops the current frame off the stack including the return address and restores the return
// registers stored on the stack.
///
@Opcode
// @class AMD64HotSpotLeaveCurrentStackFrameOp
final class AMD64HotSpotLeaveCurrentStackFrameOp extends AMD64HotSpotEpilogueOp
{
    // @def
    public static final LIRInstructionClass<AMD64HotSpotLeaveCurrentStackFrameOp> TYPE = LIRInstructionClass.create(AMD64HotSpotLeaveCurrentStackFrameOp.class);

    // @field
    private final SaveRegistersOp ___saveRegisterOp;

    // @cons
    AMD64HotSpotLeaveCurrentStackFrameOp(SaveRegistersOp __saveRegisterOp)
    {
        super(TYPE);
        this.___saveRegisterOp = __saveRegisterOp;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        FrameMap __frameMap = __crb.___frameMap;
        RegisterConfig __registerConfig = __frameMap.getRegisterConfig();
        RegisterSaveLayout __registerSaveLayout = this.___saveRegisterOp.getMap(__frameMap);
        Register __stackPointer = __registerConfig.getFrameRegister();

        // Restore integer result register.
        final int __stackSlotSize = __frameMap.getTarget().wordSize;
        Register __integerResultRegister = __registerConfig.getReturnRegister(JavaKind.Long);
        __masm.movptr(__integerResultRegister, new AMD64Address(__stackPointer, __registerSaveLayout.registerToSlot(__integerResultRegister) * __stackSlotSize));
        __masm.movptr(AMD64.rdx, new AMD64Address(__stackPointer, __registerSaveLayout.registerToSlot(AMD64.rdx) * __stackSlotSize));

        // Restore float result register.
        Register __floatResultRegister = __registerConfig.getReturnRegister(JavaKind.Double);
        __masm.movdbl(__floatResultRegister, new AMD64Address(__stackPointer, __registerSaveLayout.registerToSlot(__floatResultRegister) * __stackSlotSize));

        // All of the register save area will be popped of the stack. Only the return address remains.
        leaveFrameAndRestoreRbp(__crb, __masm);

        // Remove return address.
        __masm.addq(__stackPointer, __crb.___target.arch.getReturnAddressSize());
    }
}
