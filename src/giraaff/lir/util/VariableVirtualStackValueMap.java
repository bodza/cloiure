package giraaff.lir.util;

import jdk.vm.ci.meta.Value;

import giraaff.debug.GraalError;
import giraaff.lir.LIRValueUtil;

public class VariableVirtualStackValueMap<K extends Value, T> extends ValueMap<K, T>
{
    private final Object[] variables;
    private final Object[] slots;

    public VariableVirtualStackValueMap(int initialVariableCapacity, int initialStackSlotCapacity)
    {
        variables = new Object[initialVariableCapacity];
        slots = new Object[initialStackSlotCapacity];
    }

    @Override
    public T get(K value)
    {
        if (LIRValueUtil.isVariable(value))
        {
            return get(variables, LIRValueUtil.asVariable(value).index);
        }
        if (LIRValueUtil.isVirtualStackSlot(value))
        {
            return get(slots, LIRValueUtil.asVirtualStackSlot(value).getId());
        }
        throw GraalError.shouldNotReachHere("Unsupported Value: " + value);
    }

    @Override
    public void remove(K value)
    {
        if (LIRValueUtil.isVariable(value))
        {
            remove(variables, LIRValueUtil.asVariable(value).index);
        }
        else if (LIRValueUtil.isVirtualStackSlot(value))
        {
            remove(slots, LIRValueUtil.asVirtualStackSlot(value).getId());
        }
        else
        {
            throw GraalError.shouldNotReachHere("Unsupported Value: " + value);
        }
    }

    @Override
    public void put(K value, T object)
    {
        if (LIRValueUtil.isVariable(value))
        {
            put(variables, LIRValueUtil.asVariable(value).index, object);
        }
        else if (LIRValueUtil.isVirtualStackSlot(value))
        {
            put(slots, LIRValueUtil.asVirtualStackSlot(value).getId(), object);
        }
        else
        {
            throw GraalError.shouldNotReachHere("Unsupported Value: " + value);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T get(Object[] array, int index)
    {
        if (index >= array.length)
        {
            return null;
        }
        return (T) array[index];
    }

    private static void remove(Object[] array, int index)
    {
        if (index >= array.length)
        {
            return;
        }
        array[index] = null;
    }

    private static <T> Object[] put(Object[] array, int index, T object)
    {
        if (index >= array.length)
        {
            Object[] newArray = new Object[index + 1];
            System.arraycopy(array, 0, newArray, 0, array.length);
            newArray[index] = object;
            return newArray;
        }
        array[index] = object;
        return null;
    }
}
