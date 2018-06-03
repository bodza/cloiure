package giraaff.lir.util;

import jdk.vm.ci.meta.Value;

import giraaff.lir.LIRValueUtil;
import giraaff.util.GraalError;

// @class VariableVirtualStackValueMap
public final class VariableVirtualStackValueMap<K extends Value, T> extends ValueMap<K, T>
{
    // @field
    private final Object[] variables;
    // @field
    private final Object[] slots;

    // @cons
    public VariableVirtualStackValueMap(int __initialVariableCapacity, int __initialStackSlotCapacity)
    {
        super();
        variables = new Object[__initialVariableCapacity];
        slots = new Object[__initialStackSlotCapacity];
    }

    @Override
    public T get(K __value)
    {
        if (LIRValueUtil.isVariable(__value))
        {
            return get(variables, LIRValueUtil.asVariable(__value).index);
        }
        if (LIRValueUtil.isVirtualStackSlot(__value))
        {
            return get(slots, LIRValueUtil.asVirtualStackSlot(__value).getId());
        }
        throw GraalError.shouldNotReachHere("Unsupported Value: " + __value);
    }

    @Override
    public void remove(K __value)
    {
        if (LIRValueUtil.isVariable(__value))
        {
            remove(variables, LIRValueUtil.asVariable(__value).index);
        }
        else if (LIRValueUtil.isVirtualStackSlot(__value))
        {
            remove(slots, LIRValueUtil.asVirtualStackSlot(__value).getId());
        }
        else
        {
            throw GraalError.shouldNotReachHere("Unsupported Value: " + __value);
        }
    }

    @Override
    public void put(K __value, T __object)
    {
        if (LIRValueUtil.isVariable(__value))
        {
            put(variables, LIRValueUtil.asVariable(__value).index, __object);
        }
        else if (LIRValueUtil.isVirtualStackSlot(__value))
        {
            put(slots, LIRValueUtil.asVirtualStackSlot(__value).getId(), __object);
        }
        else
        {
            throw GraalError.shouldNotReachHere("Unsupported Value: " + __value);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T get(Object[] __array, int __index)
    {
        if (__index >= __array.length)
        {
            return null;
        }
        return (T) __array[__index];
    }

    private static void remove(Object[] __array, int __index)
    {
        if (__index >= __array.length)
        {
            return;
        }
        __array[__index] = null;
    }

    private static <T> Object[] put(Object[] __array, int __index, T __object)
    {
        if (__index >= __array.length)
        {
            Object[] __newArray = new Object[__index + 1];
            System.arraycopy(__array, 0, __newArray, 0, __array.length);
            __newArray[__index] = __object;
            return __newArray;
        }
        __array[__index] = __object;
        return null;
    }
}
