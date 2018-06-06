package giraaff.hotspot.amd64;

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
// A direct call that complies with the conventions for such calls in HotSpot. It doesn't use an
// inline cache so it's just a patchable call site.
///
@LIROpcode
// @class AMD64HotSpotDirectStaticCallOp
final class AMD64HotSpotDirectStaticCallOp extends AMD64Call.DirectCallOp
{
    // @def
    public static final LIRInstructionClass<AMD64HotSpotDirectStaticCallOp> TYPE = LIRInstructionClass.create(AMD64HotSpotDirectStaticCallOp.class);

    // @field
    private final CallTargetNode.InvokeKind ___invokeKind;

    // @cons AMD64HotSpotDirectStaticCallOp
    AMD64HotSpotDirectStaticCallOp(ResolvedJavaMethod __target, Value __result, Value[] __parameters, Value[] __temps, LIRFrameState __state, CallTargetNode.InvokeKind __invokeKind)
    {
        super(TYPE, __target, __result, __parameters, __temps, __state);
        this.___invokeKind = __invokeKind;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        __crb.recordMark(this.___invokeKind == CallTargetNode.InvokeKind.Static ? HotSpotRuntime.invokestaticMark : HotSpotRuntime.invokespecialMark);
        super.emitCode(__crb, __masm);
    }
}
