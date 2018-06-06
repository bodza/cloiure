package giraaff.lir.amd64;

import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.Value;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.LIROpcode;
import giraaff.lir.asm.CompilationResultBuilder;
import giraaff.util.GraalError;

@LIROpcode
// @class AMD64ByteSwapOp
public final class AMD64ByteSwapOp extends AMD64LIRInstruction
{
    // @def
    public static final LIRInstructionClass<AMD64ByteSwapOp> TYPE = LIRInstructionClass.create(AMD64ByteSwapOp.class);

    @LIRInstruction.Def({LIRInstruction.OperandFlag.REG, LIRInstruction.OperandFlag.HINT})
    // @field
    protected Value ___result;
    @LIRInstruction.Use
    // @field
    protected Value ___input;

    // @cons AMD64ByteSwapOp
    public AMD64ByteSwapOp(Value __result, Value __input)
    {
        super(TYPE);
        this.___result = __result;
        this.___input = __input;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        AMD64Move.move(__crb, __masm, this.___result, this.___input);
        switch ((AMD64Kind) this.___input.getPlatformKind())
        {
            case DWORD:
            {
                __masm.bswapl(ValueUtil.asRegister(this.___result));
                break;
            }
            case QWORD:
            {
                __masm.bswapq(ValueUtil.asRegister(this.___result));
                break;
            }
            default:
                throw GraalError.shouldNotReachHere();
        }
    }
}
