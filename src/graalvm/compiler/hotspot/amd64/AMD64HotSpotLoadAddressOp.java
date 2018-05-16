package graalvm.compiler.hotspot.amd64;

import static jdk.vm.ci.code.ValueUtil.asRegister;
import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Constant;

import graalvm.compiler.asm.amd64.AMD64MacroAssembler;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.lir.LIRInstructionClass;
import graalvm.compiler.lir.amd64.AMD64LIRInstruction;
import graalvm.compiler.lir.asm.CompilationResultBuilder;

public final class AMD64HotSpotLoadAddressOp extends AMD64LIRInstruction
{
    public static final LIRInstructionClass<AMD64HotSpotLoadAddressOp> TYPE = LIRInstructionClass.create(AMD64HotSpotLoadAddressOp.class);

    @Def({OperandFlag.REG}) protected AllocatableValue result;
    private final Constant constant;
    private final Object note;

    public AMD64HotSpotLoadAddressOp(AllocatableValue result, Constant constant, Object note)
    {
        super(TYPE);
        this.result = result;
        this.constant = constant;
        this.note = note;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
    {
        crb.recordInlineDataInCodeWithNote(constant, note);
        AMD64Kind kind = (AMD64Kind) result.getPlatformKind();
        switch (kind)
        {
            case DWORD:
                masm.movl(asRegister(result), masm.getPlaceholder(-1));
                break;
            case QWORD:
                masm.movq(asRegister(result), masm.getPlaceholder(-1));
                break;
            default:
                throw GraalError.shouldNotReachHere("unexpected kind: " + kind);
        }
    }
}
