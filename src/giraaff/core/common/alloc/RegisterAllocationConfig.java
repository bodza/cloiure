package giraaff.core.common.alloc;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.Register.RegisterCategory;
import jdk.vm.ci.code.RegisterArray;
import jdk.vm.ci.code.RegisterConfig;
import jdk.vm.ci.meta.PlatformKind;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;

///
// Configuration for register allocation. This is different to {@link RegisterConfig}.
///
// @class RegisterAllocationConfig
public class RegisterAllocationConfig
{
    // @class RegisterAllocationConfig.AllocatableRegisters
    public static final class AllocatableRegisters
    {
        // @field
        public final Register[] ___allocatableRegisters;
        // @field
        public final int ___minRegisterNumber;
        // @field
        public final int ___maxRegisterNumber;

        // @cons RegisterAllocationConfig.AllocatableRegisters
        public AllocatableRegisters(RegisterArray __allocatableRegisters, int __minRegisterNumber, int __maxRegisterNumber)
        {
            super();
            this.___allocatableRegisters = __allocatableRegisters.toArray();
            this.___minRegisterNumber = __minRegisterNumber;
            this.___maxRegisterNumber = __maxRegisterNumber;
        }
    }

    protected RegisterArray initAllocatable(RegisterArray __registers)
    {
        return __registers;
    }

    // @field
    protected final RegisterConfig ___registerConfig;
    // @field
    private final EconomicMap<PlatformKind.Key, RegisterAllocationConfig.AllocatableRegisters> ___categorized = EconomicMap.create(Equivalence.DEFAULT);
    // @field
    private RegisterArray ___cachedRegisters;

    // @cons RegisterAllocationConfig
    public RegisterAllocationConfig(RegisterConfig __registerConfig)
    {
        super();
        this.___registerConfig = __registerConfig;
    }

    ///
    // Gets the set of registers that can be used by the register allocator for a value of a particular kind.
    ///
    public RegisterAllocationConfig.AllocatableRegisters getAllocatableRegisters(PlatformKind __kind)
    {
        PlatformKind.Key __key = __kind.getKey();
        if (this.___categorized.containsKey(__key))
        {
            return this.___categorized.get(__key);
        }
        RegisterAllocationConfig.AllocatableRegisters __ret = createAllocatableRegisters(this.___registerConfig.filterAllocatableRegisters(__kind, getAllocatableRegisters()));
        this.___categorized.put(__key, __ret);
        return __ret;
    }

    ///
    // Gets the {@link RegisterCategory} for the given {@link PlatformKind}.
    ///
    public RegisterCategory getRegisterCategory(PlatformKind __kind)
    {
        return getAllocatableRegisters(__kind).___allocatableRegisters[0].getRegisterCategory();
    }

    protected RegisterAllocationConfig.AllocatableRegisters createAllocatableRegisters(RegisterArray __registers)
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
        return new RegisterAllocationConfig.AllocatableRegisters(__registers, __min, __max);
    }

    ///
    // Gets the set of registers that can be used by the register allocator.
    ///
    public RegisterArray getAllocatableRegisters()
    {
        if (this.___cachedRegisters == null)
        {
            this.___cachedRegisters = initAllocatable(this.___registerConfig.getAllocatableRegisters());
        }
        return this.___cachedRegisters;
    }

    public RegisterConfig getRegisterConfig()
    {
        return this.___registerConfig;
    }
}
