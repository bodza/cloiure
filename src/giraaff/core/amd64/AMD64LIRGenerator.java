package giraaff.core.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.PlatformKind;
import jdk.vm.ci.meta.VMConstant;
import jdk.vm.ci.meta.Value;
import jdk.vm.ci.meta.ValueKind;

import giraaff.asm.amd64.AMD64Assembler;
import giraaff.core.common.LIRKind;
import giraaff.core.common.NumUtil;
import giraaff.core.common.calc.Condition;
import giraaff.core.common.spi.ForeignCallLinkage;
import giraaff.core.common.spi.LIRKindTool;
import giraaff.lir.ConstantValue;
import giraaff.lir.LIRFrameState;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRValueUtil;
import giraaff.lir.LabelRef;
import giraaff.lir.StandardOp;
import giraaff.lir.SwitchStrategy;
import giraaff.lir.Variable;
import giraaff.lir.amd64.AMD64AddressValue;
import giraaff.lir.amd64.AMD64ArithmeticLIRGeneratorTool;
import giraaff.lir.amd64.AMD64ArrayCompareToOp;
import giraaff.lir.amd64.AMD64ArrayEqualsOp;
import giraaff.lir.amd64.AMD64Binary;
import giraaff.lir.amd64.AMD64BinaryConsumer;
import giraaff.lir.amd64.AMD64ByteSwapOp;
import giraaff.lir.amd64.AMD64Call;
import giraaff.lir.amd64.AMD64ControlFlow;
import giraaff.lir.amd64.AMD64LFenceOp;
import giraaff.lir.amd64.AMD64Move;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.gen.LIRGenerator;
import giraaff.lir.gen.LIRGeneratorTool;
import giraaff.phases.util.Providers;
import giraaff.util.GraalError;

///
// This class implements the AMD64 specific portion of the LIR generator.
///
// @class AMD64LIRGenerator
public abstract class AMD64LIRGenerator extends LIRGenerator
{
    // @cons AMD64LIRGenerator
    public AMD64LIRGenerator(LIRKindTool __lirKindTool, AMD64ArithmeticLIRGenerator __arithmeticLIRGen, LIRGeneratorTool.MoveFactory __moveFactory, Providers __providers, LIRGenerationResult __lirGenRes)
    {
        super(__lirKindTool, __arithmeticLIRGen, __moveFactory, __providers, __lirGenRes);
    }

    public AMD64AddressValue asAddressValue(Value __address)
    {
        if (__address instanceof AMD64AddressValue)
        {
            return (AMD64AddressValue) __address;
        }
        else
        {
            if (__address instanceof JavaConstant)
            {
                long __displacement = ((JavaConstant) __address).asLong();
                if (NumUtil.isInt(__displacement))
                {
                    return new AMD64AddressValue(__address.getValueKind(), Value.ILLEGAL, (int) __displacement);
                }
            }
            return new AMD64AddressValue(__address.getValueKind(), asAllocatable(__address), 0);
        }
    }

    @Override
    public Variable emitAddress(AllocatableValue __stackslot)
    {
        Variable __result = newVariable(LIRKind.value(target().arch.getWordKind()));
        append(new AMD64Move.StackLeaOp(__result, __stackslot));
        return __result;
    }

    ///
    // The AMD64 backend only uses DWORD and QWORD values in registers because of a performance penalty
    // when accessing WORD or BYTE registers. This function converts small integer kinds to DWORD.
    ///
    @Override
    public <K extends ValueKind<K>> K toRegisterKind(K __kind)
    {
        switch ((AMD64Kind) __kind.getPlatformKind())
        {
            case BYTE:
            case WORD:
                return __kind.changeType(AMD64Kind.DWORD);
            default:
                return __kind;
        }
    }

    @Override
    public Variable emitLogicCompareAndSwap(Value __address, Value __expectedValue, Value __newValue, Value __trueValue, Value __falseValue)
    {
        ValueKind<?> __kind = __newValue.getValueKind();
        AMD64Kind __memKind = (AMD64Kind) __kind.getPlatformKind();

        AMD64AddressValue __addressValue = asAddressValue(__address);
        RegisterValue __raxRes = AMD64.rax.asValue(__kind);
        emitMove(__raxRes, __expectedValue);
        append(new AMD64Move.CompareAndSwapOp(__memKind, __raxRes, __addressValue, __raxRes, asAllocatable(__newValue)));

        Variable __result = newVariable(__trueValue.getValueKind());
        append(new AMD64ControlFlow.CondMoveOp(__result, Condition.EQ, asAllocatable(__trueValue), __falseValue));
        return __result;
    }

