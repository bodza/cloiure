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

public final class LIRValueUtil
{
    public static boolean isVariable(Value value)
    {
        return value instanceof Variable;
    }

    public static Variable asVariable(Value value)
    {
        return (Variable) value;
    }

    public static boolean isConstantValue(Value value)
    {
        return value instanceof ConstantValue;
    }

    public static ConstantValue asConstantValue(Value value)
    {
        return (ConstantValue) value;
    }

    public static Constant asConstant(Value value)
    {
        return asConstantValue(value).getConstant();
    }

    public static boolean isJavaConstant(Value value)
    {
        return isConstantValue(value) && asConstantValue(value).isJavaConstant();
    }

    public static JavaConstant asJavaConstant(Value value)
    {
        return asConstantValue(value).getJavaConstant();
    }

    public static boolean isIntConstant(Value value, long expected)
    {
        if (isJavaConstant(value))
        {
            JavaConstant javaConstant = asJavaConstant(value);
            if (javaConstant != null && javaConstant.getJavaKind().isNumericInteger())
            {
                return javaConstant.asLong() == expected;
            }
        }
        return false;
    }

    public static boolean isStackSlotValue(Value value)
    {
        return value instanceof StackSlot || value instanceof VirtualStackSlot;
    }

    public static boolean isVirtualStackSlot(Value value)
    {
        return value instanceof VirtualStackSlot;
    }

    public static VirtualStackSlot asVirtualStackSlot(Value value)
    {
        return (VirtualStackSlot) value;
    }

    public static boolean sameRegister(Value v1, Value v2)
    {
        return ValueUtil.isRegister(v1) && ValueUtil.isRegister(v2) && ValueUtil.asRegister(v1).equals(ValueUtil.asRegister(v2));
    }

    public static boolean sameRegister(Value v1, Value v2, Value v3)
    {
        return sameRegister(v1, v2) && sameRegister(v1, v3);
    }

    /**
     * Checks if all the provided values are different physical registers. The parameters can be
     * either {@link Register registers}, {@link Value values} or arrays of them. All values that
     * are not {@link RegisterValue registers} are ignored.
     */
    public static boolean differentRegisters(Object... values)
    {
        List<Register> registers = collectRegisters(values, new ArrayList<Register>());
        for (int i = 1; i < registers.size(); i++)
        {
            Register r1 = registers.get(i);
            for (int j = 0; j < i; j++)
            {
                Register r2 = registers.get(j);
                if (r1.equals(r2))
                {
                    return false;
                }
            }
        }
        return true;
    }

    private static List<Register> collectRegisters(Object[] values, List<Register> registers)
    {
        for (Object o : values)
        {
            if (o instanceof Register)
            {
                registers.add((Register) o);
            }
            else if (o instanceof Value)
            {
                if (ValueUtil.isRegister((Value) o))
                {
                    registers.add(ValueUtil.asRegister((Value) o));
                }
            }
            else if (o instanceof Object[])
            {
                collectRegisters((Object[]) o, registers);
            }
            else
            {
                throw new IllegalArgumentException("Not a Register or Value: " + o);
            }
        }
        return registers;
    }

    /**
     * Subtract sets of registers (x - y).
     *
     * @param x a set of register to subtract from.
     * @param y a set of registers to subtract.
     * @return resulting set of registers (x - y).
     */
    public static Value[] subtractRegisters(Value[] x, Value[] y)
    {
        ArrayList<Value> result = new ArrayList<>(x.length);
        for (Value i : x)
        {
            boolean append = true;
            for (Value j : y)
            {
                if (sameRegister(i, j))
                {
                    append = false;
                    break;
                }
            }
            if (append)
            {
                result.add(i);
            }
        }
        Value[] resultArray = new Value[result.size()];
        return result.toArray(resultArray);
    }
}
