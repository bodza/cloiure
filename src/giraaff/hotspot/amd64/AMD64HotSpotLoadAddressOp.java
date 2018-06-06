package giraaff.hotspot.amd64;

import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Constant;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.amd64.AMD64LIRInstruction;
import giraaff.lir.asm.CompilationResultBuilder;
import giraaff.util.GraalError;

// @class AMD64HotSpotLoadAddressOp
public final class AMD64HotSpotLoadAddressOp extends AMD64LIRInstruction
{
    // @def
    public static final LIRInstructionClass<AMD64HotSpotLoadAddressOp> TYPE = LIRInstructionClass.create(AMD64HotSpotLoadAddressOp.class);

    @LIRInstruction.Def({LIRInstruction.OperandFlag.REG})
    // @field
    protected AllocatableValue ___result;
    // @field
    private final Constant ___constant;
    // @field
    private final Object ___note;

    // @cons AMD64HotSpotLoadAddressOp
    public AMD64HotSpotLoadAddressOp(AllocatableValue __result, Constant __constant, Object __note)
    {
        super(TYPE);
        this.___result = __result;
        this.___constant = __constant;
        this.___note = __note;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        __crb.recordInlineDataInCodeWithNote(this.___constant, this.___note);
        AMD64Kind __kind = (AMD64Kind) this.___result.getPlatformKind();
        switch (__kind)
        {
            case DWORD:
            {
                __masm.movl(ValueUtil.asRegister(this.___result), __masm.getPlaceholder(-1));
                break;
            }
            case QWORD:
            {
                __masm.movq(ValueUtil.asRegister(this.___result), __masm.getPlaceholder(-1));
                break;
            }
            default:
                throw GraalError.shouldNotReachHere("unexpected kind: " + __kind);
        }
    }
}
