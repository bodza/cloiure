package giraaff.core.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.amd64.AMD64.CPUFeature;
import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.PlatformKind;
import jdk.vm.ci.meta.VMConstant;
import jdk.vm.ci.meta.Value;
import jdk.vm.ci.meta.ValueKind;

import giraaff.asm.amd64.AMD64Assembler.AMD64BinaryArithmetic;
import giraaff.asm.amd64.AMD64Assembler.AMD64MIOp;
import giraaff.asm.amd64.AMD64Assembler.AMD64MOp;
import giraaff.asm.amd64.AMD64Assembler.AMD64MROp;
import giraaff.asm.amd64.AMD64Assembler.AMD64RMIOp;
import giraaff.asm.amd64.AMD64Assembler.AMD64RMOp;
import giraaff.asm.amd64.AMD64Assembler.AMD64RRMOp;
import giraaff.asm.amd64.AMD64Assembler.AMD64Shift;
import giraaff.asm.amd64.AMD64Assembler.AVXOp;
import giraaff.asm.amd64.AMD64Assembler.OperandSize;
import giraaff.asm.amd64.AMD64Assembler.SSEOp;
import giraaff.core.common.GraalOptions;
import giraaff.core.common.LIRKind;
import giraaff.core.common.NumUtil;
import giraaff.core.common.calc.FloatConvert;
import giraaff.debug.GraalError;
import giraaff.lir.ConstantValue;
import giraaff.lir.LIRFrameState;
import giraaff.lir.LIRValueUtil;
import giraaff.lir.Variable;
import giraaff.lir.amd64.AMD64AddressValue;
import giraaff.lir.amd64.AMD64Arithmetic;
import giraaff.lir.amd64.AMD64Arithmetic.FPDivRemOp;
import giraaff.lir.amd64.AMD64ArithmeticLIRGeneratorTool;
import giraaff.lir.amd64.AMD64Binary;
import giraaff.lir.amd64.AMD64BinaryConsumer;
import giraaff.lir.amd64.AMD64ClearRegisterOp;
import giraaff.lir.amd64.AMD64MathIntrinsicBinaryOp;
import giraaff.lir.amd64.AMD64MathIntrinsicBinaryOp.BinaryIntrinsicOpcode;
import giraaff.lir.amd64.AMD64MathIntrinsicUnaryOp;
import giraaff.lir.amd64.AMD64MathIntrinsicUnaryOp.UnaryIntrinsicOpcode;
import giraaff.lir.amd64.AMD64Move;
import giraaff.lir.amd64.AMD64MulDivOp;
import giraaff.lir.amd64.AMD64ShiftOp;
import giraaff.lir.amd64.AMD64SignExtendOp;
import giraaff.lir.amd64.AMD64Unary;
import giraaff.lir.gen.ArithmeticLIRGenerator;
import giraaff.lir.gen.LIRGenerator;

/**
 * This class implements the AMD64 specific portion of the LIR generator.
 */
public class AMD64ArithmeticLIRGenerator extends ArithmeticLIRGenerator implements AMD64ArithmeticLIRGeneratorTool
{
    private static final RegisterValue RCX_I = AMD64.rcx.asValue(LIRKind.value(AMD64Kind.DWORD));

    public AMD64ArithmeticLIRGenerator(Maths maths)
    {
        this.maths = maths == null ? new Maths() {} : maths;
    }

    private final Maths maths;

    /**
     * Interface for emitting LIR for selected {@link Math} routines. A {@code null} return value
     * for any method in this interface means the caller must emit the LIR itself.
     */
    public interface Maths
    {
        @SuppressWarnings("unused")
        default Variable emitLog(LIRGenerator gen, Value input, boolean base10)
        {
            return null;
        }

        @SuppressWarnings("unused")
        default Variable emitCos(LIRGenerator gen, Value input)
        {
            return null;
        }

        @SuppressWarnings("unused")
        default Variable emitSin(LIRGenerator gen, Value input)
        {
            return null;
        }

        @SuppressWarnings("unused")
        default Variable emitTan(LIRGenerator gen, Value input)
        {
            return null;
        }
    }

    @Override
    public Variable emitNegate(Value inputVal)
    {
        AllocatableValue input = getLIRGen().asAllocatable(inputVal);
        Variable result = getLIRGen().newVariable(LIRKind.combine(input));
        TargetDescription target = getLIRGen().target();
        boolean isAvx = ((AMD64) target.arch).getFeatures().contains(CPUFeature.AVX);
        switch ((AMD64Kind) input.getPlatformKind())
        {
            case DWORD:
                getLIRGen().append(new AMD64Unary.MOp(AMD64MOp.NEG, OperandSize.DWORD, result, input));
                break;
            case QWORD:
                getLIRGen().append(new AMD64Unary.MOp(AMD64MOp.NEG, OperandSize.QWORD, result, input));
                break;
            case SINGLE:
                if (isAvx)
                {
                    getLIRGen().append(new AMD64Binary.DataThreeOp(AVXOp.XOR, OperandSize.PS, result, input, JavaConstant.forFloat(Float.intBitsToFloat(0x80000000)), 16));
                }
                else
                {
                    getLIRGen().append(new AMD64Binary.DataTwoOp(SSEOp.XOR, OperandSize.PS, result, input, JavaConstant.forFloat(Float.intBitsToFloat(0x80000000)), 16));
                }
                break;
            case DOUBLE:
                if (isAvx)
                {
                    getLIRGen().append(new AMD64Binary.DataThreeOp(AVXOp.XOR, OperandSize.PD, result, input, JavaConstant.forDouble(Double.longBitsToDouble(0x8000000000000000L)), 16));
                }
                else
                {
                    getLIRGen().append(new AMD64Binary.DataTwoOp(SSEOp.XOR, OperandSize.PD, result, input, JavaConstant.forDouble(Double.longBitsToDouble(0x8000000000000000L)), 16));
                }
                break;
            default:
                throw GraalError.shouldNotReachHere(input.getPlatformKind().toString());
        }
        return result;
    }

