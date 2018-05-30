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
    public static final LIRInstructionClass<AMD64HotSpotDirectStaticCallOp> TYPE = LIRInstructionClass.create(AMD64HotSpotDirectStaticCallOp.class);

    private final InvokeKind invokeKind;

    // @cons
    AMD64HotSpotDirectStaticCallOp(ResolvedJavaMethod target, Value result, Value[] parameters, Value[] temps, LIRFrameState state, InvokeKind invokeKind)
    {
        super(TYPE, target, result, parameters, temps, state);
        this.invokeKind = invokeKind;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
    {
        crb.recordMark(invokeKind == InvokeKind.Static ? HotSpotRuntime.invokestaticMark : HotSpotRuntime.invokespecialMark);
        super.emitCode(crb, masm);
    }
}
