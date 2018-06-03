package giraaff.hotspot.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;

import giraaff.asm.amd64.AMD64Address;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.hotspot.HotSpotRuntime;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.amd64.AMD64LIRInstruction;
import giraaff.lir.asm.CompilationResultBuilder;

///
// Pushes an interpreter frame to the stack.
///
@Opcode
// @class AMD64HotSpotPushInterpreterFrameOp
final class AMD64HotSpotPushInterpreterFrameOp extends AMD64LIRInstruction
{
    // @def
    public static final LIRInstructionClass<AMD64HotSpotPushInterpreterFrameOp> TYPE = LIRInstructionClass.create(AMD64HotSpotPushInterpreterFrameOp.class);

    @Alive(OperandFlag.REG)
    // @field
    AllocatableValue ___frameSize;
    @Alive(OperandFlag.REG)
    // @field
    AllocatableValue ___framePc;
    @Alive(OperandFlag.REG)
    // @field
    AllocatableValue ___senderSp;
    @Alive(OperandFlag.REG)
    // @field
    AllocatableValue ___initialInfo;

    // @cons
    AMD64HotSpotPushInterpreterFrameOp(AllocatableValue __frameSize, AllocatableValue __framePc, AllocatableValue __senderSp, AllocatableValue __initialInfo)
    {
        super(TYPE);
        this.___frameSize = __frameSize;
        this.___framePc = __framePc;
        this.___senderSp = __senderSp;
        this.___initialInfo = __initialInfo;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        final Register __frameSizeRegister = ValueUtil.asRegister(this.___frameSize);
        final Register __framePcRegister = ValueUtil.asRegister(this.___framePc);
        final Register __senderSpRegister = ValueUtil.asRegister(this.___senderSp);
        final Register __initialInfoRegister = ValueUtil.asRegister(this.___initialInfo);
        final int __wordSize = 8;

        // we'll push PC and BP by hand
        __masm.subq(__frameSizeRegister, 2 * __wordSize);

        // push return address
        __masm.push(__framePcRegister);

        // prolog
        __masm.push(__initialInfoRegister);
        __masm.movq(__initialInfoRegister, AMD64.rsp);
        __masm.subq(AMD64.rsp, __frameSizeRegister);

        // this value is corrected by layout_activation_impl
        __masm.movptr(new AMD64Address(__initialInfoRegister, HotSpotRuntime.frameInterpreterFrameLastSpOffset * __wordSize), 0);

        // make the frame walkable
        __masm.movq(new AMD64Address(__initialInfoRegister, HotSpotRuntime.frameInterpreterFrameSenderSpOffset * __wordSize), __senderSpRegister);
    }
}
