package giraaff.hotspot.amd64;

import jdk.vm.ci.amd64.AMD64.CPUFeature;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.meta.Value;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.asm.CompilationResultBuilder;
import giraaff.lir.gen.DiagnosticLIRGeneratorTool.ZapStackArgumentSpaceBeforeInstruction;

/**
 * Returns from a function.
 */
@Opcode
// @class AMD64HotSpotReturnOp
final class AMD64HotSpotReturnOp extends AMD64HotSpotEpilogueBlockEndOp implements ZapStackArgumentSpaceBeforeInstruction
{
    public static final LIRInstructionClass<AMD64HotSpotReturnOp> TYPE = LIRInstructionClass.create(AMD64HotSpotReturnOp.class);

    @Use({OperandFlag.REG, OperandFlag.ILLEGAL}) protected Value value;
    private final boolean isStub;
    private final Register thread;
    private final Register scratchForSafepointOnReturn;

    // @cons
    AMD64HotSpotReturnOp(Value value, boolean isStub, Register thread, Register scratchForSafepointOnReturn)
    {
        super(TYPE);
        this.value = value;
        this.isStub = isStub;
        this.thread = thread;
        this.scratchForSafepointOnReturn = scratchForSafepointOnReturn;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
    {
        leaveFrameAndRestoreRbp(crb, masm);
        if (!isStub)
        {
            // Every non-stub compile method must have a poll before the return.
            AMD64HotSpotSafepointOp.emitCode(crb, masm, true, null, thread, scratchForSafepointOnReturn);

            /*
             * We potentially return to the interpreter, and that's an AVX-SSE transition. The only
             * live value at this point should be the return value in either rax, or in xmm0 with
             * the upper half of the register unused, so we don't destroy any value here.
             */
            if (masm.supports(CPUFeature.AVX))
            {
                masm.vzeroupper();
            }
        }
        masm.ret(0);
    }
}
