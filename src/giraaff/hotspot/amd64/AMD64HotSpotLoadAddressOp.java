package giraaff.hotspot.amd64;

import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Constant;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.amd64.AMD64LIRInstruction;
import giraaff.lir.asm.CompilationResultBuilder;
import giraaff.util.GraalError;

// @class AMD64HotSpotLoadAddressOp
public final class AMD64HotSpotLoadAddressOp extends AMD64LIRInstruction
{
    public static final LIRInstructionClass<AMD64HotSpotLoadAddressOp> TYPE = LIRInstructionClass.create(AMD64HotSpotLoadAddressOp.class);

    @Def({OperandFlag.REG}) protected AllocatableValue result;
    private final Constant constant;
    private final Object note;

    // @cons
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
                masm.movl(ValueUtil.asRegister(result), masm.getPlaceholder(-1));
                break;
            case QWORD:
                masm.movq(ValueUtil.asRegister(result), masm.getPlaceholder(-1));
                break;
            default:
                throw GraalError.shouldNotReachHere("unexpected kind: " + kind);
        }
    }
}
