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

import giraaff.asm.amd64.AMD64Assembler;
import giraaff.core.common.LIRKind;
import giraaff.core.common.NumUtil;
import giraaff.lir.ConstantValue;
import giraaff.lir.LIRFrameState;
import giraaff.lir.LIRValueUtil;
import giraaff.lir.Variable;
import giraaff.lir.amd64.AMD64AddressValue;
import giraaff.lir.amd64.AMD64ArithmeticLIRGeneratorTool;
import giraaff.lir.amd64.AMD64Binary;
import giraaff.lir.amd64.AMD64BinaryConsumer;
import giraaff.lir.amd64.AMD64ClearRegisterOp;
import giraaff.lir.amd64.AMD64Move;
import giraaff.lir.amd64.AMD64MulDivOp;
import giraaff.lir.amd64.AMD64ShiftOp;
import giraaff.lir.amd64.AMD64SignExtendOp;
import giraaff.lir.amd64.AMD64Unary;
import giraaff.lir.gen.ArithmeticLIRGenerator;
import giraaff.lir.gen.LIRGenerator;
import giraaff.util.GraalError;

///
// This class implements the AMD64 specific portion of the LIR generator.
///
// @class AMD64ArithmeticLIRGenerator
public final class AMD64ArithmeticLIRGenerator extends ArithmeticLIRGenerator implements AMD64ArithmeticLIRGeneratorTool
{
    // @def
    private static final RegisterValue RCX_I = AMD64.rcx.asValue(LIRKind.value(AMD64Kind.DWORD));

    // @cons AMD64ArithmeticLIRGenerator
    public AMD64ArithmeticLIRGenerator()
    {
        super();
    }

    @Override
    public Variable emitNegate(Value __inputVal)
    {
        AllocatableValue __input = getLIRGen().asAllocatable(__inputVal);
        Variable __result = getLIRGen().newVariable(LIRKind.combine(__input));
        switch ((AMD64Kind) __input.getPlatformKind())
        {
            case DWORD:
            {
                getLIRGen().append(new AMD64Unary.MOp(AMD64Assembler.AMD64MOp.NEG, AMD64Assembler.OperandSize.DWORD, __result, __input));
                break;
            }
            case QWORD:
            {
                getLIRGen().append(new AMD64Unary.MOp(AMD64Assembler.AMD64MOp.NEG, AMD64Assembler.OperandSize.QWORD, __result, __input));
                break;
            }
            default:
                throw GraalError.shouldNotReachHere();
        }
        return __result;
    }

    @Override
    public Variable emitNot(Value __inputVal)
    {
        AllocatableValue __input = getLIRGen().asAllocatable(__inputVal);
        Variable __result = getLIRGen().newVariable(LIRKind.combine(__input));
        switch ((AMD64Kind) __input.getPlatformKind())
        {
            case DWORD:
            {
                getLIRGen().append(new AMD64Unary.MOp(AMD64Assembler.AMD64MOp.NOT, AMD64Assembler.OperandSize.DWORD, __result, __input));
                break;
            }
            case QWORD:
            {
                getLIRGen().append(new AMD64Unary.MOp(AMD64Assembler.AMD64MOp.NOT, AMD64Assembler.OperandSize.QWORD, __result, __input));
                break;
            }
            default:
                throw GraalError.shouldNotReachHere();
        }
        return __result;
    }

    private Variable emitBinary(LIRKind __resultKind, AMD64Assembler.AMD64BinaryArithmetic __op, AMD64Assembler.OperandSize __size, boolean __commutative, Value __a, Value __b, boolean __setFlags)
    {
        if (LIRValueUtil.isJavaConstant(__b))
        {
            return emitBinaryConst(__resultKind, __op, __size, __commutative, getLIRGen().asAllocatable(__a), LIRValueUtil.asConstantValue(__b), __setFlags);
        }
        else if (__commutative && LIRValueUtil.isJavaConstant(__a))
        {
            return emitBinaryConst(__resultKind, __op, __size, __commutative, getLIRGen().asAllocatable(__b), LIRValueUtil.asConstantValue(__a), __setFlags);
        }
        else
        {
            return emitBinaryVar(__resultKind, __op.getRMOpcode(__size), __size, __commutative, getLIRGen().asAllocatable(__a), getLIRGen().asAllocatable(__b));
        }
    }

    private Variable emitBinary(LIRKind __resultKind, AMD64Assembler.AMD64RMOp __op, AMD64Assembler.OperandSize __size, boolean __commutative, Value __a, Value __b)
    {
        if (LIRValueUtil.isJavaConstant(__b))
        {
            return emitBinaryConst(__resultKind, __op, __size, getLIRGen().asAllocatable(__a), LIRValueUtil.asJavaConstant(__b));
        }
        else if (__commutative && LIRValueUtil.isJavaConstant(__a))
        {
            return emitBinaryConst(__resultKind, __op, __size, getLIRGen().asAllocatable(__b), LIRValueUtil.asJavaConstant(__a));
        }
        else
        {
            return emitBinaryVar(__resultKind, __op, __size, __commutative, getLIRGen().asAllocatable(__a), getLIRGen().asAllocatable(__b));
        }
    }

    private Variable emitBinary(LIRKind __resultKind, AMD64Assembler.AMD64RRMOp __op, AMD64Assembler.OperandSize __size, boolean __commutative, Value __a, Value __b)
    {
        if (LIRValueUtil.isJavaConstant(__b))
        {
            return emitBinaryConst(__resultKind, __op, __size, getLIRGen().asAllocatable(__a), LIRValueUtil.asJavaConstant(__b));
        }
        else if (__commutative && LIRValueUtil.isJavaConstant(__a))
        {
            return emitBinaryConst(__resultKind, __op, __size, getLIRGen().asAllocatable(__b), LIRValueUtil.asJavaConstant(__a));
        }
        else
        {
            return emitBinaryVar(__resultKind, __op, __size, __commutative, getLIRGen().asAllocatable(__a), getLIRGen().asAllocatable(__b));
        }
    }

