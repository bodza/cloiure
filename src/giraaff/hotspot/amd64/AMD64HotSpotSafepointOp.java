package giraaff.hotspot.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Value;

import giraaff.asm.amd64.AMD64Address;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.core.common.LIRKind;
import giraaff.core.common.NumUtil;
import giraaff.hotspot.HotSpotRuntime;
import giraaff.lir.LIRFrameState;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.amd64.AMD64LIRInstruction;
import giraaff.lir.asm.CompilationResultBuilder;
import giraaff.nodes.spi.NodeLIRBuilderTool;

/**
 * Emits a safepoint poll.
 */
@Opcode
// @class AMD64HotSpotSafepointOp
public final class AMD64HotSpotSafepointOp extends AMD64LIRInstruction
{
    public static final LIRInstructionClass<AMD64HotSpotSafepointOp> TYPE = LIRInstructionClass.create(AMD64HotSpotSafepointOp.class);

    // @State
    protected LIRFrameState state;
    @Temp({OperandFlag.REG, OperandFlag.ILLEGAL}) private AllocatableValue temp;

    private final Register thread;

    // @cons
    public AMD64HotSpotSafepointOp(LIRFrameState state, NodeLIRBuilderTool tool, Register thread)
    {
        super(TYPE);
        this.state = state;
        this.thread = thread;
        if (HotSpotRuntime.threadLocalHandshakes || isPollingPageFar())
        {
            temp = tool.getLIRGeneratorTool().newVariable(LIRKind.value(tool.getLIRGeneratorTool().target().arch.getWordKind()));
        }
        else
        {
            // don't waste a register if it's unneeded
            temp = Value.ILLEGAL;
        }
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler asm)
    {
        emitCode(crb, asm, false, state, thread, temp instanceof RegisterValue ? ((RegisterValue) temp).getRegister() : null);
    }

    public static void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler asm, boolean atReturn, LIRFrameState state, Register thread, Register scratch)
    {
        if (HotSpotRuntime.threadLocalHandshakes)
        {
            emitThreadLocalPoll(crb, asm, atReturn, state, thread, scratch);
        }
        else
        {
            emitGlobalPoll(crb, asm, atReturn, state, scratch);
        }
    }

    /**
     * Tests if the polling page address can be reached from the code cache with 32-bit displacements.
     */
    private static boolean isPollingPageFar()
    {
        final long pollingPageAddress = HotSpotRuntime.safepointPollingAddress;
        return !NumUtil.isInt(pollingPageAddress - HotSpotRuntime.codeCacheLowBound) || !NumUtil.isInt(pollingPageAddress - HotSpotRuntime.codeCacheHighBound);
    }

    private static void emitGlobalPoll(CompilationResultBuilder crb, AMD64MacroAssembler asm, boolean atReturn, LIRFrameState state, Register scratch)
    {
        if (isPollingPageFar())
        {
            asm.movq(scratch, HotSpotRuntime.safepointPollingAddress);
            crb.recordMark(atReturn ? HotSpotRuntime.pollReturnFarMark : HotSpotRuntime.pollFarMark);
            asm.testl(AMD64.rax, new AMD64Address(scratch));
        }
        else
        {
            crb.recordMark(atReturn ? HotSpotRuntime.pollReturnNearMark : HotSpotRuntime.pollNearMark);
            // The C++ code transforms the polling page offset into an RIP displacement
            // to the real address at that offset in the polling page.
            asm.testl(AMD64.rax, new AMD64Address(AMD64.rip, 0));
        }
    }

    private static void emitThreadLocalPoll(CompilationResultBuilder crb, AMD64MacroAssembler asm, boolean atReturn, LIRFrameState state, Register thread, Register scratch)
    {
        asm.movptr(scratch, new AMD64Address(thread, HotSpotRuntime.threadPollingPageOffset));
        crb.recordMark(atReturn ? HotSpotRuntime.pollReturnFarMark : HotSpotRuntime.pollFarMark);
        asm.testl(AMD64.rax, new AMD64Address(scratch));
    }
}
