package giraaff.hotspot.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.Value;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.hotspot.GraalHotSpotVMConfig;
import giraaff.lir.LIRFrameState;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.amd64.AMD64Call;
import giraaff.lir.amd64.AMD64Call.IndirectCallOp;
import giraaff.lir.asm.CompilationResultBuilder;

/**
 * A register indirect call that complies with the extra conventions for such calls in HotSpot. In
 * particular, the metaspace Method of the callee must be in RBX for the case where a vtable entry's
 * _from_compiled_entry is the address of an C2I adapter. Such adapters expect the target method to
 * be in RBX.
 */
@Opcode
final class AMD64IndirectCallOp extends IndirectCallOp
{
    public static final LIRInstructionClass<AMD64IndirectCallOp> TYPE = LIRInstructionClass.create(AMD64IndirectCallOp.class);

    /**
     * Vtable stubs expect the metaspace Method in RBX.
     */
    public static final Register METHOD = AMD64.rbx;

    @Use({OperandFlag.REG}) protected Value metaspaceMethod;

    AMD64IndirectCallOp(ResolvedJavaMethod targetMethod, Value result, Value[] parameters, Value[] temps, Value metaspaceMethod, Value targetAddress, LIRFrameState state)
    {
        super(TYPE, targetMethod, result, parameters, temps, targetAddress, state);
        this.metaspaceMethod = metaspaceMethod;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
    {
        crb.recordMark(GraalHotSpotVMConfig.inlineInvokeMark);
        Register callReg = ValueUtil.asRegister(targetAddress);
        int pcOffset = AMD64Call.indirectCall(crb, masm, callReg, callTarget, state);
        crb.recordInlineInvokeCallOp(pcOffset);
    }
}