    private Variable emitBinaryConst(LIRKind __resultKind, AMD64Assembler.AMD64BinaryArithmetic __op, AMD64Assembler.OperandSize __size, boolean __commutative, AllocatableValue __a, ConstantValue __b, boolean __setFlags)
    {
        long __value = __b.getJavaConstant().asLong();
        if (NumUtil.isInt(__value))
        {
            Variable __result = getLIRGen().newVariable(__resultKind);
            int __constant = (int) __value;

            if (!__setFlags)
            {
                AMD64Assembler.AMD64MOp __mop = getMOp(__op, __constant);
                if (__mop != null)
                {
                    getLIRGen().append(new AMD64Unary.MOp(__mop, __size, __result, __a));
                    return __result;
                }
            }

            getLIRGen().append(new AMD64Binary.ConstOp(__op, __size, __result, __a, __constant));
            return __result;
        }
        else
        {
            return emitBinaryVar(__resultKind, __op.getRMOpcode(__size), __size, __commutative, __a, getLIRGen().asAllocatable(__b));
        }
    }

    private static AMD64Assembler.AMD64MOp getMOp(AMD64Assembler.AMD64BinaryArithmetic __op, int __constant)
    {
        if (__constant == 1)
        {
            if (__op.equals(AMD64Assembler.AMD64BinaryArithmetic.ADD))
            {
                return AMD64Assembler.AMD64MOp.INC;
            }
            if (__op.equals(AMD64Assembler.AMD64BinaryArithmetic.SUB))
            {
                return AMD64Assembler.AMD64MOp.DEC;
            }
        }
        else if (__constant == -1)
        {
            if (__op.equals(AMD64Assembler.AMD64BinaryArithmetic.ADD))
            {
                return AMD64Assembler.AMD64MOp.DEC;
            }
            if (__op.equals(AMD64Assembler.AMD64BinaryArithmetic.SUB))
            {
                return AMD64Assembler.AMD64MOp.INC;
            }
        }
        return null;
    }

    private Variable emitBinaryConst(LIRKind __resultKind, AMD64Assembler.AMD64RMOp __op, AMD64Assembler.OperandSize __size, AllocatableValue __a, JavaConstant __b)
    {
        Variable __result = getLIRGen().newVariable(__resultKind);
        getLIRGen().append(new AMD64Binary.DataTwoOp(__op, __size, __result, __a, __b));
        return __result;
    }

    private Variable emitBinaryConst(LIRKind __resultKind, AMD64Assembler.AMD64RRMOp __op, AMD64Assembler.OperandSize __size, AllocatableValue __a, JavaConstant __b)
    {
        Variable __result = getLIRGen().newVariable(__resultKind);
        getLIRGen().append(new AMD64Binary.DataThreeOp(__op, __size, __result, __a, __b));
        return __result;
    }

    private Variable emitBinaryVar(LIRKind __resultKind, AMD64Assembler.AMD64RMOp __op, AMD64Assembler.OperandSize __size, boolean __commutative, AllocatableValue __a, AllocatableValue __b)
    {
        Variable __result = getLIRGen().newVariable(__resultKind);
        if (__commutative)
        {
            getLIRGen().append(new AMD64Binary.CommutativeTwoOp(__op, __size, __result, __a, __b));
        }
        else
        {
            getLIRGen().append(new AMD64Binary.TwoOp(__op, __size, __result, __a, __b));
        }
        return __result;
    }

    private Variable emitBinaryVar(LIRKind __resultKind, AMD64Assembler.AMD64RRMOp __op, AMD64Assembler.OperandSize __size, boolean __commutative, AllocatableValue __a, AllocatableValue __b)
    {
        Variable __result = getLIRGen().newVariable(__resultKind);
        if (__commutative)
        {
            getLIRGen().append(new AMD64Binary.CommutativeThreeOp(__op, __size, __result, __a, __b));
        }
        else
        {
            getLIRGen().append(new AMD64Binary.ThreeOp(__op, __size, __result, __a, __b));
        }
        return __result;
    }

    @Override
    protected boolean isNumericInteger(PlatformKind __kind)
    {
        return ((AMD64Kind) __kind).isInteger();
    }

    private Variable emitBaseOffsetLea(LIRKind __resultKind, Value __base, int __offset, AMD64Assembler.OperandSize __size)
    {
        Variable __result = getLIRGen().newVariable(__resultKind);
        AMD64AddressValue __address = new AMD64AddressValue(__resultKind, getLIRGen().asAllocatable(__base), __offset);
        getLIRGen().append(new AMD64Move.LeaOp(__result, __address, __size));
        return __result;
    }

