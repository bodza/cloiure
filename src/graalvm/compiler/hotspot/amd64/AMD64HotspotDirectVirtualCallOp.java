package graalvm.compiler.hotspot.amd64;

import graalvm.compiler.asm.amd64.AMD64MacroAssembler;
import graalvm.compiler.hotspot.GraalHotSpotVMConfig;
import graalvm.compiler.lir.LIRFrameState;
import graalvm.compiler.lir.LIRInstructionClass;
import graalvm.compiler.lir.Opcode;
import graalvm.compiler.lir.amd64.AMD64Call.DirectCallOp;
import graalvm.compiler.lir.asm.CompilationResultBuilder;
import graalvm.compiler.nodes.CallTargetNode.InvokeKind;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.Value;

/**
 * A direct call that complies with the conventions for such calls in HotSpot. In particular, for
 * calls using an inline cache, a MOVE instruction is emitted just prior to the aligned direct call.
 */
@Opcode("CALL_DIRECT")
final class AMD64HotspotDirectVirtualCallOp extends DirectCallOp
{
    public static final LIRInstructionClass<AMD64HotspotDirectVirtualCallOp> TYPE = LIRInstructionClass.create(AMD64HotspotDirectVirtualCallOp.class);

    private final InvokeKind invokeKind;
    private final GraalHotSpotVMConfig config;

    AMD64HotspotDirectVirtualCallOp(ResolvedJavaMethod target, Value result, Value[] parameters, Value[] temps, LIRFrameState state, InvokeKind invokeKind, GraalHotSpotVMConfig config)
    {
        super(TYPE, target, result, parameters, temps, state);
        this.invokeKind = invokeKind;
        this.config = config;
        assert invokeKind.isIndirect();
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
    {
        // The mark for an invocation that uses an inline cache must be placed at the
        // instruction that loads the Klass from the inline cache.
        crb.recordMark(invokeKind == InvokeKind.Virtual ? config.MARKID_INVOKEVIRTUAL : config.MARKID_INVOKEINTERFACE);
        // This must be emitted exactly like this to ensure it's patchable
        masm.movq(AMD64.rax, config.nonOopBits);
        int offset = super.emitCall(crb, masm);
        crb.recordInvokeVirtualOrInterfaceCallOp(offset, getPosition());
    }
}
