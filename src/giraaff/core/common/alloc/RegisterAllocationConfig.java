package giraaff.core.common.alloc;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.Register.RegisterCategory;
import jdk.vm.ci.code.RegisterArray;
import jdk.vm.ci.code.RegisterConfig;
import jdk.vm.ci.meta.PlatformKind;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;

/**
 * Configuration for register allocation. This is different to {@link RegisterConfig}.
 */
public class RegisterAllocationConfig
{
    public static final class AllocatableRegisters
    {
        public final Register[] allocatableRegisters;
        public final int minRegisterNumber;
        public final int maxRegisterNumber;

        public AllocatableRegisters(RegisterArray allocatableRegisters, int minRegisterNumber, int maxRegisterNumber)
        {
            this.allocatableRegisters = allocatableRegisters.toArray();
            this.minRegisterNumber = minRegisterNumber;
            this.maxRegisterNumber = maxRegisterNumber;
        }
    }

    protected RegisterArray initAllocatable(RegisterArray registers)
    {
        return registers;
    }

    protected final RegisterConfig registerConfig;
    private final EconomicMap<PlatformKind.Key, AllocatableRegisters> categorized = EconomicMap.create(Equivalence.DEFAULT);
    private RegisterArray cachedRegisters;

    public RegisterAllocationConfig(RegisterConfig registerConfig)
    {
        this.registerConfig = registerConfig;
    }

    /**
     * Gets the set of registers that can be used by the register allocator for a value of a particular kind.
     */
    public AllocatableRegisters getAllocatableRegisters(PlatformKind kind)
    {
        PlatformKind.Key key = kind.getKey();
        if (categorized.containsKey(key))
        {
            return categorized.get(key);
        }
        AllocatableRegisters ret = createAllocatableRegisters(registerConfig.filterAllocatableRegisters(kind, getAllocatableRegisters()));
        categorized.put(key, ret);
        return ret;
    }

    /**
     * Gets the {@link RegisterCategory} for the given {@link PlatformKind}.
     */
    public RegisterCategory getRegisterCategory(PlatformKind kind)
    {
        return getAllocatableRegisters(kind).allocatableRegisters[0].getRegisterCategory();
    }

    protected AllocatableRegisters createAllocatableRegisters(RegisterArray registers)
    {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (Register reg : registers)
        {
            int number = reg.number;
            if (number < min)
            {
                min = number;
            }
            if (number > max)
            {
                max = number;
            }
        }
        return new AllocatableRegisters(registers, min, max);
    }

    /**
     * Gets the set of registers that can be used by the register allocator.
     */
    public RegisterArray getAllocatableRegisters()
    {
        if (cachedRegisters == null)
        {
            cachedRegisters = initAllocatable(registerConfig.getAllocatableRegisters());
        }
        return cachedRegisters;
    }

    public RegisterConfig getRegisterConfig()
    {
        return registerConfig;
    }
}
