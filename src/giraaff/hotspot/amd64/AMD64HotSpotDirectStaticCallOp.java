package giraaff.hotspot.amd64;

import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.Value;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.hotspot.HotSpotRuntime;
import giraaff.lir.LIRFrameState;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.amd64.AMD64Call.DirectCallOp;
import giraaff.lir.asm.CompilationResultBuilder;
import giraaff.nodes.CallTargetNode.InvokeKind;

/**
 * A direct call that complies with the conventions for such calls in HotSpot. It doesn't use an
 * inline cache so it's just a patchable call site.
 */
@Opcode
// @class AMD64HotSpotDirectStaticCallOp
final class AMD64HotSpotDirectStaticCallOp extends DirectCallOp
{
    // @def
    public static final LIRInstructionClass<AMD64HotSpotDirectStaticCallOp> TYPE = LIRInstructionClass.create(AMD64HotSpotDirectStaticCallOp.class);

    // @field
    private final InvokeKind invokeKind;

    // @cons
    AMD64HotSpotDirectStaticCallOp(ResolvedJavaMethod __target, Value __result, Value[] __parameters, Value[] __temps, LIRFrameState __state, InvokeKind __invokeKind)
    {
        super(TYPE, __target, __result, __parameters, __temps, __state);
        this.invokeKind = __invokeKind;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        __crb.recordMark(invokeKind == InvokeKind.Static ? HotSpotRuntime.invokestaticMark : HotSpotRuntime.invokespecialMark);
        super.emitCode(__crb, __masm);
    }
}