    @Override
    public Variable emitNot(Value inputVal)
    {
        AllocatableValue input = getLIRGen().asAllocatable(inputVal);
        Variable result = getLIRGen().newVariable(LIRKind.combine(input));
        switch ((AMD64Kind) input.getPlatformKind())
        {
            case DWORD:
                getLIRGen().append(new AMD64Unary.MOp(AMD64MOp.NOT, OperandSize.DWORD, result, input));
                break;
            case QWORD:
                getLIRGen().append(new AMD64Unary.MOp(AMD64MOp.NOT, OperandSize.QWORD, result, input));
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
        return result;
    }

    private Variable emitBinary(LIRKind resultKind, AMD64BinaryArithmetic op, OperandSize size, boolean commutative, Value a, Value b, boolean setFlags)
    {
        if (LIRValueUtil.isJavaConstant(b))
        {
            return emitBinaryConst(resultKind, op, size, commutative, getLIRGen().asAllocatable(a), LIRValueUtil.asConstantValue(b), setFlags);
        }
        else if (commutative && LIRValueUtil.isJavaConstant(a))
        {
            return emitBinaryConst(resultKind, op, size, commutative, getLIRGen().asAllocatable(b), LIRValueUtil.asConstantValue(a), setFlags);
        }
        else
        {
            return emitBinaryVar(resultKind, op.getRMOpcode(size), size, commutative, getLIRGen().asAllocatable(a), getLIRGen().asAllocatable(b));
        }
    }

    private Variable emitBinary(LIRKind resultKind, AMD64RMOp op, OperandSize size, boolean commutative, Value a, Value b)
    {
        if (LIRValueUtil.isJavaConstant(b))
        {
            return emitBinaryConst(resultKind, op, size, getLIRGen().asAllocatable(a), LIRValueUtil.asJavaConstant(b));
        }
        else if (commutative && LIRValueUtil.isJavaConstant(a))
        {
            return emitBinaryConst(resultKind, op, size, getLIRGen().asAllocatable(b), LIRValueUtil.asJavaConstant(a));
        }
        else
        {
            return emitBinaryVar(resultKind, op, size, commutative, getLIRGen().asAllocatable(a), getLIRGen().asAllocatable(b));
        }
    }

    private Variable emitBinary(LIRKind resultKind, AMD64RRMOp op, OperandSize size, boolean commutative, Value a, Value b)
    {
        if (LIRValueUtil.isJavaConstant(b))
        {
            return emitBinaryConst(resultKind, op, size, getLIRGen().asAllocatable(a), LIRValueUtil.asJavaConstant(b));
        }
        else if (commutative && LIRValueUtil.isJavaConstant(a))
        {
            return emitBinaryConst(resultKind, op, size, getLIRGen().asAllocatable(b), LIRValueUtil.asJavaConstant(a));
        }
        else
        {
            return emitBinaryVar(resultKind, op, size, commutative, getLIRGen().asAllocatable(a), getLIRGen().asAllocatable(b));
        }
    }

    private Variable emitBinaryConst(LIRKind resultKind, AMD64BinaryArithmetic op, OperandSize size, boolean commutative, AllocatableValue a, ConstantValue b, boolean setFlags)
    {
        long value = b.getJavaConstant().asLong();
        if (NumUtil.isInt(value))
        {
            Variable result = getLIRGen().newVariable(resultKind);
            int constant = (int) value;

            if (!setFlags)
            {
                AMD64MOp mop = getMOp(op, constant);
                if (mop != null)
                {
                    getLIRGen().append(new AMD64Unary.MOp(mop, size, result, a));
                    return result;
                }
            }

            getLIRGen().append(new AMD64Binary.ConstOp(op, size, result, a, constant));
            return result;
        }
        else
        {
            return emitBinaryVar(resultKind, op.getRMOpcode(size), size, commutative, a, getLIRGen().asAllocatable(b));
        }
    }

    private static AMD64MOp getMOp(AMD64BinaryArithmetic op, int constant)
    {
        if (constant == 1)
        {
            if (op.equals(AMD64BinaryArithmetic.ADD))
            {
                return AMD64MOp.INC;
            }
            if (op.equals(AMD64BinaryArithmetic.SUB))
            {
                return AMD64MOp.DEC;
            }
        }
        else if (constant == -1)
        {
            if (op.equals(AMD64BinaryArithmetic.ADD))
            {
                return AMD64MOp.DEC;
            }
            if (op.equals(AMD64BinaryArithmetic.SUB))
            {
                return AMD64MOp.INC;
            }
        }
        return null;
    }

    private Variable emitBinaryConst(LIRKind resultKind, AMD64RMOp op, OperandSize size, AllocatableValue a, JavaConstant b)
    {
        Variable result = getLIRGen().newVariable(resultKind);
        getLIRGen().append(new AMD64Binary.DataTwoOp(op, size, result, a, b));
        return result;
    }

    private Variable emitBinaryConst(LIRKind resultKind, AMD64RRMOp op, OperandSize size, AllocatableValue a, JavaConstant b)
    {
        Variable result = getLIRGen().newVariable(resultKind);
        getLIRGen().append(new AMD64Binary.DataThreeOp(op, size, result, a, b));
        return result;
    }

    private Variable emitBinaryVar(LIRKind resultKind, AMD64RMOp op, OperandSize size, boolean commutative, AllocatableValue a, AllocatableValue b)
    {
        Variable result = getLIRGen().newVariable(resultKind);
        if (commutative)
        {
            getLIRGen().append(new AMD64Binary.CommutativeTwoOp(op, size, result, a, b));
        }
        else
        {
            getLIRGen().append(new AMD64Binary.TwoOp(op, size, result, a, b));
        }
        return result;
    }

    private Variable emitBinaryVar(LIRKind resultKind, AMD64RRMOp op, OperandSize size, boolean commutative, AllocatableValue a, AllocatableValue b)
    {
        Variable result = getLIRGen().newVariable(resultKind);
        if (commutative)
        {
            getLIRGen().append(new AMD64Binary.CommutativeThreeOp(op, size, result, a, b));
        }
        else
        {
            getLIRGen().append(new AMD64Binary.ThreeOp(op, size, result, a, b));
        }
        return result;
    }

    @Override
    protected boolean isNumericInteger(PlatformKind kind)
    {
        return ((AMD64Kind) kind).isInteger();
    }

    private Variable emitBaseOffsetLea(LIRKind resultKind, Value base, int offset, OperandSize size)
    {
        Variable result = getLIRGen().newVariable(resultKind);
        AMD64AddressValue address = new AMD64AddressValue(resultKind, getLIRGen().asAllocatable(base), offset);
        getLIRGen().append(new AMD64Move.LeaOp(result, address, size));
        return result;
    }

    @Override
    public Variable emitAdd(LIRKind resultKind, Value a, Value b, boolean setFlags)
    {
        TargetDescription target = getLIRGen().target();
        boolean isAvx = ((AMD64) target.arch).getFeatures().contains(CPUFeature.AVX);
        switch ((AMD64Kind) a.getPlatformKind())
        {
            case DWORD:
                if (LIRValueUtil.isJavaConstant(b) && !setFlags)
                {
                    long displacement = LIRValueUtil.asJavaConstant(b).asLong();
                    if (NumUtil.isInt(displacement) && displacement != 1 && displacement != -1)
                    {
                        return emitBaseOffsetLea(resultKind, a, (int) displacement, OperandSize.DWORD);
                    }
                }
                return emitBinary(resultKind, AMD64BinaryArithmetic.ADD, OperandSize.DWORD, true, a, b, setFlags);
            case QWORD:
                if (LIRValueUtil.isJavaConstant(b) && !setFlags)
                {
                    long displacement = LIRValueUtil.asJavaConstant(b).asLong();
                    if (NumUtil.isInt(displacement) && displacement != 1 && displacement != -1)
                    {
                        return emitBaseOffsetLea(resultKind, a, (int) displacement, OperandSize.QWORD);
                    }
                }
                return emitBinary(resultKind, AMD64BinaryArithmetic.ADD, OperandSize.QWORD, true, a, b, setFlags);
            case SINGLE:
                if (isAvx)
                {
                    return emitBinary(resultKind, AVXOp.ADD, OperandSize.SS, true, a, b);
                }
                else
                {
                    return emitBinary(resultKind, SSEOp.ADD, OperandSize.SS, true, a, b);
                }
            case DOUBLE:
                if (isAvx)
                {
                    return emitBinary(resultKind, AVXOp.ADD, OperandSize.SD, true, a, b);
                }
                else
                {
                    return emitBinary(resultKind, SSEOp.ADD, OperandSize.SD, true, a, b);
                }
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public Variable emitSub(LIRKind resultKind, Value a, Value b, boolean setFlags)
    {
        TargetDescription target = getLIRGen().target();
        boolean isAvx = ((AMD64) target.arch).getFeatures().contains(CPUFeature.AVX);
        switch ((AMD64Kind) a.getPlatformKind())
        {
            case DWORD:
                return emitBinary(resultKind, AMD64BinaryArithmetic.SUB, OperandSize.DWORD, false, a, b, setFlags);
            case QWORD:
                return emitBinary(resultKind, AMD64BinaryArithmetic.SUB, OperandSize.QWORD, false, a, b, setFlags);
            case SINGLE:
                if (isAvx)
                {
                    return emitBinary(resultKind, AVXOp.SUB, OperandSize.SS, false, a, b);
                }
                else
                {
                    return emitBinary(resultKind, SSEOp.SUB, OperandSize.SS, false, a, b);
                }
            case DOUBLE:
                if (isAvx)
                {
                    return emitBinary(resultKind, AVXOp.SUB, OperandSize.SD, false, a, b);
                }
                else
                {
                    return emitBinary(resultKind, SSEOp.SUB, OperandSize.SD, false, a, b);
                }
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    private Variable emitIMULConst(OperandSize size, AllocatableValue a, ConstantValue b)
    {
        long value = b.getJavaConstant().asLong();
        if (NumUtil.isInt(value))
        {
            int imm = (int) value;
            AMD64RMIOp op;
            if (NumUtil.isByte(imm))
            {
                op = AMD64RMIOp.IMUL_SX;
            }
            else
            {
                op = AMD64RMIOp.IMUL;
            }

            Variable ret = getLIRGen().newVariable(LIRKind.combine(a, b));
            getLIRGen().append(new AMD64Binary.RMIOp(op, size, ret, a, imm));
            return ret;
        }
        else
        {
            return emitBinaryVar(LIRKind.combine(a, b), AMD64RMOp.IMUL, size, true, a, getLIRGen().asAllocatable(b));
        }
    }

    private Variable emitIMUL(OperandSize size, Value a, Value b)
    {
        if (LIRValueUtil.isJavaConstant(b))
        {
            return emitIMULConst(size, getLIRGen().asAllocatable(a), LIRValueUtil.asConstantValue(b));
        }
        else if (LIRValueUtil.isJavaConstant(a))
        {
            return emitIMULConst(size, getLIRGen().asAllocatable(b), LIRValueUtil.asConstantValue(a));
        }
        else
        {
            return emitBinaryVar(LIRKind.combine(a, b), AMD64RMOp.IMUL, size, true, getLIRGen().asAllocatable(a), getLIRGen().asAllocatable(b));
        }
    }

    @Override
    public Variable emitMul(Value a, Value b, boolean setFlags)
    {
        LIRKind resultKind = LIRKind.combine(a, b);
        TargetDescription target = getLIRGen().target();
        boolean isAvx = ((AMD64) target.arch).getFeatures().contains(CPUFeature.AVX);
        switch ((AMD64Kind) a.getPlatformKind())
        {
            case DWORD:
                return emitIMUL(OperandSize.DWORD, a, b);
            case QWORD:
                return emitIMUL(OperandSize.QWORD, a, b);
            case SINGLE:
                if (isAvx)
                {
                    return emitBinary(resultKind, AVXOp.MUL, OperandSize.SS, true, a, b);
                }
                else
                {
                    return emitBinary(resultKind, SSEOp.MUL, OperandSize.SS, true, a, b);
                }
            case DOUBLE:
                if (isAvx)
                {
                    return emitBinary(resultKind, AVXOp.MUL, OperandSize.SD, true, a, b);
                }
                else
                {
                    return emitBinary(resultKind, SSEOp.MUL, OperandSize.SD, true, a, b);
                }
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    private RegisterValue moveToReg(Register reg, Value v)
    {
        RegisterValue ret = reg.asValue(v.getValueKind());
        getLIRGen().emitMove(ret, v);
        return ret;
    }

    private Value emitMulHigh(AMD64MOp opcode, OperandSize size, Value a, Value b)
    {
        AMD64MulDivOp mulHigh = getLIRGen().append(new AMD64MulDivOp(opcode, size, LIRKind.combine(a, b), moveToReg(AMD64.rax, a), getLIRGen().asAllocatable(b)));
        return getLIRGen().emitMove(mulHigh.getHighResult());
    }

    @Override
    public Value emitMulHigh(Value a, Value b)
    {
        switch ((AMD64Kind) a.getPlatformKind())
        {
            case DWORD:
                return emitMulHigh(AMD64MOp.IMUL, OperandSize.DWORD, a, b);
            case QWORD:
                return emitMulHigh(AMD64MOp.IMUL, OperandSize.QWORD, a, b);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public Value emitUMulHigh(Value a, Value b)
    {
        switch ((AMD64Kind) a.getPlatformKind())
        {
            case DWORD:
                return emitMulHigh(AMD64MOp.MUL, OperandSize.DWORD, a, b);
            case QWORD:
                return emitMulHigh(AMD64MOp.MUL, OperandSize.QWORD, a, b);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    public Value emitBinaryMemory(AMD64RMOp op, OperandSize size, AllocatableValue a, AMD64AddressValue location, LIRFrameState state)
    {
        Variable result = getLIRGen().newVariable(LIRKind.combine(a));
        getLIRGen().append(new AMD64Binary.MemoryTwoOp(op, size, result, a, location, state));
        return result;
    }

    public Value emitBinaryMemory(AMD64RRMOp op, OperandSize size, AllocatableValue a, AMD64AddressValue location, LIRFrameState state)
    {
        Variable result = getLIRGen().newVariable(LIRKind.combine(a));
        getLIRGen().append(new AMD64Binary.MemoryThreeOp(op, size, result, a, location, state));
        return result;
    }

    protected Value emitConvertMemoryOp(PlatformKind kind, AMD64RMOp op, OperandSize size, AMD64AddressValue address, LIRFrameState state)
    {
        Variable result = getLIRGen().newVariable(LIRKind.value(kind));
        getLIRGen().append(new AMD64Unary.MemoryOp(op, size, result, address, state));
        return result;
    }

    protected Value emitZeroExtendMemory(AMD64Kind memoryKind, int resultBits, AMD64AddressValue address, LIRFrameState state)
    {
        // Issue a zero extending load of the proper bit size and set the result to
        // the proper kind.
        Variable result = getLIRGen().newVariable(LIRKind.value(resultBits <= 32 ? AMD64Kind.DWORD : AMD64Kind.QWORD));
        switch (memoryKind)
        {
            case BYTE:
                getLIRGen().append(new AMD64Unary.MemoryOp(AMD64RMOp.MOVZXB, OperandSize.DWORD, result, address, state));
                break;
            case WORD:
                getLIRGen().append(new AMD64Unary.MemoryOp(AMD64RMOp.MOVZX, OperandSize.DWORD, result, address, state));
                break;
            case DWORD:
                getLIRGen().append(new AMD64Unary.MemoryOp(AMD64RMOp.MOV, OperandSize.DWORD, result, address, state));
                break;
            case QWORD:
                getLIRGen().append(new AMD64Unary.MemoryOp(AMD64RMOp.MOV, OperandSize.QWORD, result, address, state));
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
        return result;
    }

    private AMD64MulDivOp emitIDIV(OperandSize size, Value a, Value b, LIRFrameState state)
    {
        LIRKind kind = LIRKind.combine(a, b);

        AMD64SignExtendOp sx = getLIRGen().append(new AMD64SignExtendOp(size, kind, moveToReg(AMD64.rax, a)));
        return getLIRGen().append(new AMD64MulDivOp(AMD64MOp.IDIV, size, kind, sx.getHighResult(), sx.getLowResult(), getLIRGen().asAllocatable(b), state));
    }

    private AMD64MulDivOp emitDIV(OperandSize size, Value a, Value b, LIRFrameState state)
    {
        LIRKind kind = LIRKind.combine(a, b);

        RegisterValue rax = moveToReg(AMD64.rax, a);
        RegisterValue rdx = AMD64.rdx.asValue(kind);
        getLIRGen().append(new AMD64ClearRegisterOp(size, rdx));
        return getLIRGen().append(new AMD64MulDivOp(AMD64MOp.DIV, size, kind, rdx, rax, getLIRGen().asAllocatable(b), state));
    }

    public Value[] emitSignedDivRem(Value a, Value b, LIRFrameState state)
    {
        AMD64MulDivOp op;
        switch ((AMD64Kind) a.getPlatformKind())
        {
            case DWORD:
                op = emitIDIV(OperandSize.DWORD, a, b, state);
                break;
            case QWORD:
                op = emitIDIV(OperandSize.QWORD, a, b, state);
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
        return new Value[] { getLIRGen().emitMove(op.getQuotient()), getLIRGen().emitMove(op.getRemainder()) };
    }

    public Value[] emitUnsignedDivRem(Value a, Value b, LIRFrameState state)
    {
        AMD64MulDivOp op;
        switch ((AMD64Kind) a.getPlatformKind())
        {
            case DWORD:
                op = emitDIV(OperandSize.DWORD, a, b, state);
                break;
            case QWORD:
                op = emitDIV(OperandSize.QWORD, a, b, state);
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
        return new Value[] { getLIRGen().emitMove(op.getQuotient()), getLIRGen().emitMove(op.getRemainder()) };
    }

    @Override
    public Value emitDiv(Value a, Value b, LIRFrameState state)
    {
        TargetDescription target = getLIRGen().target();
        boolean isAvx = ((AMD64) target.arch).getFeatures().contains(CPUFeature.AVX);
        LIRKind resultKind = LIRKind.combine(a, b);
        switch ((AMD64Kind) a.getPlatformKind())
        {
            case DWORD:
                AMD64MulDivOp op = emitIDIV(OperandSize.DWORD, a, b, state);
                return getLIRGen().emitMove(op.getQuotient());
            case QWORD:
                AMD64MulDivOp lop = emitIDIV(OperandSize.QWORD, a, b, state);
                return getLIRGen().emitMove(lop.getQuotient());
            case SINGLE:
                if (isAvx)
                {
                    return emitBinary(resultKind, AVXOp.DIV, OperandSize.SS, false, a, b);
                }
                else
                {
                    return emitBinary(resultKind, SSEOp.DIV, OperandSize.SS, false, a, b);
                }
            case DOUBLE:
                if (isAvx)
                {
                    return emitBinary(resultKind, AVXOp.DIV, OperandSize.SD, false, a, b);
                }
                else
                {
                    return emitBinary(resultKind, SSEOp.DIV, OperandSize.SD, false, a, b);
                }
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public Value emitRem(Value a, Value b, LIRFrameState state)
    {
        switch ((AMD64Kind) a.getPlatformKind())
        {
            case DWORD:
                AMD64MulDivOp op = emitIDIV(OperandSize.DWORD, a, b, state);
                return getLIRGen().emitMove(op.getRemainder());
            case QWORD:
                AMD64MulDivOp lop = emitIDIV(OperandSize.QWORD, a, b, state);
                return getLIRGen().emitMove(lop.getRemainder());
            case SINGLE:
            {
                Variable result = getLIRGen().newVariable(LIRKind.combine(a, b));
                getLIRGen().append(new FPDivRemOp(AMD64Arithmetic.FREM, result, getLIRGen().load(a), getLIRGen().load(b)));
                return result;
            }
            case DOUBLE:
            {
                Variable result = getLIRGen().newVariable(LIRKind.combine(a, b));
                getLIRGen().append(new FPDivRemOp(AMD64Arithmetic.DREM, result, getLIRGen().load(a), getLIRGen().load(b)));
                return result;
            }
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public Variable emitUDiv(Value a, Value b, LIRFrameState state)
    {
        AMD64MulDivOp op;
        switch ((AMD64Kind) a.getPlatformKind())
        {
            case DWORD:
                op = emitDIV(OperandSize.DWORD, a, b, state);
                break;
            case QWORD:
                op = emitDIV(OperandSize.QWORD, a, b, state);
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
        return getLIRGen().emitMove(op.getQuotient());
    }

    @Override
    public Variable emitURem(Value a, Value b, LIRFrameState state)
    {
        AMD64MulDivOp op;
        switch ((AMD64Kind) a.getPlatformKind())
        {
            case DWORD:
                op = emitDIV(OperandSize.DWORD, a, b, state);
                break;
            case QWORD:
                op = emitDIV(OperandSize.QWORD, a, b, state);
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
        return getLIRGen().emitMove(op.getRemainder());
    }

    @Override
    public Variable emitAnd(Value a, Value b)
    {
        LIRKind resultKind = LIRKind.combine(a, b);
        TargetDescription target = getLIRGen().target();
        boolean isAvx = ((AMD64) target.arch).getFeatures().contains(CPUFeature.AVX);
        switch ((AMD64Kind) a.getPlatformKind())
        {
            case DWORD:
                return emitBinary(resultKind, AMD64BinaryArithmetic.AND, OperandSize.DWORD, true, a, b, false);
            case QWORD:
                return emitBinary(resultKind, AMD64BinaryArithmetic.AND, OperandSize.QWORD, true, a, b, false);
            case SINGLE:
                if (isAvx)
                {
                    return emitBinary(resultKind, AVXOp.AND, OperandSize.PS, true, a, b);
                }
                else
                {
                    return emitBinary(resultKind, SSEOp.AND, OperandSize.PS, true, a, b);
                }
            case DOUBLE:
                if (isAvx)
                {
                    return emitBinary(resultKind, AVXOp.AND, OperandSize.PD, true, a, b);
                }
                else
                {
                    return emitBinary(resultKind, SSEOp.AND, OperandSize.PD, true, a, b);
                }
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public Variable emitOr(Value a, Value b)
    {
        LIRKind resultKind = LIRKind.combine(a, b);
        TargetDescription target = getLIRGen().target();
        boolean isAvx = ((AMD64) target.arch).getFeatures().contains(CPUFeature.AVX);
        switch ((AMD64Kind) a.getPlatformKind())
        {
            case DWORD:
                return emitBinary(resultKind, AMD64BinaryArithmetic.OR, OperandSize.DWORD, true, a, b, false);
            case QWORD:
                return emitBinary(resultKind, AMD64BinaryArithmetic.OR, OperandSize.QWORD, true, a, b, false);
            case SINGLE:
                if (isAvx)
                {
                    return emitBinary(resultKind, AVXOp.OR, OperandSize.PS, true, a, b);
                }
                else
                {
                    return emitBinary(resultKind, SSEOp.OR, OperandSize.PS, true, a, b);
                }
            case DOUBLE:
                if (isAvx)
                {
                    return emitBinary(resultKind, AVXOp.OR, OperandSize.PD, true, a, b);
                }
                else
                {
                    return emitBinary(resultKind, SSEOp.OR, OperandSize.PD, true, a, b);
                }
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public Variable emitXor(Value a, Value b)
    {
        LIRKind resultKind = LIRKind.combine(a, b);
        TargetDescription target = getLIRGen().target();
        boolean isAvx = ((AMD64) target.arch).getFeatures().contains(CPUFeature.AVX);
        switch ((AMD64Kind) a.getPlatformKind())
        {
            case DWORD:
                return emitBinary(resultKind, AMD64BinaryArithmetic.XOR, OperandSize.DWORD, true, a, b, false);
            case QWORD:
                return emitBinary(resultKind, AMD64BinaryArithmetic.XOR, OperandSize.QWORD, true, a, b, false);
            case SINGLE:
                if (isAvx)
                {
                    return emitBinary(resultKind, AVXOp.XOR, OperandSize.PS, true, a, b);
                }
                else
                {
                    return emitBinary(resultKind, SSEOp.XOR, OperandSize.PS, true, a, b);
                }
            case DOUBLE:
                if (isAvx)
                {
                    return emitBinary(resultKind, AVXOp.XOR, OperandSize.PD, true, a, b);
                }
                else
                {
                    return emitBinary(resultKind, SSEOp.XOR, OperandSize.PD, true, a, b);
                }
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    private Variable emitShift(AMD64Shift op, OperandSize size, Value a, Value b)
    {
        Variable result = getLIRGen().newVariable(LIRKind.combine(a, b).changeType(a.getPlatformKind()));
        AllocatableValue input = getLIRGen().asAllocatable(a);
        if (LIRValueUtil.isJavaConstant(b))
        {
            JavaConstant c = LIRValueUtil.asJavaConstant(b);
            if (c.asLong() == 1)
            {
                getLIRGen().append(new AMD64Unary.MOp(op.m1Op, size, result, input));
            }
            else
            {
                /*
                 * c is implicitly masked to 5 or 6 bits by the CPU, so casting it to (int) is
                 * always correct, even without the NumUtil.is32bit() test.
                 */
                getLIRGen().append(new AMD64Binary.ConstOp(op.miOp, size, result, input, (int) c.asLong()));
            }
        }
        else
        {
            getLIRGen().emitMove(RCX_I, b);
            getLIRGen().append(new AMD64ShiftOp(op.mcOp, size, result, input, RCX_I));
        }
        return result;
    }

    @Override
    public Variable emitShl(Value a, Value b)
    {
        switch ((AMD64Kind) a.getPlatformKind())
        {
            case DWORD:
                return emitShift(AMD64Shift.SHL, OperandSize.DWORD, a, b);
            case QWORD:
                return emitShift(AMD64Shift.SHL, OperandSize.QWORD, a, b);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public Variable emitShr(Value a, Value b)
    {
        switch ((AMD64Kind) a.getPlatformKind())
        {
            case DWORD:
                return emitShift(AMD64Shift.SAR, OperandSize.DWORD, a, b);
            case QWORD:
                return emitShift(AMD64Shift.SAR, OperandSize.QWORD, a, b);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public Variable emitUShr(Value a, Value b)
    {
        switch ((AMD64Kind) a.getPlatformKind())
        {
            case DWORD:
                return emitShift(AMD64Shift.SHR, OperandSize.DWORD, a, b);
            case QWORD:
                return emitShift(AMD64Shift.SHR, OperandSize.QWORD, a, b);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    public Variable emitRol(Value a, Value b)
    {
        switch ((AMD64Kind) a.getPlatformKind())
        {
            case DWORD:
                return emitShift(AMD64Shift.ROL, OperandSize.DWORD, a, b);
            case QWORD:
                return emitShift(AMD64Shift.ROL, OperandSize.QWORD, a, b);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    public Variable emitRor(Value a, Value b)
    {
        switch ((AMD64Kind) a.getPlatformKind())
        {
            case DWORD:
                return emitShift(AMD64Shift.ROR, OperandSize.DWORD, a, b);
            case QWORD:
                return emitShift(AMD64Shift.ROR, OperandSize.QWORD, a, b);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    private AllocatableValue emitConvertOp(LIRKind kind, AMD64RMOp op, OperandSize size, Value input)
    {
        Variable result = getLIRGen().newVariable(kind);
        getLIRGen().append(new AMD64Unary.RMOp(op, size, result, getLIRGen().asAllocatable(input)));
        return result;
    }

    private AllocatableValue emitConvertOp(LIRKind kind, AMD64MROp op, OperandSize size, Value input)
    {
        Variable result = getLIRGen().newVariable(kind);
        getLIRGen().append(new AMD64Unary.MROp(op, size, result, getLIRGen().asAllocatable(input)));
        return result;
    }

    @Override
    public Value emitReinterpret(LIRKind to, Value inputVal)
    {
        ValueKind<?> from = inputVal.getValueKind();
        if (to.equals(from))
        {
            return inputVal;
        }

        AllocatableValue input = getLIRGen().asAllocatable(inputVal);
        /*
         * Conversions between integer to floating point types require moves between CPU and FPU
         * registers.
         */
        AMD64Kind fromKind = (AMD64Kind) from.getPlatformKind();
        switch ((AMD64Kind) to.getPlatformKind())
        {
            case DWORD:
                switch (fromKind)
                {
                    case SINGLE:
                        return emitConvertOp(to, AMD64MROp.MOVD, OperandSize.DWORD, input);
                }
                break;
            case QWORD:
                switch (fromKind)
                {
                    case DOUBLE:
                        return emitConvertOp(to, AMD64MROp.MOVQ, OperandSize.QWORD, input);
                }
                break;
            case SINGLE:
                switch (fromKind)
                {
                    case DWORD:
                        return emitConvertOp(to, AMD64RMOp.MOVD, OperandSize.DWORD, input);
                }
                break;
            case DOUBLE:
                switch (fromKind)
                {
                    case QWORD:
                        return emitConvertOp(to, AMD64RMOp.MOVQ, OperandSize.QWORD, input);
                }
                break;
        }
        throw GraalError.shouldNotReachHere();
    }

    @Override
    public Value emitFloatConvert(FloatConvert op, Value input)
    {
        switch (op)
        {
            case D2F:
                return emitConvertOp(LIRKind.combine(input).changeType(AMD64Kind.SINGLE), SSEOp.CVTSD2SS, OperandSize.SD, input);
            case D2I:
                return emitConvertOp(LIRKind.combine(input).changeType(AMD64Kind.DWORD), SSEOp.CVTTSD2SI, OperandSize.DWORD, input);
            case D2L:
                return emitConvertOp(LIRKind.combine(input).changeType(AMD64Kind.QWORD), SSEOp.CVTTSD2SI, OperandSize.QWORD, input);
            case F2D:
                return emitConvertOp(LIRKind.combine(input).changeType(AMD64Kind.DOUBLE), SSEOp.CVTSS2SD, OperandSize.SS, input);
            case F2I:
                return emitConvertOp(LIRKind.combine(input).changeType(AMD64Kind.DWORD), SSEOp.CVTTSS2SI, OperandSize.DWORD, input);
            case F2L:
                return emitConvertOp(LIRKind.combine(input).changeType(AMD64Kind.QWORD), SSEOp.CVTTSS2SI, OperandSize.QWORD, input);
            case I2D:
                return emitConvertOp(LIRKind.combine(input).changeType(AMD64Kind.DOUBLE), SSEOp.CVTSI2SD, OperandSize.DWORD, input);
            case I2F:
                return emitConvertOp(LIRKind.combine(input).changeType(AMD64Kind.SINGLE), SSEOp.CVTSI2SS, OperandSize.DWORD, input);
            case L2D:
                return emitConvertOp(LIRKind.combine(input).changeType(AMD64Kind.DOUBLE), SSEOp.CVTSI2SD, OperandSize.QWORD, input);
            case L2F:
                return emitConvertOp(LIRKind.combine(input).changeType(AMD64Kind.SINGLE), SSEOp.CVTSI2SS, OperandSize.QWORD, input);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public Value emitNarrow(Value inputVal, int bits)
    {
        if (inputVal.getPlatformKind() == AMD64Kind.QWORD && bits <= 32)
        {
            // TODO make it possible to reinterpret Long as Int in LIR without move
            return emitConvertOp(LIRKind.combine(inputVal).changeType(AMD64Kind.DWORD), AMD64RMOp.MOV, OperandSize.DWORD, inputVal);
        }
        else
        {
            return inputVal;
        }
    }

    @Override
    public Value emitSignExtend(Value inputVal, int fromBits, int toBits)
    {
        if (fromBits == toBits)
        {
            return inputVal;
        }
        else if (toBits > 32)
        {
            // sign extend to 64 bits
            switch (fromBits)
            {
                case 8:
                    return emitConvertOp(LIRKind.combine(inputVal).changeType(AMD64Kind.QWORD), AMD64RMOp.MOVSXB, OperandSize.QWORD, inputVal);
                case 16:
                    return emitConvertOp(LIRKind.combine(inputVal).changeType(AMD64Kind.QWORD), AMD64RMOp.MOVSX, OperandSize.QWORD, inputVal);
                case 32:
                    return emitConvertOp(LIRKind.combine(inputVal).changeType(AMD64Kind.QWORD), AMD64RMOp.MOVSXD, OperandSize.QWORD, inputVal);
                default:
                    throw GraalError.unimplemented("unsupported sign extension (" + fromBits + " bit -> " + toBits + " bit)");
            }
        }
        else
        {
            // sign extend to 32 bits (smaller values are internally represented as 32 bit values)
            switch (fromBits)
            {
                case 8:
                    return emitConvertOp(LIRKind.combine(inputVal).changeType(AMD64Kind.DWORD), AMD64RMOp.MOVSXB, OperandSize.DWORD, inputVal);
                case 16:
                    return emitConvertOp(LIRKind.combine(inputVal).changeType(AMD64Kind.DWORD), AMD64RMOp.MOVSX, OperandSize.DWORD, inputVal);
                case 32:
                    return inputVal;
                default:
                    throw GraalError.unimplemented("unsupported sign extension (" + fromBits + " bit -> " + toBits + " bit)");
            }
        }
    }

    @Override
    public Value emitZeroExtend(Value inputVal, int fromBits, int toBits)
    {
        if (fromBits == toBits)
        {
            return inputVal;
        }
        else if (fromBits > 32)
        {
            Variable result = getLIRGen().newVariable(LIRKind.combine(inputVal));
            long mask = CodeUtil.mask(fromBits);
            getLIRGen().append(new AMD64Binary.DataTwoOp(AMD64BinaryArithmetic.AND.getRMOpcode(OperandSize.QWORD), OperandSize.QWORD, result, getLIRGen().asAllocatable(inputVal), JavaConstant.forLong(mask)));
            return result;
        }
        else
        {
            LIRKind resultKind = LIRKind.combine(inputVal);
            if (toBits > 32)
            {
                resultKind = resultKind.changeType(AMD64Kind.QWORD);
            }
            else
            {
                resultKind = resultKind.changeType(AMD64Kind.DWORD);
            }

            /*
             * Always emit DWORD operations, even if the resultKind is Long. On AMD64, all DWORD
             * operations implicitly set the upper half of the register to 0, which is what we want
             * anyway. Compared to the QWORD operations, the encoding of the DWORD operations is
             * sometimes one byte shorter.
             */
            switch (fromBits)
            {
                case 8:
                    return emitConvertOp(resultKind, AMD64RMOp.MOVZXB, OperandSize.DWORD, inputVal);
                case 16:
                    return emitConvertOp(resultKind, AMD64RMOp.MOVZX, OperandSize.DWORD, inputVal);
                case 32:
                    return emitConvertOp(resultKind, AMD64RMOp.MOV, OperandSize.DWORD, inputVal);
            }

            // odd bit count, fall back on manual masking
            Variable result = getLIRGen().newVariable(resultKind);
            JavaConstant mask;
            if (toBits > 32)
            {
                mask = JavaConstant.forLong(CodeUtil.mask(fromBits));
            }
            else
            {
                mask = JavaConstant.forInt((int) CodeUtil.mask(fromBits));
            }
            getLIRGen().append(new AMD64Binary.DataTwoOp(AMD64BinaryArithmetic.AND.getRMOpcode(OperandSize.DWORD), OperandSize.DWORD, result, getLIRGen().asAllocatable(inputVal), mask));
            return result;
        }
    }

    @Override
    public Variable emitBitCount(Value value)
    {
        Variable result = getLIRGen().newVariable(LIRKind.combine(value).changeType(AMD64Kind.DWORD));
        if (value.getPlatformKind() == AMD64Kind.QWORD)
        {
            getLIRGen().append(new AMD64Unary.RMOp(AMD64RMOp.POPCNT, OperandSize.QWORD, result, getLIRGen().asAllocatable(value)));
        }
        else
        {
            getLIRGen().append(new AMD64Unary.RMOp(AMD64RMOp.POPCNT, OperandSize.DWORD, result, getLIRGen().asAllocatable(value)));
        }
        return result;
    }

    @Override
    public Variable emitBitScanForward(Value value)
    {
        Variable result = getLIRGen().newVariable(LIRKind.combine(value).changeType(AMD64Kind.DWORD));
        getLIRGen().append(new AMD64Unary.RMOp(AMD64RMOp.BSF, OperandSize.QWORD, result, getLIRGen().asAllocatable(value)));
        return result;
    }

    @Override
    public Variable emitBitScanReverse(Value value)
    {
        Variable result = getLIRGen().newVariable(LIRKind.combine(value).changeType(AMD64Kind.DWORD));
        if (value.getPlatformKind() == AMD64Kind.QWORD)
        {
            getLIRGen().append(new AMD64Unary.RMOp(AMD64RMOp.BSR, OperandSize.QWORD, result, getLIRGen().asAllocatable(value)));
        }
        else
        {
            getLIRGen().append(new AMD64Unary.RMOp(AMD64RMOp.BSR, OperandSize.DWORD, result, getLIRGen().asAllocatable(value)));
        }
        return result;
    }

    @Override
    public Value emitCountLeadingZeros(Value value)
    {
        Variable result = getLIRGen().newVariable(LIRKind.combine(value).changeType(AMD64Kind.DWORD));
        if (value.getPlatformKind() == AMD64Kind.QWORD)
        {
            getLIRGen().append(new AMD64Unary.RMOp(AMD64RMOp.LZCNT, OperandSize.QWORD, result, getLIRGen().asAllocatable(value)));
        }
        else
        {
            getLIRGen().append(new AMD64Unary.RMOp(AMD64RMOp.LZCNT, OperandSize.DWORD, result, getLIRGen().asAllocatable(value)));
        }
        return result;
    }

    @Override
    public Value emitCountTrailingZeros(Value value)
    {
        Variable result = getLIRGen().newVariable(LIRKind.combine(value).changeType(AMD64Kind.DWORD));
        if (value.getPlatformKind() == AMD64Kind.QWORD)
        {
            getLIRGen().append(new AMD64Unary.RMOp(AMD64RMOp.TZCNT, OperandSize.QWORD, result, getLIRGen().asAllocatable(value)));
        }
        else
        {
            getLIRGen().append(new AMD64Unary.RMOp(AMD64RMOp.TZCNT, OperandSize.DWORD, result, getLIRGen().asAllocatable(value)));
        }
        return result;
    }

    @Override
    public Value emitMathAbs(Value input)
    {
        Variable result = getLIRGen().newVariable(LIRKind.combine(input));
        switch ((AMD64Kind) input.getPlatformKind())
        {
            case SINGLE:
                getLIRGen().append(new AMD64Binary.DataTwoOp(SSEOp.AND, OperandSize.PS, result, getLIRGen().asAllocatable(input), JavaConstant.forFloat(Float.intBitsToFloat(0x7FFFFFFF)), 16));
                break;
            case DOUBLE:
                getLIRGen().append(new AMD64Binary.DataTwoOp(SSEOp.AND, OperandSize.PD, result, getLIRGen().asAllocatable(input), JavaConstant.forDouble(Double.longBitsToDouble(0x7FFFFFFFFFFFFFFFL)), 16));
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
        return result;
    }

    @Override
    public Value emitMathSqrt(Value input)
    {
        Variable result = getLIRGen().newVariable(LIRKind.combine(input));
        switch ((AMD64Kind) input.getPlatformKind())
        {
            case SINGLE:
                getLIRGen().append(new AMD64Unary.RMOp(SSEOp.SQRT, OperandSize.SS, result, getLIRGen().asAllocatable(input)));
                break;
            case DOUBLE:
                getLIRGen().append(new AMD64Unary.RMOp(SSEOp.SQRT, OperandSize.SD, result, getLIRGen().asAllocatable(input)));
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
        return result;
    }

    @Override
    public Value emitMathLog(Value input, boolean base10)
    {
        LIRGenerator gen = getLIRGen();
        Variable result = maths.emitLog(gen, input, base10);
        if (result == null)
        {
            result = gen.newVariable(LIRKind.combine(input));
            AllocatableValue stackSlot = gen.getResult().getFrameMapBuilder().allocateSpillSlot(LIRKind.value(AMD64Kind.QWORD));
            gen.append(new AMD64MathIntrinsicUnaryOp(getAMD64LIRGen(), base10 ? UnaryIntrinsicOpcode.LOG10 : UnaryIntrinsicOpcode.LOG, result, gen.asAllocatable(input), stackSlot));
        }
        return result;
    }

    @Override
    public Value emitMathCos(Value input)
    {
        LIRGenerator gen = getLIRGen();
        Variable result = maths.emitCos(gen, input);
        if (result == null)
        {
            result = gen.newVariable(LIRKind.combine(input));
            AllocatableValue stackSlot = gen.getResult().getFrameMapBuilder().allocateSpillSlot(LIRKind.value(AMD64Kind.QWORD));
            gen.append(new AMD64MathIntrinsicUnaryOp(getAMD64LIRGen(), UnaryIntrinsicOpcode.COS, result, gen.asAllocatable(input), stackSlot));
        }
        return result;
    }

    @Override
    public Value emitMathSin(Value input)
    {
        LIRGenerator gen = getLIRGen();
        Variable result = maths.emitSin(gen, input);
        if (result == null)
        {
            result = gen.newVariable(LIRKind.combine(input));
            AllocatableValue stackSlot = gen.getResult().getFrameMapBuilder().allocateSpillSlot(LIRKind.value(AMD64Kind.QWORD));
            gen.append(new AMD64MathIntrinsicUnaryOp(getAMD64LIRGen(), UnaryIntrinsicOpcode.SIN, result, gen.asAllocatable(input), stackSlot));
        }
        return result;
    }

    @Override
    public Value emitMathTan(Value input)
    {
        LIRGenerator gen = getLIRGen();
        Variable result = maths.emitTan(gen, input);
        if (result == null)
        {
            result = gen.newVariable(LIRKind.combine(input));
            AllocatableValue stackSlot = gen.getResult().getFrameMapBuilder().allocateSpillSlot(LIRKind.value(AMD64Kind.QWORD));
            gen.append(new AMD64MathIntrinsicUnaryOp(getAMD64LIRGen(), UnaryIntrinsicOpcode.TAN, result, gen.asAllocatable(input), stackSlot));
        }
        return result;
    }

    @Override
    public Value emitMathExp(Value input)
    {
        Variable result = getLIRGen().newVariable(LIRKind.combine(input));
        AllocatableValue stackSlot = getLIRGen().getResult().getFrameMapBuilder().allocateSpillSlot(LIRKind.value(AMD64Kind.QWORD));
        getLIRGen().append(new AMD64MathIntrinsicUnaryOp(getAMD64LIRGen(), UnaryIntrinsicOpcode.EXP, result, getLIRGen().asAllocatable(input), stackSlot));
        return result;
    }

    @Override
    public Value emitMathPow(Value input1, Value input2)
    {
        Variable result = getLIRGen().newVariable(LIRKind.combine(input1));
        getLIRGen().append(new AMD64MathIntrinsicBinaryOp(getAMD64LIRGen(), BinaryIntrinsicOpcode.POW, result, getLIRGen().asAllocatable(input1), getLIRGen().asAllocatable(input2)));
        return result;
    }

    protected AMD64LIRGenerator getAMD64LIRGen()
    {
        return (AMD64LIRGenerator) getLIRGen();
    }

    @Override
    public Variable emitLoad(LIRKind kind, Value address, LIRFrameState state)
    {
        AMD64AddressValue loadAddress = getAMD64LIRGen().asAddressValue(address);
        Variable result = getLIRGen().newVariable(getLIRGen().toRegisterKind(kind));
        switch ((AMD64Kind) kind.getPlatformKind())
        {
            case BYTE:
                getLIRGen().append(new AMD64Unary.MemoryOp(AMD64RMOp.MOVSXB, OperandSize.DWORD, result, loadAddress, state));
                break;
            case WORD:
                getLIRGen().append(new AMD64Unary.MemoryOp(AMD64RMOp.MOVSX, OperandSize.DWORD, result, loadAddress, state));
                break;
            case DWORD:
                getLIRGen().append(new AMD64Unary.MemoryOp(AMD64RMOp.MOV, OperandSize.DWORD, result, loadAddress, state));
                break;
            case QWORD:
                getLIRGen().append(new AMD64Unary.MemoryOp(AMD64RMOp.MOV, OperandSize.QWORD, result, loadAddress, state));
                break;
            case SINGLE:
                getLIRGen().append(new AMD64Unary.MemoryOp(AMD64RMOp.MOVSS, OperandSize.SS, result, loadAddress, state));
                break;
            case DOUBLE:
                getLIRGen().append(new AMD64Unary.MemoryOp(AMD64RMOp.MOVSD, OperandSize.SD, result, loadAddress, state));
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
        return result;
    }

    protected void emitStoreConst(AMD64Kind kind, AMD64AddressValue address, ConstantValue value, LIRFrameState state)
    {
        Constant c = value.getConstant();
        if (JavaConstant.isNull(c))
        {
            OperandSize size = kind == AMD64Kind.DWORD ? OperandSize.DWORD : OperandSize.QWORD;
            getLIRGen().append(new AMD64BinaryConsumer.MemoryConstOp(AMD64MIOp.MOV, size, address, 0, state));
            return;
        }
        else if (c instanceof VMConstant)
        {
            // only 32-bit constants can be patched
            if (kind == AMD64Kind.DWORD)
            {
                if (getLIRGen().target().inlineObjects || !(c instanceof JavaConstant))
                {
                    // if c is a JavaConstant, it's an oop, otherwise it's a metaspace constant
                    getLIRGen().append(new AMD64BinaryConsumer.MemoryVMConstOp(AMD64MIOp.MOV, address, (VMConstant) c, state));
                    return;
                }
            }
        }
        else
        {
            JavaConstant jc = (JavaConstant) c;

            AMD64MIOp op = AMD64MIOp.MOV;
            OperandSize size;
            long imm;

            switch (kind)
            {
                case BYTE:
                    op = AMD64MIOp.MOVB;
                    size = OperandSize.BYTE;
                    imm = jc.asInt();
                    break;
                case WORD:
                    size = OperandSize.WORD;
                    imm = jc.asInt();
                    break;
                case DWORD:
                    size = OperandSize.DWORD;
                    imm = jc.asInt();
                    break;
                case QWORD:
                    size = OperandSize.QWORD;
                    imm = jc.asLong();
                    break;
                case SINGLE:
                    size = OperandSize.DWORD;
                    imm = Float.floatToRawIntBits(jc.asFloat());
                    break;
                case DOUBLE:
                    size = OperandSize.QWORD;
                    imm = Double.doubleToRawLongBits(jc.asDouble());
                    break;
                default:
                    throw GraalError.shouldNotReachHere("unexpected kind " + kind);
            }

            if (NumUtil.isInt(imm))
            {
                getLIRGen().append(new AMD64BinaryConsumer.MemoryConstOp(op, size, address, (int) imm, state));
                return;
            }
        }

        // fallback: load, then store
        emitStore(kind, address, getLIRGen().asAllocatable(value), state);
    }

    protected void emitStore(AMD64Kind kind, AMD64AddressValue address, AllocatableValue value, LIRFrameState state)
    {
        switch (kind)
        {
            case BYTE:
                getLIRGen().append(new AMD64BinaryConsumer.MemoryMROp(AMD64MROp.MOVB, OperandSize.BYTE, address, value, state));
                break;
            case WORD:
                getLIRGen().append(new AMD64BinaryConsumer.MemoryMROp(AMD64MROp.MOV, OperandSize.WORD, address, value, state));
                break;
            case DWORD:
                getLIRGen().append(new AMD64BinaryConsumer.MemoryMROp(AMD64MROp.MOV, OperandSize.DWORD, address, value, state));
                break;
            case QWORD:
                getLIRGen().append(new AMD64BinaryConsumer.MemoryMROp(AMD64MROp.MOV, OperandSize.QWORD, address, value, state));
                break;
            case SINGLE:
                getLIRGen().append(new AMD64BinaryConsumer.MemoryMROp(AMD64MROp.MOVSS, OperandSize.SS, address, value, state));
                break;
            case DOUBLE:
                getLIRGen().append(new AMD64BinaryConsumer.MemoryMROp(AMD64MROp.MOVSD, OperandSize.SD, address, value, state));
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public void emitStore(ValueKind<?> lirKind, Value address, Value input, LIRFrameState state)
    {
        AMD64AddressValue storeAddress = getAMD64LIRGen().asAddressValue(address);
        AMD64Kind kind = (AMD64Kind) lirKind.getPlatformKind();
        if (LIRValueUtil.isConstantValue(input))
        {
            emitStoreConst(kind, storeAddress, LIRValueUtil.asConstantValue(input), state);
        }
        else
        {
            emitStore(kind, storeAddress, getLIRGen().asAllocatable(input), state);
        }
    }

    @Override
    public void emitCompareOp(AMD64Kind cmpKind, Variable left, Value right)
    {
        OperandSize size;
        switch (cmpKind)
        {
            case BYTE:
                size = OperandSize.BYTE;
                break;
            case WORD:
                size = OperandSize.WORD;
                break;
            case DWORD:
                size = OperandSize.DWORD;
                break;
            case QWORD:
                size = OperandSize.QWORD;
                break;
            case SINGLE:
                getLIRGen().append(new AMD64BinaryConsumer.Op(SSEOp.UCOMIS, OperandSize.PS, left, getLIRGen().asAllocatable(right)));
                return;
            case DOUBLE:
                getLIRGen().append(new AMD64BinaryConsumer.Op(SSEOp.UCOMIS, OperandSize.PD, left, getLIRGen().asAllocatable(right)));
                return;
            default:
                throw GraalError.shouldNotReachHere("unexpected kind: " + cmpKind);
        }

        if (LIRValueUtil.isConstantValue(right))
        {
            Constant c = LIRValueUtil.asConstant(right);
            if (JavaConstant.isNull(c))
            {
                getLIRGen().append(new AMD64BinaryConsumer.Op(AMD64RMOp.TEST, size, left, left));
                return;
            }
            else if (c instanceof VMConstant)
            {
                VMConstant vc = (VMConstant) c;
                if (size == OperandSize.DWORD)
                {
                    getLIRGen().append(new AMD64BinaryConsumer.VMConstOp(AMD64BinaryArithmetic.CMP.getMIOpcode(OperandSize.DWORD, false), left, vc));
                }
                else
                {
                    getLIRGen().append(new AMD64BinaryConsumer.DataOp(AMD64BinaryArithmetic.CMP.getRMOpcode(size), size, left, vc));
                }
                return;
            }
            else if (c instanceof JavaConstant)
            {
                JavaConstant jc = (JavaConstant) c;
                if (jc.isDefaultForKind())
                {
                    AMD64RMOp op = size == OperandSize.BYTE ? AMD64RMOp.TESTB : AMD64RMOp.TEST;
                    getLIRGen().append(new AMD64BinaryConsumer.Op(op, size, left, left));
                    return;
                }
                else if (NumUtil.is32bit(jc.asLong()))
                {
                    getLIRGen().append(new AMD64BinaryConsumer.ConstOp(AMD64BinaryArithmetic.CMP, size, left, (int) jc.asLong()));
                    return;
                }
            }
        }

        // fallback: load, then compare
        getLIRGen().append(new AMD64BinaryConsumer.Op(AMD64BinaryArithmetic.CMP.getRMOpcode(size), size, left, getLIRGen().asAllocatable(right)));
    }

    @Override
    public Value emitRound(Value value, RoundingMode mode)
    {
        Variable result = getLIRGen().newVariable(LIRKind.combine(value));
        if (value.getPlatformKind() == AMD64Kind.SINGLE)
        {
            getLIRGen().append(new AMD64Binary.RMIOp(AMD64RMIOp.ROUNDSS, OperandSize.PD, result, getLIRGen().asAllocatable(value), mode.encoding));
        }
        else
        {
            getLIRGen().append(new AMD64Binary.RMIOp(AMD64RMIOp.ROUNDSD, OperandSize.PD, result, getLIRGen().asAllocatable(value), mode.encoding));
        }
        return result;
    }
}
