package graalvm.compiler.hotspot.amd64;

import graalvm.compiler.asm.amd64.AMD64MacroAssembler;
import graalvm.compiler.hotspot.GraalHotSpotVMConfig;
import graalvm.compiler.lir.LIRFrameState;
import graalvm.compiler.lir.LIRInstructionClass;
import graalvm.compiler.lir.Opcode;
import graalvm.compiler.lir.amd64.AMD64Call.DirectCallOp;
import graalvm.compiler.lir.asm.CompilationResultBuilder;
import graalvm.compiler.nodes.CallTargetNode.InvokeKind;

import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.Value;

/**
 * A direct call that complies with the conventions for such calls in HotSpot. It doesn't use an
 * inline cache so it's just a patchable call site.
 */
@Opcode("CALL_DIRECT")
final class AMD64HotSpotDirectStaticCallOp extends DirectCallOp {
    public static final LIRInstructionClass<AMD64HotSpotDirectStaticCallOp> TYPE = LIRInstructionClass.create(AMD64HotSpotDirectStaticCallOp.class);

    private final InvokeKind invokeKind;
    private final GraalHotSpotVMConfig config;

    AMD64HotSpotDirectStaticCallOp(ResolvedJavaMethod target, Value result, Value[] parameters, Value[] temps, LIRFrameState state, InvokeKind invokeKind, GraalHotSpotVMConfig config) {
        super(TYPE, target, result, parameters, temps, state);
        assert invokeKind.isDirect();
        this.invokeKind = invokeKind;
        this.config = config;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm) {
        crb.recordMark(invokeKind == InvokeKind.Static ? config.MARKID_INVOKESTATIC : config.MARKID_INVOKESPECIAL);
        super.emitCode(crb, masm);
    }
}
