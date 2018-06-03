package giraaff.lir.util;

import java.util.function.BiConsumer;

import jdk.vm.ci.code.Architecture;
import jdk.vm.ci.code.Register;

// @class RegisterMap
public final class RegisterMap<T>
{
    // @field
    private final Object[] values;
    // @field
    private final Architecture arch;

    // @cons
    public RegisterMap(Architecture __arch)
    {
        super();
        this.values = new Object[__arch.getRegisters().size()];
        this.arch = __arch;
    }

    @SuppressWarnings("unchecked")
    public T get(Register __reg)
    {
        return (T) values[index(__reg)];
    }

    public void remove(Register __reg)
    {
        values[index(__reg)] = null;
    }

    public void put(Register __reg, T __value)
    {
        values[index(__reg)] = __value;
    }

    @SuppressWarnings("unchecked")
    public void forEach(BiConsumer<? super Register, ? super T> __consumer)
    {
        for (int __i = 0; __i < values.length; ++__i)
        {
            T __value = (T) values[__i];
            if (__value != null)
            {
                __consumer.accept(arch.getRegisters().get(__i), __value);
            }
        }
    }

    private static int index(Register __reg)
    {
        return __reg.number;
    }
}