    @Override
    public Value emitValueCompareAndSwap(Value __address, Value __expectedValue, Value __newValue)
    {
        ValueKind<?> __kind = __newValue.getValueKind();
        AMD64Kind __memKind = (AMD64Kind) __kind.getPlatformKind();

        AMD64AddressValue __addressValue = asAddressValue(__address);
        RegisterValue __raxRes = AMD64.rax.asValue(__kind);
        emitMove(__raxRes, __expectedValue);
        append(new AMD64Move.CompareAndSwapOp(__memKind, __raxRes, __addressValue, __raxRes, asAllocatable(__newValue)));
        Variable __result = newVariable(__kind);
        emitMove(__result, __raxRes);
        return __result;
    }

    public void emitCompareAndSwapBranch(ValueKind<?> __kind, AMD64AddressValue __address, Value __expectedValue, Value __newValue, Condition __condition, LabelRef __trueLabel, LabelRef __falseLabel, double __trueLabelProbability)
    {
        AMD64Kind __memKind = (AMD64Kind) __kind.getPlatformKind();
        RegisterValue __raxValue = AMD64.rax.asValue(__kind);
        emitMove(__raxValue, __expectedValue);
        append(new AMD64Move.CompareAndSwapOp(__memKind, __raxValue, __address, __raxValue, asAllocatable(__newValue)));
        append(new AMD64ControlFlow.BranchOp(__condition, __trueLabel, __falseLabel, __trueLabelProbability));
    }

    @Override
    public Value emitAtomicReadAndAdd(Value __address, Value __delta)
    {
        ValueKind<?> __kind = __delta.getValueKind();
        Variable __result = newVariable(__kind);
        AMD64AddressValue __addressValue = asAddressValue(__address);
        append(new AMD64Move.AtomicReadAndAddOp((AMD64Kind) __kind.getPlatformKind(), __result, __addressValue, asAllocatable(__delta)));
        return __result;
    }

    @Override
    public Value emitAtomicReadAndWrite(Value __address, Value __newValue)
    {
        ValueKind<?> __kind = __newValue.getValueKind();
        Variable __result = newVariable(__kind);
        AMD64AddressValue __addressValue = asAddressValue(__address);
        append(new AMD64Move.AtomicReadAndWriteOp((AMD64Kind) __kind.getPlatformKind(), __result, __addressValue, asAllocatable(__newValue)));
        return __result;
    }

    @Override
    public void emitNullCheck(Value __address, LIRFrameState __state)
    {
        append(new AMD64Move.NullCheckOp(asAddressValue(__address), __state));
    }

    @Override
    public void emitJump(LabelRef __label)
    {
        append(new StandardOp.JumpOp(__label));
    }

    @Override
    public void emitCompareBranch(PlatformKind __cmpKind, Value __left, Value __right, Condition __cond, LabelRef __trueLabel, LabelRef __falseLabel, double __trueLabelProbability)
    {
        __cond = emitCompare(__cmpKind, __left, __right, __cond);
        append(new AMD64ControlFlow.BranchOp(__cond, __trueLabel, __falseLabel, __trueLabelProbability));
    }

    @Override
    public void emitOverflowCheckBranch(LabelRef __overflow, LabelRef __noOverflow, LIRKind __cmpLIRKind, double __overflowProbability)
    {
        append(new AMD64ControlFlow.BranchOp(AMD64Assembler.ConditionFlag.Overflow, __overflow, __noOverflow, __overflowProbability));
    }

    @Override
    public void emitIntegerTestBranch(Value __left, Value __right, LabelRef __trueDestination, LabelRef __falseDestination, double __trueDestinationProbability)
    {
        emitIntegerTest(__left, __right);
        append(new AMD64ControlFlow.BranchOp(Condition.EQ, __trueDestination, __falseDestination, __trueDestinationProbability));
    }

    @Override
    public Variable emitConditionalMove(PlatformKind __cmpKind, Value __left, Value __right, Condition __cond, Value __trueValue, Value __falseValue)
    {
        __cond = emitCompare(__cmpKind, __left, __right, __cond);

        Variable __result = newVariable(__trueValue.getValueKind());
        if (LIRValueUtil.isIntConstant(__trueValue, 1) && LIRValueUtil.isIntConstant(__falseValue, 0))
        {
            append(new AMD64ControlFlow.CondSetOp(__result, __cond));
        }
        else if (LIRValueUtil.isIntConstant(__trueValue, 0) && LIRValueUtil.isIntConstant(__falseValue, 1))
        {
            append(new AMD64ControlFlow.CondSetOp(__result, __cond.negate()));
        }
        else
        {
            append(new AMD64ControlFlow.CondMoveOp(__result, __cond, load(__trueValue), loadNonConst(__falseValue)));
        }
        return __result;
    }

