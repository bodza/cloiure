package giraaff.lir.util;

import java.util.function.BiConsumer;

import jdk.vm.ci.code.Architecture;
import jdk.vm.ci.code.Register;

public class RegisterMap<T>
{
    private final Object[] values;
    private final Architecture architecture;

    public RegisterMap(Architecture arch)
    {
        this.values = new Object[arch.getRegisters().size()];
        this.architecture = arch;
    }

    @SuppressWarnings("unchecked")
    public T get(Register reg)
    {
        return (T) values[index(reg)];
    }

    public void remove(Register reg)
    {
        values[index(reg)] = null;
    }

    public void put(Register reg, T value)
    {
        values[index(reg)] = value;
    }

    @SuppressWarnings("unchecked")
    public void forEach(BiConsumer<? super Register, ? super T> consumer)
    {
        for (int i = 0; i < values.length; ++i)
        {
            T value = (T) values[i];
            if (value != null)
            {
                consumer.accept(architecture.getRegisters().get(i), value);
            }
        }
    }

    private static int index(Register reg)
    {
        return reg.number;
    }
}
