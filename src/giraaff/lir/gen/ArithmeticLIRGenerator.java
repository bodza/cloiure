package giraaff.lir.gen;

import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.PlatformKind;
import jdk.vm.ci.meta.Value;

import giraaff.core.common.LIRKind;
import giraaff.lir.Variable;

/**
 * This class traverses the HIR instructions and generates LIR instructions from them.
 */
// @class ArithmeticLIRGenerator
public abstract class ArithmeticLIRGenerator implements ArithmeticLIRGeneratorTool
{
    // @field
    LIRGenerator lirGen;

    public LIRGenerator getLIRGen()
    {
        return lirGen;
    }

    protected abstract boolean isNumericInteger(PlatformKind kind);

    protected abstract Variable emitAdd(LIRKind resultKind, Value a, Value b, boolean setFlags);

    protected abstract Variable emitSub(LIRKind resultKind, Value a, Value b, boolean setFlags);

    @Override
    public final Variable emitAdd(Value __aVal, Value __bVal, boolean __setFlags)
    {
        return emitAddOrSub(__aVal, __bVal, __setFlags, true);
    }

    @Override
    public final Variable emitSub(Value __aVal, Value __bVal, boolean __setFlags)
    {
        return emitAddOrSub(__aVal, __bVal, __setFlags, false);
    }

    private Variable emitAddOrSub(Value __aVal, Value __bVal, boolean __setFlags, boolean __isAdd)
    {
        LIRKind __resultKind;
        Value __a = __aVal;
        Value __b = __bVal;

        if (isNumericInteger(__a.getPlatformKind()))
        {
            LIRKind __aKind = __a.getValueKind(LIRKind.class);
            LIRKind __bKind = __b.getValueKind(LIRKind.class);

            if (__aKind.isUnknownReference())
            {
                __resultKind = __aKind;
            }
            else if (__bKind.isUnknownReference())
            {
                __resultKind = __bKind;
            }
            else if (__aKind.isValue() && __bKind.isValue())
            {
                __resultKind = __aKind;
            }
            else if (__aKind.isValue())
            {
                if (__bKind.isDerivedReference())
                {
                    __resultKind = __bKind;
                }
                else
                {
                    AllocatableValue __allocatable = getLIRGen().asAllocatable(__b);
                    __resultKind = __bKind.makeDerivedReference(__allocatable);
                    __b = __allocatable;
                }
            }
            else if (__bKind.isValue())
            {
                if (__aKind.isDerivedReference())
                {
                    __resultKind = __aKind;
                }
                else
                {
                    AllocatableValue __allocatable = getLIRGen().asAllocatable(__a);
                    __resultKind = __aKind.makeDerivedReference(__allocatable);
                    __a = __allocatable;
                }
            }
            else
            {
                __resultKind = __aKind.makeUnknownReference();
            }
        }
        else
        {
            __resultKind = LIRKind.combine(__a, __b);
        }

        return __isAdd ? emitAdd(__resultKind, __a, __b, __setFlags) : emitSub(__resultKind, __a, __b, __setFlags);
    }
}