    @Override
    public Variable emitIntegerTestMove(Value __left, Value __right, Value __trueValue, Value __falseValue)
    {
        emitIntegerTest(__left, __right);
        Variable __result = newVariable(__trueValue.getValueKind());
        append(new AMD64ControlFlow.CondMoveOp(__result, Condition.EQ, load(__trueValue), loadNonConst(__falseValue)));
        return __result;
    }

    private void emitIntegerTest(Value __a, Value __b)
    {
        AMD64Assembler.OperandSize __size = __a.getPlatformKind() == AMD64Kind.QWORD ? AMD64Assembler.OperandSize.QWORD : AMD64Assembler.OperandSize.DWORD;
        if (LIRValueUtil.isJavaConstant(__b) && NumUtil.is32bit(LIRValueUtil.asJavaConstant(__b).asLong()))
        {
            append(new AMD64BinaryConsumer.ConsumerConstOp(AMD64Assembler.AMD64MIOp.TEST, __size, asAllocatable(__a), (int) LIRValueUtil.asJavaConstant(__b).asLong()));
        }
        else if (LIRValueUtil.isJavaConstant(__a) && NumUtil.is32bit(LIRValueUtil.asJavaConstant(__a).asLong()))
        {
            append(new AMD64BinaryConsumer.ConsumerConstOp(AMD64Assembler.AMD64MIOp.TEST, __size, asAllocatable(__b), (int) LIRValueUtil.asJavaConstant(__a).asLong()));
        }
        else if (ValueUtil.isAllocatableValue(__b))
        {
            append(new AMD64BinaryConsumer.ConsumerOp(AMD64Assembler.AMD64RMOp.TEST, __size, asAllocatable(__b), asAllocatable(__a)));
        }
        else
        {
            append(new AMD64BinaryConsumer.ConsumerOp(AMD64Assembler.AMD64RMOp.TEST, __size, asAllocatable(__a), asAllocatable(__b)));
        }
    }

    ///
    // This method emits the compare against memory instruction, and may reorder the operands. It
    // returns true if it did so.
    //
    // @param b the right operand of the comparison
    // @return true if the left and right operands were switched, false otherwise
    ///
    private boolean emitCompareMemory(AMD64Kind __cmpKind, Value __a, AMD64AddressValue __b, LIRFrameState __state)
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

