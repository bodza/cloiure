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
    // @def
    public static final LIRInstructionClass<AMD64HotSpotLoadAddressOp> TYPE = LIRInstructionClass.create(AMD64HotSpotLoadAddressOp.class);

    @Def({OperandFlag.REG})
    // @field
    protected AllocatableValue result;
    // @field
    private final Constant constant;
    // @field
    private final Object note;

    // @cons
    public AMD64HotSpotLoadAddressOp(AllocatableValue __result, Constant __constant, Object __note)
    {
        super(TYPE);
        this.result = __result;
        this.constant = __constant;
        this.note = __note;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        __crb.recordInlineDataInCodeWithNote(constant, note);
        AMD64Kind __kind = (AMD64Kind) result.getPlatformKind();
        switch (__kind)
        {
            case DWORD:
                __masm.movl(ValueUtil.asRegister(result), __masm.getPlaceholder(-1));
                break;
            case QWORD:
                __masm.movq(ValueUtil.asRegister(result), __masm.getPlaceholder(-1));
                break;
            default:
                throw GraalError.shouldNotReachHere("unexpected kind: " + __kind);
        }
    }
}