    @Override
    public Variable emitAdd(LIRKind __resultKind, Value __a, Value __b, boolean __setFlags)
    {
        switch ((AMD64Kind) __a.getPlatformKind())
        {
            case DWORD:
                if (LIRValueUtil.isJavaConstant(__b) && !__setFlags)
                {
                    long __displacement = LIRValueUtil.asJavaConstant(__b).asLong();
                    if (NumUtil.isInt(__displacement) && __displacement != 1 && __displacement != -1)
                    {
                        return emitBaseOffsetLea(__resultKind, __a, (int) __displacement, AMD64Assembler.OperandSize.DWORD);
                    }
                }
                return emitBinary(__resultKind, AMD64Assembler.AMD64BinaryArithmetic.ADD, AMD64Assembler.OperandSize.DWORD, true, __a, __b, __setFlags);
            case QWORD:
                if (LIRValueUtil.isJavaConstant(__b) && !__setFlags)
                {
                    long __displacement = LIRValueUtil.asJavaConstant(__b).asLong();
                    if (NumUtil.isInt(__displacement) && __displacement != 1 && __displacement != -1)
                    {
                        return emitBaseOffsetLea(__resultKind, __a, (int) __displacement, AMD64Assembler.OperandSize.QWORD);
                    }
                }
                return emitBinary(__resultKind, AMD64Assembler.AMD64BinaryArithmetic.ADD, AMD64Assembler.OperandSize.QWORD, true, __a, __b, __setFlags);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public Variable emitSub(LIRKind __resultKind, Value __a, Value __b, boolean __setFlags)
    {
        switch ((AMD64Kind) __a.getPlatformKind())
        {
            case DWORD:
                return emitBinary(__resultKind, AMD64Assembler.AMD64BinaryArithmetic.SUB, AMD64Assembler.OperandSize.DWORD, false, __a, __b, __setFlags);
            case QWORD:
                return emitBinary(__resultKind, AMD64Assembler.AMD64BinaryArithmetic.SUB, AMD64Assembler.OperandSize.QWORD, false, __a, __b, __setFlags);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    private Variable emitIMULConst(AMD64Assembler.OperandSize __size, AllocatableValue __a, ConstantValue __b)
    {
        long __value = __b.getJavaConstant().asLong();
        if (NumUtil.isInt(__value))
        {
            int __imm = (int) __value;
            AMD64Assembler.AMD64RMIOp __op;
            if (NumUtil.isByte(__imm))
            {
                __op = AMD64Assembler.AMD64RMIOp.IMUL_SX;
            }
            else
            {
                __op = AMD64Assembler.AMD64RMIOp.IMUL;
            }

            Variable __ret = getLIRGen().newVariable(LIRKind.combine(__a, __b));
            getLIRGen().append(new AMD64Binary.RMIOp(__op, __size, __ret, __a, __imm));
            return __ret;
        }
        else
        {
            return emitBinaryVar(LIRKind.combine(__a, __b), AMD64Assembler.AMD64RMOp.IMUL, __size, true, __a, getLIRGen().asAllocatable(__b));
        }
    }

    private Variable emitIMUL(AMD64Assembler.OperandSize __size, Value __a, Value __b)
    {
        if (LIRValueUtil.isJavaConstant(__b))
        {
            return emitIMULConst(__size, getLIRGen().asAllocatable(__a), LIRValueUtil.asConstantValue(__b));
        }
        else if (LIRValueUtil.isJavaConstant(__a))
        {
            return emitIMULConst(__size, getLIRGen().asAllocatable(__b), LIRValueUtil.asConstantValue(__a));
        }
        else
        {
            return emitBinaryVar(LIRKind.combine(__a, __b), AMD64Assembler.AMD64RMOp.IMUL, __size, true, getLIRGen().asAllocatable(__a), getLIRGen().asAllocatable(__b));
        }
    }

    @Override
    public Variable emitMul(Value __a, Value __b, boolean __setFlags)
    {
        LIRKind __resultKind = LIRKind.combine(__a, __b);
        switch ((AMD64Kind) __a.getPlatformKind())
        {
            case DWORD:
                return emitIMUL(AMD64Assembler.OperandSize.DWORD, __a, __b);
            case QWORD:
                return emitIMUL(AMD64Assembler.OperandSize.QWORD, __a, __b);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    private RegisterValue moveToReg(Register __reg, Value __v)
    {
        RegisterValue __ret = __reg.asValue(__v.getValueKind());
        getLIRGen().emitMove(__ret, __v);
        return __ret;
    }

    private Value emitMulHigh(AMD64Assembler.AMD64MOp __opcode, AMD64Assembler.OperandSize __size, Value __a, Value __b)
    {
        AMD64MulDivOp __mulHigh = getLIRGen().append(new AMD64MulDivOp(__opcode, __size, LIRKind.combine(__a, __b), moveToReg(AMD64.rax, __a), getLIRGen().asAllocatable(__b)));
        return getLIRGen().emitMove(__mulHigh.getHighResult());
    }

    @Override
    public Value emitMulHigh(Value __a, Value __b)
    {
        switch ((AMD64Kind) __a.getPlatformKind())
        {
            case DWORD:
                return emitMulHigh(AMD64Assembler.AMD64MOp.IMUL, AMD64Assembler.OperandSize.DWORD, __a, __b);
            case QWORD:
                return emitMulHigh(AMD64Assembler.AMD64MOp.IMUL, AMD64Assembler.OperandSize.QWORD, __a, __b);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public Value emitUMulHigh(Value __a, Value __b)
    {
        switch ((AMD64Kind) __a.getPlatformKind())
        {
            case DWORD:
                return emitMulHigh(AMD64Assembler.AMD64MOp.MUL, AMD64Assembler.OperandSize.DWORD, __a, __b);
            case QWORD:
                return emitMulHigh(AMD64Assembler.AMD64MOp.MUL, AMD64Assembler.OperandSize.QWORD, __a, __b);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    public Value emitBinaryMemory(AMD64Assembler.AMD64RMOp __op, AMD64Assembler.OperandSize __size, AllocatableValue __a, AMD64AddressValue __location, LIRFrameState __state)
    {
        Variable __result = getLIRGen().newVariable(LIRKind.combine(__a));
        getLIRGen().append(new AMD64Binary.MemoryTwoOp(__op, __size, __result, __a, __location, __state));
        return __result;
    }

    public Value emitBinaryMemory(AMD64Assembler.AMD64RRMOp __op, AMD64Assembler.OperandSize __size, AllocatableValue __a, AMD64AddressValue __location, LIRFrameState __state)
    {
        Variable __result = getLIRGen().newVariable(LIRKind.combine(__a));
        getLIRGen().append(new AMD64Binary.MemoryThreeOp(__op, __size, __result, __a, __location, __state));
        return __result;
    }

    protected Value emitConvertMemoryOp(PlatformKind __kind, AMD64Assembler.AMD64RMOp __op, AMD64Assembler.OperandSize __size, AMD64AddressValue __address, LIRFrameState __state)
    {
        Variable __result = getLIRGen().newVariable(LIRKind.value(__kind));
        getLIRGen().append(new AMD64Unary.MemoryOp(__op, __size, __result, __address, __state));
        return __result;
    }

    protected Value emitZeroExtendMemory(AMD64Kind __memoryKind, int __resultBits, AMD64AddressValue __address, LIRFrameState __state)
    {
        // Issue a zero extending load of the proper bit size and set the result to the proper kind.
        Variable __result = getLIRGen().newVariable(LIRKind.value(__resultBits <= 32 ? AMD64Kind.DWORD : AMD64Kind.QWORD));
        switch (__memoryKind)
        {
            case BYTE:
            {
                getLIRGen().append(new AMD64Unary.MemoryOp(AMD64Assembler.AMD64RMOp.MOVZXB, AMD64Assembler.OperandSize.DWORD, __result, __address, __state));
                break;
            }
            case WORD:
            {
                getLIRGen().append(new AMD64Unary.MemoryOp(AMD64Assembler.AMD64RMOp.MOVZX, AMD64Assembler.OperandSize.DWORD, __result, __address, __state));
                break;
            }
            case DWORD:
            {
                getLIRGen().append(new AMD64Unary.MemoryOp(AMD64Assembler.AMD64RMOp.MOV, AMD64Assembler.OperandSize.DWORD, __result, __address, __state));
                break;
            }
            case QWORD:
            {
                getLIRGen().append(new AMD64Unary.MemoryOp(AMD64Assembler.AMD64RMOp.MOV, AMD64Assembler.OperandSize.QWORD, __result, __address, __state));
                break;
            }
            default:
                throw GraalError.shouldNotReachHere();
        }
        return __result;
    }

    private AMD64MulDivOp emitIDIV(AMD64Assembler.OperandSize __size, Value __a, Value __b, LIRFrameState __state)
    {
        LIRKind __kind = LIRKind.combine(__a, __b);

        AMD64SignExtendOp __sx = getLIRGen().append(new AMD64SignExtendOp(__size, __kind, moveToReg(AMD64.rax, __a)));
        return getLIRGen().append(new AMD64MulDivOp(AMD64Assembler.AMD64MOp.IDIV, __size, __kind, __sx.getHighResult(), __sx.getLowResult(), getLIRGen().asAllocatable(__b), __state));
    }

    private AMD64MulDivOp emitDIV(AMD64Assembler.OperandSize __size, Value __a, Value __b, LIRFrameState __state)
    {
        LIRKind __kind = LIRKind.combine(__a, __b);

        RegisterValue __rax = moveToReg(AMD64.rax, __a);
        RegisterValue __rdx = AMD64.rdx.asValue(__kind);
        getLIRGen().append(new AMD64ClearRegisterOp(__size, __rdx));
        return getLIRGen().append(new AMD64MulDivOp(AMD64Assembler.AMD64MOp.DIV, __size, __kind, __rdx, __rax, getLIRGen().asAllocatable(__b), __state));
    }

    public Value[] emitSignedDivRem(Value __a, Value __b, LIRFrameState __state)
    {
        AMD64MulDivOp __op;
        switch ((AMD64Kind) __a.getPlatformKind())
        {
            case DWORD:
            {
                __op = emitIDIV(AMD64Assembler.OperandSize.DWORD, __a, __b, __state);
                break;
            }
            case QWORD:
            {
                __op = emitIDIV(AMD64Assembler.OperandSize.QWORD, __a, __b, __state);
                break;
            }
            default:
                throw GraalError.shouldNotReachHere();
        }
        return new Value[] { getLIRGen().emitMove(__op.getQuotient()), getLIRGen().emitMove(__op.getRemainder()) };
    }

    public Value[] emitUnsignedDivRem(Value __a, Value __b, LIRFrameState __state)
    {
        AMD64MulDivOp __op;
        switch ((AMD64Kind) __a.getPlatformKind())
        {
            case DWORD:
            {
                __op = emitDIV(AMD64Assembler.OperandSize.DWORD, __a, __b, __state);
                break;
            }
            case QWORD:
            {
                __op = emitDIV(AMD64Assembler.OperandSize.QWORD, __a, __b, __state);
                break;
            }
            default:
                throw GraalError.shouldNotReachHere();
        }
        return new Value[] { getLIRGen().emitMove(__op.getQuotient()), getLIRGen().emitMove(__op.getRemainder()) };
    }

    @Override
    public Value emitDiv(Value __a, Value __b, LIRFrameState __state)
    {
        LIRKind __resultKind = LIRKind.combine(__a, __b);
        switch ((AMD64Kind) __a.getPlatformKind())
        {
            case DWORD:
            {
                AMD64MulDivOp __op = emitIDIV(AMD64Assembler.OperandSize.DWORD, __a, __b, __state);
                return getLIRGen().emitMove(__op.getQuotient());
            }
            case QWORD:
            {
                AMD64MulDivOp __lop = emitIDIV(AMD64Assembler.OperandSize.QWORD, __a, __b, __state);
                return getLIRGen().emitMove(__lop.getQuotient());
            }
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public Value emitRem(Value __a, Value __b, LIRFrameState __state)
    {
        switch ((AMD64Kind) __a.getPlatformKind())
        {
            case DWORD:
            {
                AMD64MulDivOp __op = emitIDIV(AMD64Assembler.OperandSize.DWORD, __a, __b, __state);
                return getLIRGen().emitMove(__op.getRemainder());
            }
            case QWORD:
            {
                AMD64MulDivOp __lop = emitIDIV(AMD64Assembler.OperandSize.QWORD, __a, __b, __state);
                return getLIRGen().emitMove(__lop.getRemainder());
            }
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public Variable emitUDiv(Value __a, Value __b, LIRFrameState __state)
    {
        AMD64MulDivOp __op;
        switch ((AMD64Kind) __a.getPlatformKind())
        {
            case DWORD:
            {
                __op = emitDIV(AMD64Assembler.OperandSize.DWORD, __a, __b, __state);
                break;
            }
            case QWORD:
            {
                __op = emitDIV(AMD64Assembler.OperandSize.QWORD, __a, __b, __state);
                break;
            }
            default:
                throw GraalError.shouldNotReachHere();
        }
        return getLIRGen().emitMove(__op.getQuotient());
    }

    @Override
    public Variable emitURem(Value __a, Value __b, LIRFrameState __state)
    {
        AMD64MulDivOp __op;
        switch ((AMD64Kind) __a.getPlatformKind())
        {
            case DWORD:
            {
                __op = emitDIV(AMD64Assembler.OperandSize.DWORD, __a, __b, __state);
                break;
            }
            case QWORD:
            {
                __op = emitDIV(AMD64Assembler.OperandSize.QWORD, __a, __b, __state);
                break;
            }
            default:
                throw GraalError.shouldNotReachHere();
        }
        return getLIRGen().emitMove(__op.getRemainder());
    }

    @Override
    public Variable emitAnd(Value __a, Value __b)
    {
        LIRKind __resultKind = LIRKind.combine(__a, __b);
        switch ((AMD64Kind) __a.getPlatformKind())
        {
            case DWORD:
                return emitBinary(__resultKind, AMD64Assembler.AMD64BinaryArithmetic.AND, AMD64Assembler.OperandSize.DWORD, true, __a, __b, false);
            case QWORD:
                return emitBinary(__resultKind, AMD64Assembler.AMD64BinaryArithmetic.AND, AMD64Assembler.OperandSize.QWORD, true, __a, __b, false);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public Variable emitOr(Value __a, Value __b)
    {
        LIRKind __resultKind = LIRKind.combine(__a, __b);
        switch ((AMD64Kind) __a.getPlatformKind())
        {
            case DWORD:
                return emitBinary(__resultKind, AMD64Assembler.AMD64BinaryArithmetic.OR, AMD64Assembler.OperandSize.DWORD, true, __a, __b, false);
            case QWORD:
                return emitBinary(__resultKind, AMD64Assembler.AMD64BinaryArithmetic.OR, AMD64Assembler.OperandSize.QWORD, true, __a, __b, false);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public Variable emitXor(Value __a, Value __b)
    {
        LIRKind __resultKind = LIRKind.combine(__a, __b);
        switch ((AMD64Kind) __a.getPlatformKind())
        {
            case DWORD:
                return emitBinary(__resultKind, AMD64Assembler.AMD64BinaryArithmetic.XOR, AMD64Assembler.OperandSize.DWORD, true, __a, __b, false);
            case QWORD:
                return emitBinary(__resultKind, AMD64Assembler.AMD64BinaryArithmetic.XOR, AMD64Assembler.OperandSize.QWORD, true, __a, __b, false);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    private Variable emitShift(AMD64Assembler.AMD64Shift __op, AMD64Assembler.OperandSize __size, Value __a, Value __b)
    {
        LIRGenerator __gen = getLIRGen();
        Variable __result = __gen.newVariable(LIRKind.combine(__a, __b).changeType(__a.getPlatformKind()));
        AllocatableValue __input = __gen.asAllocatable(__a);
        if (LIRValueUtil.isJavaConstant(__b))
        {
            JavaConstant __c = LIRValueUtil.asJavaConstant(__b);
            if (__c.asLong() == 1)
            {
                __gen.append(new AMD64Unary.MOp(__op.___m1Op, __size, __result, __input));
            }
            else
            {
                // c is implicitly masked to 5 or 6 bits by the CPU, so casting it to (int) is
                // always correct, even without the NumUtil.is32bit() test.
                __gen.append(new AMD64Binary.ConstOp(__op.___miOp, __size, __result, __input, (int) __c.asLong()));
            }
        }
        else
        {
            __gen.emitMove(RCX_I, __b);
            __gen.append(new AMD64ShiftOp(__op.___mcOp, __size, __result, __input, RCX_I));
        }
        return __result;
    }

    @Override
    public Variable emitShl(Value __a, Value __b)
    {
        switch ((AMD64Kind) __a.getPlatformKind())
        {
            case DWORD:
                return emitShift(AMD64Assembler.AMD64Shift.SHL, AMD64Assembler.OperandSize.DWORD, __a, __b);
            case QWORD:
                return emitShift(AMD64Assembler.AMD64Shift.SHL, AMD64Assembler.OperandSize.QWORD, __a, __b);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public Variable emitShr(Value __a, Value __b)
    {
        switch ((AMD64Kind) __a.getPlatformKind())
        {
            case DWORD:
                return emitShift(AMD64Assembler.AMD64Shift.SAR, AMD64Assembler.OperandSize.DWORD, __a, __b);
            case QWORD:
                return emitShift(AMD64Assembler.AMD64Shift.SAR, AMD64Assembler.OperandSize.QWORD, __a, __b);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public Variable emitUShr(Value __a, Value __b)
    {
        switch ((AMD64Kind) __a.getPlatformKind())
        {
            case DWORD:
                return emitShift(AMD64Assembler.AMD64Shift.SHR, AMD64Assembler.OperandSize.DWORD, __a, __b);
            case QWORD:
                return emitShift(AMD64Assembler.AMD64Shift.SHR, AMD64Assembler.OperandSize.QWORD, __a, __b);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    public Variable emitRol(Value __a, Value __b)
    {
        switch ((AMD64Kind) __a.getPlatformKind())
        {
            case DWORD:
                return emitShift(AMD64Assembler.AMD64Shift.ROL, AMD64Assembler.OperandSize.DWORD, __a, __b);
            case QWORD:
                return emitShift(AMD64Assembler.AMD64Shift.ROL, AMD64Assembler.OperandSize.QWORD, __a, __b);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    public Variable emitRor(Value __a, Value __b)
    {
        switch ((AMD64Kind) __a.getPlatformKind())
        {
            case DWORD:
                return emitShift(AMD64Assembler.AMD64Shift.ROR, AMD64Assembler.OperandSize.DWORD, __a, __b);
            case QWORD:
                return emitShift(AMD64Assembler.AMD64Shift.ROR, AMD64Assembler.OperandSize.QWORD, __a, __b);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    private AllocatableValue emitConvertOp(LIRKind __kind, AMD64Assembler.AMD64RMOp __op, AMD64Assembler.OperandSize __size, Value __input)
    {
        Variable __result = getLIRGen().newVariable(__kind);
        getLIRGen().append(new AMD64Unary.RMOp(__op, __size, __result, getLIRGen().asAllocatable(__input)));
        return __result;
    }

    private AllocatableValue emitConvertOp(LIRKind __kind, AMD64Assembler.AMD64MROp __op, AMD64Assembler.OperandSize __size, Value __input)
    {
        Variable __result = getLIRGen().newVariable(__kind);
        getLIRGen().append(new AMD64Unary.MROp(__op, __size, __result, getLIRGen().asAllocatable(__input)));
        return __result;
    }

    @Override
    public Value emitReinterpret(LIRKind __to, Value __inputVal)
    {
        ValueKind<?> __from = __inputVal.getValueKind();
        if (__to.equals(__from))
        {
            return __inputVal;
        }

        throw GraalError.shouldNotReachHere();
    }

    @Override
    public Value emitNarrow(Value __inputVal, int __bits)
    {
        if (__inputVal.getPlatformKind() == AMD64Kind.QWORD && __bits <= 32)
        {
            // TODO make it possible to reinterpret Long as Int in LIR without move
            return emitConvertOp(LIRKind.combine(__inputVal).changeType(AMD64Kind.DWORD), AMD64Assembler.AMD64RMOp.MOV, AMD64Assembler.OperandSize.DWORD, __inputVal);
        }
        else
        {
            return __inputVal;
        }
    }

    @Override
    public Value emitSignExtend(Value __inputVal, int __fromBits, int __toBits)
    {
        if (__fromBits == __toBits)
        {
            return __inputVal;
        }
        else if (__toBits > 32)
        {
            // sign extend to 64 bits
            switch (__fromBits)
            {
                case 8:
                    return emitConvertOp(LIRKind.combine(__inputVal).changeType(AMD64Kind.QWORD), AMD64Assembler.AMD64RMOp.MOVSXB, AMD64Assembler.OperandSize.QWORD, __inputVal);
                case 16:
                    return emitConvertOp(LIRKind.combine(__inputVal).changeType(AMD64Kind.QWORD), AMD64Assembler.AMD64RMOp.MOVSX, AMD64Assembler.OperandSize.QWORD, __inputVal);
                case 32:
                    return emitConvertOp(LIRKind.combine(__inputVal).changeType(AMD64Kind.QWORD), AMD64Assembler.AMD64RMOp.MOVSXD, AMD64Assembler.OperandSize.QWORD, __inputVal);
                default:
                    throw GraalError.unimplemented("unsupported sign extension (" + __fromBits + " bit -> " + __toBits + " bit)");
            }
        }
        else
        {
            // sign extend to 32 bits (smaller values are internally represented as 32 bit values)
            switch (__fromBits)
            {
                case 8:
                    return emitConvertOp(LIRKind.combine(__inputVal).changeType(AMD64Kind.DWORD), AMD64Assembler.AMD64RMOp.MOVSXB, AMD64Assembler.OperandSize.DWORD, __inputVal);
                case 16:
                    return emitConvertOp(LIRKind.combine(__inputVal).changeType(AMD64Kind.DWORD), AMD64Assembler.AMD64RMOp.MOVSX, AMD64Assembler.OperandSize.DWORD, __inputVal);
                case 32:
                    return __inputVal;
                default:
                    throw GraalError.unimplemented("unsupported sign extension (" + __fromBits + " bit -> " + __toBits + " bit)");
            }
        }
    }

    @Override
    public Value emitZeroExtend(Value __inputVal, int __fromBits, int __toBits)
    {
        if (__fromBits == __toBits)
        {
            return __inputVal;
        }
        else if (__fromBits > 32)
        {
            LIRGenerator __gen = getLIRGen();
            Variable __result = __gen.newVariable(LIRKind.combine(__inputVal));
            long __mask = CodeUtil.mask(__fromBits);
            __gen.append(new AMD64Binary.DataTwoOp(AMD64Assembler.AMD64BinaryArithmetic.AND.getRMOpcode(AMD64Assembler.OperandSize.QWORD), AMD64Assembler.OperandSize.QWORD, __result, __gen.asAllocatable(__inputVal), JavaConstant.forLong(__mask)));
            return __result;
        }
        else
        {
            LIRKind __resultKind = LIRKind.combine(__inputVal);
            if (__toBits > 32)
            {
                __resultKind = __resultKind.changeType(AMD64Kind.QWORD);
            }
            else
            {
                __resultKind = __resultKind.changeType(AMD64Kind.DWORD);
            }

            // Always emit DWORD operations, even if the resultKind is Long. On AMD64, all DWORD
            // operations implicitly set the upper half of the register to 0, which is what we want
            // anyway. Compared to the QWORD operations, the encoding of the DWORD operations is
            // sometimes one byte shorter.
            switch (__fromBits)
            {
                case 8:
                    return emitConvertOp(__resultKind, AMD64Assembler.AMD64RMOp.MOVZXB, AMD64Assembler.OperandSize.DWORD, __inputVal);
                case 16:
                    return emitConvertOp(__resultKind, AMD64Assembler.AMD64RMOp.MOVZX, AMD64Assembler.OperandSize.DWORD, __inputVal);
                case 32:
                    return emitConvertOp(__resultKind, AMD64Assembler.AMD64RMOp.MOV, AMD64Assembler.OperandSize.DWORD, __inputVal);
            }

            // odd bit count, fall back on manual masking
            LIRGenerator __gen = getLIRGen();
            Variable __result = __gen.newVariable(__resultKind);
            JavaConstant __mask;
            if (__toBits > 32)
            {
                __mask = JavaConstant.forLong(CodeUtil.mask(__fromBits));
            }
            else
            {
                __mask = JavaConstant.forInt((int) CodeUtil.mask(__fromBits));
            }
            __gen.append(new AMD64Binary.DataTwoOp(AMD64Assembler.AMD64BinaryArithmetic.AND.getRMOpcode(AMD64Assembler.OperandSize.DWORD), AMD64Assembler.OperandSize.DWORD, __result, __gen.asAllocatable(__inputVal), __mask));
            return __result;
        }
    }

    @Override
    public Variable emitBitCount(Value __value)
    {
        LIRGenerator __gen = getLIRGen();
        Variable __result = __gen.newVariable(LIRKind.combine(__value).changeType(AMD64Kind.DWORD));
        if (__value.getPlatformKind() == AMD64Kind.QWORD)
        {
            __gen.append(new AMD64Unary.RMOp(AMD64Assembler.AMD64RMOp.POPCNT, AMD64Assembler.OperandSize.QWORD, __result, __gen.asAllocatable(__value)));
        }
        else
        {
            __gen.append(new AMD64Unary.RMOp(AMD64Assembler.AMD64RMOp.POPCNT, AMD64Assembler.OperandSize.DWORD, __result, __gen.asAllocatable(__value)));
        }
        return __result;
    }

    @Override
    public Variable emitBitScanForward(Value __value)
    {
        LIRGenerator __gen = getLIRGen();
        Variable __result = __gen.newVariable(LIRKind.combine(__value).changeType(AMD64Kind.DWORD));
        __gen.append(new AMD64Unary.RMOp(AMD64Assembler.AMD64RMOp.BSF, AMD64Assembler.OperandSize.QWORD, __result, __gen.asAllocatable(__value)));
        return __result;
    }

    @Override
    public Variable emitBitScanReverse(Value __value)
    {
        LIRGenerator __gen = getLIRGen();
        Variable __result = __gen.newVariable(LIRKind.combine(__value).changeType(AMD64Kind.DWORD));
        if (__value.getPlatformKind() == AMD64Kind.QWORD)
        {
            __gen.append(new AMD64Unary.RMOp(AMD64Assembler.AMD64RMOp.BSR, AMD64Assembler.OperandSize.QWORD, __result, __gen.asAllocatable(__value)));
        }
        else
        {
            __gen.append(new AMD64Unary.RMOp(AMD64Assembler.AMD64RMOp.BSR, AMD64Assembler.OperandSize.DWORD, __result, __gen.asAllocatable(__value)));
        }
        return __result;
    }

    @Override
    public Value emitCountLeadingZeros(Value __value)
    {
        LIRGenerator __gen = getLIRGen();
        Variable __result = __gen.newVariable(LIRKind.combine(__value).changeType(AMD64Kind.DWORD));
        if (__value.getPlatformKind() == AMD64Kind.QWORD)
        {
            __gen.append(new AMD64Unary.RMOp(AMD64Assembler.AMD64RMOp.LZCNT, AMD64Assembler.OperandSize.QWORD, __result, __gen.asAllocatable(__value)));
        }
        else
        {
            __gen.append(new AMD64Unary.RMOp(AMD64Assembler.AMD64RMOp.LZCNT, AMD64Assembler.OperandSize.DWORD, __result, __gen.asAllocatable(__value)));
        }
        return __result;
    }

    @Override
    public Value emitCountTrailingZeros(Value __value)
    {
        LIRGenerator __gen = getLIRGen();
        Variable __result = __gen.newVariable(LIRKind.combine(__value).changeType(AMD64Kind.DWORD));
        if (__value.getPlatformKind() == AMD64Kind.QWORD)
        {
            __gen.append(new AMD64Unary.RMOp(AMD64Assembler.AMD64RMOp.TZCNT, AMD64Assembler.OperandSize.QWORD, __result, __gen.asAllocatable(__value)));
        }
        else
        {
            __gen.append(new AMD64Unary.RMOp(AMD64Assembler.AMD64RMOp.TZCNT, AMD64Assembler.OperandSize.DWORD, __result, __gen.asAllocatable(__value)));
        }
        return __result;
    }

    protected AMD64LIRGenerator getAMD64LIRGen()
    {
        return (AMD64LIRGenerator) getLIRGen();
    }

    @Override
    public Variable emitLoad(LIRKind __kind, Value __address, LIRFrameState __state)
    {
        AMD64AddressValue __loadAddress = getAMD64LIRGen().asAddressValue(__address);
        Variable __result = getLIRGen().newVariable(getLIRGen().toRegisterKind(__kind));
        switch ((AMD64Kind) __kind.getPlatformKind())
        {
            case BYTE:
            {
                getLIRGen().append(new AMD64Unary.MemoryOp(AMD64Assembler.AMD64RMOp.MOVSXB, AMD64Assembler.OperandSize.DWORD, __result, __loadAddress, __state));
                break;
            }
            case WORD:
            {
                getLIRGen().append(new AMD64Unary.MemoryOp(AMD64Assembler.AMD64RMOp.MOVSX, AMD64Assembler.OperandSize.DWORD, __result, __loadAddress, __state));
                break;
            }
            case DWORD:
            {
                getLIRGen().append(new AMD64Unary.MemoryOp(AMD64Assembler.AMD64RMOp.MOV, AMD64Assembler.OperandSize.DWORD, __result, __loadAddress, __state));
                break;
            }
            case QWORD:
            {
                getLIRGen().append(new AMD64Unary.MemoryOp(AMD64Assembler.AMD64RMOp.MOV, AMD64Assembler.OperandSize.QWORD, __result, __loadAddress, __state));
                break;
            }
            default:
                throw GraalError.shouldNotReachHere();
        }
        return __result;
    }

    protected void emitStoreConst(AMD64Kind __kind, AMD64AddressValue __address, ConstantValue __value, LIRFrameState __state)
    {
        Constant __c = __value.getConstant();
        if (JavaConstant.isNull(__c))
        {
            AMD64Assembler.OperandSize __size = __kind == AMD64Kind.DWORD ? AMD64Assembler.OperandSize.DWORD : AMD64Assembler.OperandSize.QWORD;
            getLIRGen().append(new AMD64BinaryConsumer.MemoryConstOp(AMD64Assembler.AMD64MIOp.MOV, __size, __address, 0, __state));
            return;
        }
        else if (__c instanceof VMConstant)
        {
            // only 32-bit constants can be patched
            if (__kind == AMD64Kind.DWORD)
            {
                if (getLIRGen().target().inlineObjects || !(__c instanceof JavaConstant))
                {
                    // if c is a JavaConstant, it's an oop, otherwise it's a metaspace constant
                    getLIRGen().append(new AMD64BinaryConsumer.MemoryVMConstOp(AMD64Assembler.AMD64MIOp.MOV, __address, (VMConstant) __c, __state));
                    return;
                }
            }
        }
        else
        {
            JavaConstant __jc = (JavaConstant) __c;

            AMD64Assembler.AMD64MIOp __op = AMD64Assembler.AMD64MIOp.MOV;
            AMD64Assembler.OperandSize __size;
            long __imm;

            switch (__kind)
            {
                case BYTE:
                    __op = AMD64Assembler.AMD64MIOp.MOVB;
                    __size = AMD64Assembler.OperandSize.BYTE;
                    __imm = __jc.asInt();
                    break;
                case WORD:
                    __size = AMD64Assembler.OperandSize.WORD;
                    __imm = __jc.asInt();
                    break;
                case DWORD:
                    __size = AMD64Assembler.OperandSize.DWORD;
                    __imm = __jc.asInt();
                    break;
                case QWORD:
                    __size = AMD64Assembler.OperandSize.QWORD;
                    __imm = __jc.asLong();
                    break;
                default:
                    throw GraalError.shouldNotReachHere("unexpected kind " + __kind);
            }

            if (NumUtil.isInt(__imm))
            {
                getLIRGen().append(new AMD64BinaryConsumer.MemoryConstOp(__op, __size, __address, (int) __imm, __state));
                return;
            }
        }

        // fallback: load, then store
        emitStore(__kind, __address, getLIRGen().asAllocatable(__value), __state);
    }

    protected void emitStore(AMD64Kind __kind, AMD64AddressValue __address, AllocatableValue __value, LIRFrameState __state)
    {
        switch (__kind)
        {
            case BYTE:
            {
                getLIRGen().append(new AMD64BinaryConsumer.MemoryMROp(AMD64Assembler.AMD64MROp.MOVB, AMD64Assembler.OperandSize.BYTE, __address, __value, __state));
                break;
            }
            case WORD:
            {
                getLIRGen().append(new AMD64BinaryConsumer.MemoryMROp(AMD64Assembler.AMD64MROp.MOV, AMD64Assembler.OperandSize.WORD, __address, __value, __state));
                break;
            }
            case DWORD:
            {
                getLIRGen().append(new AMD64BinaryConsumer.MemoryMROp(AMD64Assembler.AMD64MROp.MOV, AMD64Assembler.OperandSize.DWORD, __address, __value, __state));
                break;
            }
            case QWORD:
            {
                getLIRGen().append(new AMD64BinaryConsumer.MemoryMROp(AMD64Assembler.AMD64MROp.MOV, AMD64Assembler.OperandSize.QWORD, __address, __value, __state));
                break;
            }
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public void emitStore(ValueKind<?> __lirKind, Value __address, Value __input, LIRFrameState __state)
    {
        AMD64AddressValue __storeAddress = getAMD64LIRGen().asAddressValue(__address);
        AMD64Kind __kind = (AMD64Kind) __lirKind.getPlatformKind();
        if (LIRValueUtil.isConstantValue(__input))
        {
            emitStoreConst(__kind, __storeAddress, LIRValueUtil.asConstantValue(__input), __state);
        }
        else
        {
            emitStore(__kind, __storeAddress, getLIRGen().asAllocatable(__input), __state);
        }
    }

    @Override
    public void emitCompareOp(AMD64Kind __cmpKind, Variable __left, Value __right)
    {
        AMD64Assembler.OperandSize __size;
        switch (__cmpKind)
        {
            case BYTE:
            {
                __size = AMD64Assembler.OperandSize.BYTE;
                break;
            }
            case WORD:
            {
                __size = AMD64Assembler.OperandSize.WORD;
                break;
            }
            case DWORD:
            {
                __size = AMD64Assembler.OperandSize.DWORD;
                break;
            }
            case QWORD:
            {
                __size = AMD64Assembler.OperandSize.QWORD;
                break;
            }
            default:
                throw GraalError.shouldNotReachHere("unexpected kind: " + __cmpKind);
        }

        if (LIRValueUtil.isConstantValue(__right))
        {
            Constant __c = LIRValueUtil.asConstant(__right);
            if (JavaConstant.isNull(__c))
            {
                getLIRGen().append(new AMD64BinaryConsumer.ConsumerOp(AMD64Assembler.AMD64RMOp.TEST, __size, __left, __left));
                return;
            }
            else if (__c instanceof VMConstant)
            {
                VMConstant __vc = (VMConstant) __c;
                if (__size == AMD64Assembler.OperandSize.DWORD)
                {
                    getLIRGen().append(new AMD64BinaryConsumer.VMConstOp(AMD64Assembler.AMD64BinaryArithmetic.CMP.getMIOpcode(AMD64Assembler.OperandSize.DWORD, false), __left, __vc));
                }
                else
                {
                    getLIRGen().append(new AMD64BinaryConsumer.DataOp(AMD64Assembler.AMD64BinaryArithmetic.CMP.getRMOpcode(__size), __size, __left, __vc));
                }
                return;
            }
            else if (__c instanceof JavaConstant)
            {
                JavaConstant __jc = (JavaConstant) __c;
                if (__jc.isDefaultForKind())
                {
                    AMD64Assembler.AMD64RMOp __op = __size == AMD64Assembler.OperandSize.BYTE ? AMD64Assembler.AMD64RMOp.TESTB : AMD64Assembler.AMD64RMOp.TEST;
                    getLIRGen().append(new AMD64BinaryConsumer.ConsumerOp(__op, __size, __left, __left));
                    return;
                }
                else if (NumUtil.is32bit(__jc.asLong()))
                {
                    getLIRGen().append(new AMD64BinaryConsumer.ConsumerConstOp(AMD64Assembler.AMD64BinaryArithmetic.CMP, __size, __left, (int) __jc.asLong()));
                    return;
                }
            }
        }

        // fallback: load, then compare
        getLIRGen().append(new AMD64BinaryConsumer.ConsumerOp(AMD64Assembler.AMD64BinaryArithmetic.CMP.getRMOpcode(__size), __size, __left, getLIRGen().asAllocatable(__right)));
    }
}
