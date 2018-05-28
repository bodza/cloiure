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
public final class AMD64ByteSwapOp extends AMD64LIRInstruction
{
    public static final LIRInstructionClass<AMD64ByteSwapOp> TYPE = LIRInstructionClass.create(AMD64ByteSwapOp.class);

    @Def({OperandFlag.REG, OperandFlag.HINT}) protected Value result;
    @Use protected Value input;

    public AMD64ByteSwapOp(Value result, Value input)
    {
        super(TYPE);
        this.result = result;
        this.input = input;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
    {
        AMD64Move.move(crb, masm, result, input);
        switch ((AMD64Kind) input.getPlatformKind())
        {
            case DWORD:
                masm.bswapl(ValueUtil.asRegister(result));
                break;
            case QWORD:
                masm.bswapq(ValueUtil.asRegister(result));
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
    }
}
