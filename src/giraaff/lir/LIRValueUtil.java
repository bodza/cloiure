package giraaff.lir;

import java.util.ArrayList;
import java.util.List;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.Value;

// @class LIRValueUtil
public final class LIRValueUtil
{
    public static boolean isVariable(Value __value)
    {
        return __value instanceof Variable;
    }

    public static Variable asVariable(Value __value)
    {
        return (Variable) __value;
    }

    public static boolean isConstantValue(Value __value)
    {
        return __value instanceof ConstantValue;
    }

    public static ConstantValue asConstantValue(Value __value)
    {
        return (ConstantValue) __value;
    }

    public static Constant asConstant(Value __value)
    {
        return asConstantValue(__value).getConstant();
    }

    public static boolean isJavaConstant(Value __value)
    {
        return isConstantValue(__value) && asConstantValue(__value).isJavaConstant();
    }

    public static JavaConstant asJavaConstant(Value __value)
    {
        return asConstantValue(__value).getJavaConstant();
    }

    public static boolean isIntConstant(Value __value, long __expected)
    {
        if (isJavaConstant(__value))
        {
            JavaConstant __javaConstant = asJavaConstant(__value);
            if (__javaConstant != null && __javaConstant.getJavaKind().isNumericInteger())
            {
                return __javaConstant.asLong() == __expected;
            }
        }
        return false;
    }

    public static boolean isStackSlotValue(Value __value)
    {
        return __value instanceof StackSlot || __value instanceof VirtualStackSlot;
    }

    public static boolean isVirtualStackSlot(Value __value)
    {
        return __value instanceof VirtualStackSlot;
    }

    public static VirtualStackSlot asVirtualStackSlot(Value __value)
    {
        return (VirtualStackSlot) __value;
    }

    public static boolean sameRegister(Value __v1, Value __v2)
    {
        return ValueUtil.isRegister(__v1) && ValueUtil.isRegister(__v2) && ValueUtil.asRegister(__v1).equals(ValueUtil.asRegister(__v2));
    }

    public static boolean sameRegister(Value __v1, Value __v2, Value __v3)
    {
        return sameRegister(__v1, __v2) && sameRegister(__v1, __v3);
    }

    /**
     * Checks if all the provided values are different physical registers. The parameters can be
     * either {@link Register registers}, {@link Value values} or arrays of them. All values that
     * are not {@link RegisterValue registers} are ignored.
     */
    public static boolean differentRegisters(Object... __values)
    {
        List<Register> __registers = collectRegisters(__values, new ArrayList<Register>());
        for (int __i = 1; __i < __registers.size(); __i++)
        {
            Register __r1 = __registers.get(__i);
            for (int __j = 0; __j < __i; __j++)
            {
                Register __r2 = __registers.get(__j);
                if (__r1.equals(__r2))
                {
                    return false;
                }
            }
        }
        return true;
    }

    private static List<Register> collectRegisters(Object[] __values, List<Register> __registers)
    {
        for (Object __o : __values)
        {
            if (__o instanceof Register)
            {
                __registers.add((Register) __o);
            }
            else if (__o instanceof Value)
            {
                if (ValueUtil.isRegister((Value) __o))
                {
                    __registers.add(ValueUtil.asRegister((Value) __o));
                }
            }
            else if (__o instanceof Object[])
            {
                collectRegisters((Object[]) __o, __registers);
            }
            else
            {
                throw new IllegalArgumentException("Not a Register or Value: " + __o);
            }
        }
        return __registers;
    }

    /**
     * Subtract sets of registers (x - y).
     *
     * @param x a set of register to subtract from.
     * @param y a set of registers to subtract.
     * @return resulting set of registers (x - y).
     */
    public static Value[] subtractRegisters(Value[] __x, Value[] __y)
    {
        ArrayList<Value> __result = new ArrayList<>(__x.length);
        for (Value __i : __x)
        {
            boolean __append = true;
            for (Value __j : __y)
            {
                if (sameRegister(__i, __j))
                {
                    __append = false;
                    break;
                }
            }
            if (__append)
            {
                __result.add(__i);
            }
        }
        Value[] __resultArray = new Value[__result.size()];
        return __result.toArray(__resultArray);
    }
}
