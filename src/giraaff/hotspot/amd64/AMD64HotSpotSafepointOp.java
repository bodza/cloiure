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

///
// Emits a safepoint poll.
///
@Opcode
// @class AMD64HotSpotSafepointOp
public final class AMD64HotSpotSafepointOp extends AMD64LIRInstruction
{
    // @def
    public static final LIRInstructionClass<AMD64HotSpotSafepointOp> TYPE = LIRInstructionClass.create(AMD64HotSpotSafepointOp.class);

    // @State
    // @field
    protected LIRFrameState ___state;
    @Temp({OperandFlag.REG, OperandFlag.ILLEGAL})
    // @field
    private AllocatableValue ___temp;

    // @field
    private final Register ___thread;

    // @cons
    public AMD64HotSpotSafepointOp(LIRFrameState __state, NodeLIRBuilderTool __tool, Register __thread)
    {
        super(TYPE);
        this.___state = __state;
        this.___thread = __thread;
        if (HotSpotRuntime.threadLocalHandshakes || isPollingPageFar())
        {
            this.___temp = __tool.getLIRGeneratorTool().newVariable(LIRKind.value(__tool.getLIRGeneratorTool().target().arch.getWordKind()));
        }
        else
        {
            // don't waste a register if it's unneeded
            this.___temp = Value.ILLEGAL;
        }
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __asm)
    {
        emitCode(__crb, __asm, false, this.___state, this.___thread, this.___temp instanceof RegisterValue ? ((RegisterValue) this.___temp).getRegister() : null);
    }

    public static void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __asm, boolean __atReturn, LIRFrameState __state, Register __thread, Register __scratch)
    {
        if (HotSpotRuntime.threadLocalHandshakes)
        {
            emitThreadLocalPoll(__crb, __asm, __atReturn, __state, __thread, __scratch);
        }
        else
        {
            emitGlobalPoll(__crb, __asm, __atReturn, __state, __scratch);
        }
    }

    ///
    // Tests if the polling page address can be reached from the code cache with 32-bit displacements.
    ///
    private static boolean isPollingPageFar()
    {
        final long __pollingPageAddress = HotSpotRuntime.safepointPollingAddress;
        return !NumUtil.isInt(__pollingPageAddress - HotSpotRuntime.codeCacheLowBound) || !NumUtil.isInt(__pollingPageAddress - HotSpotRuntime.codeCacheHighBound);
    }

    private static void emitGlobalPoll(CompilationResultBuilder __crb, AMD64MacroAssembler __asm, boolean __atReturn, LIRFrameState __state, Register __scratch)
    {
        if (isPollingPageFar())
        {
            __asm.movq(__scratch, HotSpotRuntime.safepointPollingAddress);
            __crb.recordMark(__atReturn ? HotSpotRuntime.pollReturnFarMark : HotSpotRuntime.pollFarMark);
            __asm.testl(AMD64.rax, new AMD64Address(__scratch));
        }
        else
        {
            __crb.recordMark(__atReturn ? HotSpotRuntime.pollReturnNearMark : HotSpotRuntime.pollNearMark);
            // The C++ code transforms the polling page offset into an RIP displacement
            // to the real address at that offset in the polling page.
            __asm.testl(AMD64.rax, new AMD64Address(AMD64.rip, 0));
        }
    }

    private static void emitThreadLocalPoll(CompilationResultBuilder __crb, AMD64MacroAssembler __asm, boolean __atReturn, LIRFrameState __state, Register __thread, Register __scratch)
    {
        __asm.movptr(__scratch, new AMD64Address(__thread, HotSpotRuntime.threadPollingPageOffset));
        __crb.recordMark(__atReturn ? HotSpotRuntime.pollReturnFarMark : HotSpotRuntime.pollFarMark);
        __asm.testl(AMD64.rax, new AMD64Address(__scratch));
    }
}
