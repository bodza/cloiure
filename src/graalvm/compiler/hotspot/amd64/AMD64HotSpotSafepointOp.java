package graalvm.compiler.hotspot.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.code.site.InfopointReason;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.Value;

import graalvm.compiler.asm.amd64.AMD64Address;
import graalvm.compiler.asm.amd64.AMD64MacroAssembler;
import graalvm.compiler.core.common.GraalOptions;
import graalvm.compiler.core.common.LIRKind;
import graalvm.compiler.core.common.NumUtil;
import graalvm.compiler.hotspot.GraalHotSpotVMConfig;
import graalvm.compiler.lir.LIRFrameState;
import graalvm.compiler.lir.LIRInstructionClass;
import graalvm.compiler.lir.Opcode;
import graalvm.compiler.lir.amd64.AMD64LIRInstruction;
import graalvm.compiler.lir.asm.CompilationResultBuilder;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;

/**
 * Emits a safepoint poll.
 */
@Opcode("SAFEPOINT")
public final class AMD64HotSpotSafepointOp extends AMD64LIRInstruction
{
    public static final LIRInstructionClass<AMD64HotSpotSafepointOp> TYPE = LIRInstructionClass.create(AMD64HotSpotSafepointOp.class);

    @State protected LIRFrameState state;
    @Temp({OperandFlag.REG, OperandFlag.ILLEGAL}) private AllocatableValue temp;

    private final GraalHotSpotVMConfig config;
    private final Register thread;

    public AMD64HotSpotSafepointOp(LIRFrameState state, GraalHotSpotVMConfig config, NodeLIRBuilderTool tool, Register thread)
    {
        super(TYPE);
        this.state = state;
        this.config = config;
        this.thread = thread;
        if (config.threadLocalHandshakes || isPollingPageFar(config) || GraalOptions.ImmutableCode.getValue(tool.getOptions()))
        {
            temp = tool.getLIRGeneratorTool().newVariable(LIRKind.value(tool.getLIRGeneratorTool().target().arch.getWordKind()));
        }
        else
        {
            // Don't waste a register if it's unneeded
            temp = Value.ILLEGAL;
        }
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler asm)
    {
        emitCode(crb, asm, config, false, state, thread, temp instanceof RegisterValue ? ((RegisterValue) temp).getRegister() : null);
    }

    public static void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler asm, GraalHotSpotVMConfig config, boolean atReturn, LIRFrameState state, Register thread, Register scratch)
    {
        if (config.threadLocalHandshakes)
        {
            emitThreadLocalPoll(crb, asm, config, atReturn, state, thread, scratch);
        }
        else
        {
            emitGlobalPoll(crb, asm, config, atReturn, state, scratch);
        }
    }

    /**
     * Tests if the polling page address can be reached from the code cache with 32-bit
     * displacements.
     */
    private static boolean isPollingPageFar(GraalHotSpotVMConfig config)
    {
        final long pollingPageAddress = config.safepointPollingAddress;
        return config.forceUnreachable || !NumUtil.isInt(pollingPageAddress - config.codeCacheLowBound) || !NumUtil.isInt(pollingPageAddress - config.codeCacheHighBound);
    }

    private static void emitGlobalPoll(CompilationResultBuilder crb, AMD64MacroAssembler asm, GraalHotSpotVMConfig config, boolean atReturn, LIRFrameState state, Register scratch)
    {
        if (GraalOptions.ImmutableCode.getValue(crb.getOptions()))
        {
            JavaKind hostWordKind = JavaKind.Long;
            int alignment = hostWordKind.getBitCount() / Byte.SIZE;
            JavaConstant pollingPageAddress = JavaConstant.forIntegerKind(hostWordKind, config.safepointPollingAddress);
            // This move will be patched to load the safepoint page from a data segment
            // co-located with the immutable code.
            if (GraalOptions.GeneratePIC.getValue(crb.getOptions()))
            {
                asm.movq(scratch, asm.getPlaceholder(-1));
            }
            else
            {
                asm.movq(scratch, (AMD64Address) crb.recordDataReferenceInCode(pollingPageAddress, alignment));
            }
            final int pos = asm.position();
            crb.recordMark(atReturn ? config.MARKID_POLL_RETURN_FAR : config.MARKID_POLL_FAR);
            if (state != null)
            {
                crb.recordInfopoint(pos, state, InfopointReason.SAFEPOINT);
            }
            asm.testl(AMD64.rax, new AMD64Address(scratch));
        }
        else if (isPollingPageFar(config))
        {
            asm.movq(scratch, config.safepointPollingAddress);
            crb.recordMark(atReturn ? config.MARKID_POLL_RETURN_FAR : config.MARKID_POLL_FAR);
            final int pos = asm.position();
            if (state != null)
            {
                crb.recordInfopoint(pos, state, InfopointReason.SAFEPOINT);
            }
            asm.testl(AMD64.rax, new AMD64Address(scratch));
        }
        else
        {
            crb.recordMark(atReturn ? config.MARKID_POLL_RETURN_NEAR : config.MARKID_POLL_NEAR);
            final int pos = asm.position();
            if (state != null)
            {
                crb.recordInfopoint(pos, state, InfopointReason.SAFEPOINT);
            }
            // The C++ code transforms the polling page offset into an RIP displacement
            // to the real address at that offset in the polling page.
            asm.testl(AMD64.rax, new AMD64Address(AMD64.rip, 0));
        }
    }

    private static void emitThreadLocalPoll(CompilationResultBuilder crb, AMD64MacroAssembler asm, GraalHotSpotVMConfig config, boolean atReturn, LIRFrameState state, Register thread, Register scratch)
    {
        asm.movptr(scratch, new AMD64Address(thread, config.threadPollingPageOffset));
        crb.recordMark(atReturn ? config.MARKID_POLL_RETURN_FAR : config.MARKID_POLL_FAR);
        final int pos = asm.position();
        if (state != null)
        {
            crb.recordInfopoint(pos, state, InfopointReason.SAFEPOINT);
        }
        asm.testl(AMD64.rax, new AMD64Address(scratch));
    }
}
