package giraaff.hotspot.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.Value;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.hotspot.HotSpotRuntime;
import giraaff.lir.LIRFrameState;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.LIROpcode;
import giraaff.lir.amd64.AMD64Call;
import giraaff.lir.asm.CompilationResultBuilder;
import giraaff.nodes.CallTargetNode;

///
// A direct call that complies with the conventions for such calls in HotSpot. In particular, for
// calls using an inline cache, a MOVE instruction is emitted just prior to the aligned direct call.
///
@LIROpcode
// @class AMD64HotspotDirectVirtualCallOp
final class AMD64HotspotDirectVirtualCallOp extends AMD64Call.DirectCallOp
{
    // @def
    public static final LIRInstructionClass<AMD64HotspotDirectVirtualCallOp> TYPE = LIRInstructionClass.create(AMD64HotspotDirectVirtualCallOp.class);

    // @field
    private final CallTargetNode.InvokeKind ___invokeKind;

    // @cons AMD64HotspotDirectVirtualCallOp
    AMD64HotspotDirectVirtualCallOp(ResolvedJavaMethod __target, Value __result, Value[] __parameters, Value[] __temps, LIRFrameState __state, CallTargetNode.InvokeKind __invokeKind)
    {
        super(TYPE, __target, __result, __parameters, __temps, __state);
        this.___invokeKind = __invokeKind;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        // The mark for an invocation that uses an inline cache must be placed
        // at the instruction that loads the Klass from the inline cache.
        __crb.recordMark(this.___invokeKind == CallTargetNode.InvokeKind.Virtual ? HotSpotRuntime.invokevirtualMark : HotSpotRuntime.invokeinterfaceMark);
        // This must be emitted exactly like this to ensure, it's patchable.
        __masm.movq(AMD64.rax, HotSpotRuntime.nonOopBits);
        int __offset = super.emitCall(__crb, __masm);
        __crb.recordInvokeVirtualOrInterfaceCallOp(__offset);
    }
}
