package giraaff.hotspot.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.Value;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.hotspot.HotSpotRuntime;
import giraaff.lir.LIRFrameState;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.amd64.AMD64Call;
import giraaff.lir.amd64.AMD64Call.IndirectCallOp;
import giraaff.lir.asm.CompilationResultBuilder;

///
// A register indirect call that complies with the extra conventions for such calls in HotSpot. In
// particular, the metaspace Method of the callee must be in RBX for the case where a vtable entry's
// _from_compiled_entry is the address of an C2I adapter. Such adapters expect the target method to
// be in RBX.
///
@Opcode
// @class AMD64IndirectCallOp
final class AMD64IndirectCallOp extends IndirectCallOp
{
    // @def
    public static final LIRInstructionClass<AMD64IndirectCallOp> TYPE = LIRInstructionClass.create(AMD64IndirectCallOp.class);

    ///
    // Vtable stubs expect the metaspace Method in RBX.
    ///
    // @def
    public static final Register METHOD = AMD64.rbx;

    @Use({OperandFlag.REG})
    // @field
    protected Value ___metaspaceMethod;

    // @cons
    AMD64IndirectCallOp(ResolvedJavaMethod __targetMethod, Value __result, Value[] __parameters, Value[] __temps, Value __metaspaceMethod, Value __targetAddress, LIRFrameState __state)
    {
        super(TYPE, __targetMethod, __result, __parameters, __temps, __targetAddress, __state);
        this.___metaspaceMethod = __metaspaceMethod;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        __crb.recordMark(HotSpotRuntime.inlineInvokeMark);
        Register __callReg = ValueUtil.asRegister(this.___targetAddress);
        int __pcOffset = AMD64Call.indirectCall(__crb, __masm, __callReg, this.___callTarget, this.___state);
        __crb.recordInlineInvokeCallOp(__pcOffset);
    }
}
