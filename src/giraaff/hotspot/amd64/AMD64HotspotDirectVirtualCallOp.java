package giraaff.hotspot.amd64;

import jdk.vm.ci.amd64.AMD64;
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
 * A direct call that complies with the conventions for such calls in HotSpot. In particular, for
 * calls using an inline cache, a MOVE instruction is emitted just prior to the aligned direct call.
 */
@Opcode
// @class AMD64HotspotDirectVirtualCallOp
final class AMD64HotspotDirectVirtualCallOp extends DirectCallOp
{
    public static final LIRInstructionClass<AMD64HotspotDirectVirtualCallOp> TYPE = LIRInstructionClass.create(AMD64HotspotDirectVirtualCallOp.class);

    private final InvokeKind invokeKind;

    // @cons
    AMD64HotspotDirectVirtualCallOp(ResolvedJavaMethod target, Value result, Value[] parameters, Value[] temps, LIRFrameState state, InvokeKind invokeKind)
    {
        super(TYPE, target, result, parameters, temps, state);
        this.invokeKind = invokeKind;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
    {
        // The mark for an invocation that uses an inline cache must be placed
        // at the instruction that loads the Klass from the inline cache.
        crb.recordMark(invokeKind == InvokeKind.Virtual ? HotSpotRuntime.invokevirtualMark : HotSpotRuntime.invokeinterfaceMark);
        // This must be emitted exactly like this to ensure, it's patchable.
        masm.movq(AMD64.rax, HotSpotRuntime.nonOopBits);
        int offset = super.emitCall(crb, masm);
        crb.recordInvokeVirtualOrInterfaceCallOp(offset);
    }
}
