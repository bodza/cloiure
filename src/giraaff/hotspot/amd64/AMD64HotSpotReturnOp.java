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
    // @def
    public static final LIRInstructionClass<AMD64HotSpotReturnOp> TYPE = LIRInstructionClass.create(AMD64HotSpotReturnOp.class);

    @Use({OperandFlag.REG, OperandFlag.ILLEGAL})
    // @field
    protected Value value;
    // @field
    private final boolean isStub;
    // @field
    private final Register thread;
    // @field
    private final Register scratchForSafepointOnReturn;

    // @cons
    AMD64HotSpotReturnOp(Value __value, boolean __isStub, Register __thread, Register __scratchForSafepointOnReturn)
    {
        super(TYPE);
        this.value = __value;
        this.isStub = __isStub;
        this.thread = __thread;
        this.scratchForSafepointOnReturn = __scratchForSafepointOnReturn;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        leaveFrameAndRestoreRbp(__crb, __masm);
        if (!isStub)
        {
            // Every non-stub compile method must have a poll before the return.
            AMD64HotSpotSafepointOp.emitCode(__crb, __masm, true, null, thread, scratchForSafepointOnReturn);

            /*
             * We potentially return to the interpreter, and that's an AVX-SSE transition. The only
             * live value at this point should be the return value in either rax, or in xmm0 with
             * the upper half of the register unused, so we don't destroy any value here.
             */
            if (__masm.supports(CPUFeature.AVX))
            {
                __masm.vzeroupper();
            }
        }
        __masm.ret(0);
    }
}