        if (LIRValueUtil.isConstantValue(__a))
        {
            return emitCompareMemoryConOp(__size, LIRValueUtil.asConstantValue(__a), __b, __state);
        }
        else
        {
            return emitCompareRegMemoryOp(__size, asAllocatable(__a), __b, __state);
        }
    }

    protected boolean emitCompareMemoryConOp(AMD64Assembler.OperandSize __size, ConstantValue __a, AMD64AddressValue __b, LIRFrameState __state)
    {
        if (JavaConstant.isNull(__a.getConstant()))
        {
            append(new AMD64BinaryConsumer.MemoryConstOp(AMD64Assembler.AMD64BinaryArithmetic.CMP, __size, __b, 0, __state));
            return true;
        }
        else if (__a.getConstant() instanceof VMConstant && __size == AMD64Assembler.OperandSize.DWORD)
        {
            VMConstant __vc = (VMConstant) __a.getConstant();
            append(new AMD64BinaryConsumer.MemoryVMConstOp(AMD64Assembler.AMD64BinaryArithmetic.CMP.getMIOpcode(__size, false), __b, __vc, __state));
            return true;
        }
        else
        {
            long __value = __a.getJavaConstant().asLong();
            if (NumUtil.is32bit(__value))
            {
                append(new AMD64BinaryConsumer.MemoryConstOp(AMD64Assembler.AMD64BinaryArithmetic.CMP, __size, __b, (int) __value, __state));
                return true;
            }
            else
            {
                return emitCompareRegMemoryOp(__size, asAllocatable(__a), __b, __state);
            }
        }
    }

    private boolean emitCompareRegMemoryOp(AMD64Assembler.OperandSize __size, AllocatableValue __a, AMD64AddressValue __b, LIRFrameState __state)
    {
        AMD64Assembler.AMD64RMOp __op = AMD64Assembler.AMD64BinaryArithmetic.CMP.getRMOpcode(__size);
        append(new AMD64BinaryConsumer.MemoryRMOp(__op, __size, __a, __b, __state));
        return false;
    }

    ///
    // This method emits the compare instruction, and may reorder the operands. It returns true if
    // it did so.
    //
    // @param a the left operand of the comparison
    // @param b the right operand of the comparison
    // @param cond the condition of the comparison
    // @return true if the left and right operands were switched, false otherwise
    ///
    private Condition emitCompare(PlatformKind __cmpKind, Value __a, Value __b, Condition __cond)
    {
        if (LIRValueUtil.isVariable(__b))
        {
            emitRawCompare(__cmpKind, __b, __a);
            return __cond.mirror();
        }
        else
        {
            emitRawCompare(__cmpKind, __a, __b);
            return __cond;
        }
    }

    private void emitRawCompare(PlatformKind __cmpKind, Value __left, Value __right)
    {
        ((AMD64ArithmeticLIRGeneratorTool) this.___arithmeticLIRGen).emitCompareOp((AMD64Kind) __cmpKind, load(__left), loadNonConst(__right));
    }

    @Override
    public void emitMembar(int __barriers)
    {
        int __necessaryBarriers = target().arch.requiredBarriers(__barriers);
        if (target().isMP && __necessaryBarriers != 0)
        {
            append(new AMD64Move.MembarOp(__necessaryBarriers));
        }
    }

    public abstract void emitCCall(long __address, CallingConvention __nativeCallingConvention, Value[] __args, int __numberOfFloatingPointArguments);

    @Override
    protected void emitForeignCallOp(ForeignCallLinkage __linkage, Value __result, Value[] __arguments, Value[] __temps, LIRFrameState __info)
    {
        long __maxOffset = __linkage.getMaxCallTargetOffset();
        if (__maxOffset != (int) __maxOffset)
        {
            append(new AMD64Call.DirectFarForeignCallOp(__linkage, __result, __arguments, __temps, __info));
        }
        else
        {
            append(new AMD64Call.DirectNearForeignCallOp(__linkage, __result, __arguments, __temps, __info));
        }
    }

    @Override
    public Variable emitByteSwap(Value __input)
    {
        Variable __result = newVariable(LIRKind.combine(__input));
        append(new AMD64ByteSwapOp(__result, __input));
        return __result;
    }

    @Override
    public Variable emitArrayCompareTo(JavaKind __kind1, JavaKind __kind2, Value __array1, Value __array2, Value __length1, Value __length2)
    {
        LIRKind __resultKind = LIRKind.value(AMD64Kind.DWORD);
        RegisterValue __raxRes = AMD64.rax.asValue(__resultKind);
        RegisterValue __cnt1 = AMD64.rcx.asValue(__length1.getValueKind());
        RegisterValue __cnt2 = AMD64.rdx.asValue(__length2.getValueKind());
        emitMove(__cnt1, __length1);
        emitMove(__cnt2, __length2);
        append(new AMD64ArrayCompareToOp(this, __kind1, __kind2, __raxRes, __array1, __array2, __cnt1, __cnt2));
        Variable __result = newVariable(__resultKind);
        emitMove(__result, __raxRes);
        return __result;
    }

    @Override
    public Variable emitArrayEquals(JavaKind __kind, Value __array1, Value __array2, Value __length)
    {
        Variable __result = newVariable(LIRKind.value(AMD64Kind.DWORD));
        append(new AMD64ArrayEqualsOp(this, __kind, __result, __array1, __array2, asAllocatable(__length)));
        return __result;
    }

    @Override
    public void emitReturn(JavaKind __kind, Value __input)
    {
        AllocatableValue __operand = Value.ILLEGAL;
        if (__input != null)
        {
            __operand = resultOperandFor(__kind, __input.getValueKind());
            emitMove(__operand, __input);
        }
        append(new AMD64ControlFlow.ReturnOp(__operand));
    }

    protected AMD64ControlFlow.StrategySwitchOp createStrategySwitchOp(SwitchStrategy __strategy, LabelRef[] __keyTargets, LabelRef __defaultTarget, Variable __key, AllocatableValue __temp)
    {
        return new AMD64ControlFlow.StrategySwitchOp(__strategy, __keyTargets, __defaultTarget, __key, __temp);
    }

    @Override
    public void emitStrategySwitch(SwitchStrategy __strategy, Variable __key, LabelRef[] __keyTargets, LabelRef __defaultTarget)
    {
        // a temp is needed for loading object constants
        boolean __needsTemp = !LIRKind.isValue(__key);
        append(createStrategySwitchOp(__strategy, __keyTargets, __defaultTarget, __key, __needsTemp ? newVariable(__key.getValueKind()) : Value.ILLEGAL));
    }

    @Override
    protected void emitTableSwitch(int __lowKey, LabelRef __defaultTarget, LabelRef[] __targets, Value __key)
    {
        append(new AMD64ControlFlow.TableSwitchOp(__lowKey, __defaultTarget, __targets, __key, newVariable(LIRKind.value(target().arch.getWordKind())), newVariable(__key.getValueKind())));
    }

    public void emitLFence()
    {
        append(new AMD64LFenceOp());
    }
}
