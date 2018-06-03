package giraaff.lir.amd64;

import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.Value;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.asm.CompilationResultBuilder;
import giraaff.util.GraalError;

@Opcode
// @class AMD64ByteSwapOp
public final class AMD64ByteSwapOp extends AMD64LIRInstruction
{
    // @def
    public static final LIRInstructionClass<AMD64ByteSwapOp> TYPE = LIRInstructionClass.create(AMD64ByteSwapOp.class);

    @Def({OperandFlag.REG, OperandFlag.HINT})
    // @field
    protected Value result;
    @Use
    // @field
    protected Value input;

    // @cons
    public AMD64ByteSwapOp(Value __result, Value __input)
    {
        super(TYPE);
        this.result = __result;
        this.input = __input;
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        AMD64Move.move(__crb, __masm, result, input);
        switch ((AMD64Kind) input.getPlatformKind())
        {
            case DWORD:
                __masm.bswapl(ValueUtil.asRegister(result));
                break;
            case QWORD:
                __masm.bswapq(ValueUtil.asRegister(result));
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
    }
}
