package giraaff.hotspot.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;

import giraaff.asm.amd64.AMD64Address;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.hotspot.GraalHotSpotVMConfig;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.amd64.AMD64LIRInstruction;
import giraaff.lir.asm.CompilationResultBuilder;

/**
 * Pushes an interpreter frame to the stack.
 */
@Opcode("PUSH_INTERPRETER_FRAME")
final class AMD64HotSpotPushInterpreterFrameOp extends AMD64LIRInstruction
{
    public static final LIRInstructionClass<AMD64HotSpotPushInterpreterFrameOp> TYPE = LIRInstructionClass.create(AMD64HotSpotPushInterpreterFrameOp.class);

    @Alive(OperandFlag.REG) AllocatableValue frameSize;
    @Alive(OperandFlag.REG) AllocatableValue framePc;
    @Alive(OperandFlag.REG) AllocatableValue senderSp;
    @Alive(OperandFlag.REG) AllocatableValue initialInfo;
    private final GraalHotSpotVMConfig config;

    AMD64HotSpotPushInterpreterFrameOp(AllocatableValue frameSize, AllocatableValue framePc, AllocatableValue senderSp, AllocatableValue initialInfo, GraalHotSpotVMConfig config)
    {
        super(TYPE);
        this.frameSize = frameSize;
        this.framePc = framePc;
        this.senderSp = senderSp;
        this.initialInfo = initialInfo;
        this.config = config;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
    {
        final Register frameSizeRegister = ValueUtil.asRegister(frameSize);
        final Register framePcRegister = ValueUtil.asRegister(framePc);
        final Register senderSpRegister = ValueUtil.asRegister(senderSp);
        final Register initialInfoRegister = ValueUtil.asRegister(initialInfo);
        final int wordSize = 8;

        // we'll push PC and BP by hand
        masm.subq(frameSizeRegister, 2 * wordSize);

        // push return address
        masm.push(framePcRegister);

        // prolog
        masm.push(initialInfoRegister);
        masm.movq(initialInfoRegister, AMD64.rsp);
        masm.subq(AMD64.rsp, frameSizeRegister);

        // this value is corrected by layout_activation_impl
        masm.movptr(new AMD64Address(initialInfoRegister, config.frameInterpreterFrameLastSpOffset * wordSize), 0);

        // make the frame walkable
        masm.movq(new AMD64Address(initialInfoRegister, config.frameInterpreterFrameSenderSpOffset * wordSize), senderSpRegister);
    }
}
