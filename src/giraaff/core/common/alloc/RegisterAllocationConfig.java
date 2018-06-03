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
// @class RegisterAllocationConfig
public class RegisterAllocationConfig
{
    // @class RegisterAllocationConfig.AllocatableRegisters
    public static final class AllocatableRegisters
    {
        // @field
        public final Register[] allocatableRegisters;
        // @field
        public final int minRegisterNumber;
        // @field
        public final int maxRegisterNumber;

        // @cons
        public AllocatableRegisters(RegisterArray __allocatableRegisters, int __minRegisterNumber, int __maxRegisterNumber)
        {
            super();
            this.allocatableRegisters = __allocatableRegisters.toArray();
            this.minRegisterNumber = __minRegisterNumber;
            this.maxRegisterNumber = __maxRegisterNumber;
        }
    }

    protected RegisterArray initAllocatable(RegisterArray __registers)
    {
        return __registers;
    }

    // @field
    protected final RegisterConfig registerConfig;
    // @field
    private final EconomicMap<PlatformKind.Key, AllocatableRegisters> categorized = EconomicMap.create(Equivalence.DEFAULT);
    // @field
    private RegisterArray cachedRegisters;

    // @cons
    public RegisterAllocationConfig(RegisterConfig __registerConfig)
    {
        super();
        this.registerConfig = __registerConfig;
    }

    /**
     * Gets the set of registers that can be used by the register allocator for a value of a particular kind.
     */
    public AllocatableRegisters getAllocatableRegisters(PlatformKind __kind)
    {
        PlatformKind.Key __key = __kind.getKey();
        if (categorized.containsKey(__key))
        {
            return categorized.get(__key);
        }
        AllocatableRegisters __ret = createAllocatableRegisters(registerConfig.filterAllocatableRegisters(__kind, getAllocatableRegisters()));
        categorized.put(__key, __ret);
        return __ret;
    }

    /**
     * Gets the {@link RegisterCategory} for the given {@link PlatformKind}.
     */
    public RegisterCategory getRegisterCategory(PlatformKind __kind)
    {
        return getAllocatableRegisters(__kind).allocatableRegisters[0].getRegisterCategory();
    }

    protected AllocatableRegisters createAllocatableRegisters(RegisterArray __registers)
    {
        int __min = Integer.MAX_VALUE;
        int __max = Integer.MIN_VALUE;
        for (Register __reg : __registers)
        {
            int __number = __reg.number;
            if (__number < __min)
            {
                __min = __number;
            }
            if (__number > __max)
            {
                __max = __number;
            }
        }
        return new AllocatableRegisters(__registers, __min, __max);
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
