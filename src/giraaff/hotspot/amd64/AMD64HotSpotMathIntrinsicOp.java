package giraaff.hotspot.amd64;

import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.Value;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.amd64.AMD64LIRInstruction;
import giraaff.lir.asm.CompilationResultBuilder;
import giraaff.util.GraalError;

/**
 * This provides the default implementation expected by some HotSpot based lowerings of Math
 * intrinsics. Depending on the release different patterns might be used.
 */
public final class AMD64HotSpotMathIntrinsicOp extends AMD64LIRInstruction
{
    public static final LIRInstructionClass<AMD64HotSpotMathIntrinsicOp> TYPE = LIRInstructionClass.create(AMD64HotSpotMathIntrinsicOp.class);

    public enum IntrinsicOpcode
    {
        SIN,
        COS,
        TAN,
        LOG,
        LOG10
    }

    @Opcode private final IntrinsicOpcode opcode;
    @Def protected Value result;
    @Use protected Value input;

    public AMD64HotSpotMathIntrinsicOp(IntrinsicOpcode opcode, Value result, Value input)
    {
        super(TYPE);
        this.opcode = opcode;
        this.result = result;
        this.input = input;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
    {
        switch (opcode)
        {
            case LOG:
                masm.flog(ValueUtil.asRegister(result, AMD64Kind.DOUBLE), ValueUtil.asRegister(input, AMD64Kind.DOUBLE), false);
                break;
            case LOG10:
                masm.flog(ValueUtil.asRegister(result, AMD64Kind.DOUBLE), ValueUtil.asRegister(input, AMD64Kind.DOUBLE), true);
                break;
            case SIN:
                masm.fsin(ValueUtil.asRegister(result, AMD64Kind.DOUBLE), ValueUtil.asRegister(input, AMD64Kind.DOUBLE));
                break;
            case COS:
                masm.fcos(ValueUtil.asRegister(result, AMD64Kind.DOUBLE), ValueUtil.asRegister(input, AMD64Kind.DOUBLE));
                break;
            case TAN:
                masm.ftan(ValueUtil.asRegister(result, AMD64Kind.DOUBLE), ValueUtil.asRegister(input, AMD64Kind.DOUBLE));
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
    }
}
