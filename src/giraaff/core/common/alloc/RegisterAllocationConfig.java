package giraaff.core.common.alloc;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.Register.RegisterCategory;
import jdk.vm.ci.code.RegisterArray;
import jdk.vm.ci.code.RegisterConfig;
import jdk.vm.ci.meta.PlatformKind;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;

import giraaff.core.common.GraalOptions;

/**
 * Configuration for register allocation. This is different to {@link RegisterConfig} as it only
 * returns registers specified by {@link GraalOptions#RegisterPressure}.
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

    public static final String ALL_REGISTERS = "<all>";

    private static Register findRegister(String name, RegisterArray all)
    {
        for (Register reg : all)
        {
            if (reg.name.equals(name))
            {
                return reg;
            }
        }
        throw new IllegalArgumentException("register " + name + " is not allocatable");
    }

    protected RegisterArray initAllocatable(RegisterArray registers)
    {
        if (allocationRestrictedTo != null)
        {
            Register[] regs = new Register[allocationRestrictedTo.length];
            for (int i = 0; i < allocationRestrictedTo.length; i++)
            {
                regs[i] = findRegister(allocationRestrictedTo[i], registers);
            }
            return new RegisterArray(regs);
        }

        return registers;
    }

    protected final RegisterConfig registerConfig;
    private final EconomicMap<PlatformKind.Key, AllocatableRegisters> categorized = EconomicMap.create(Equivalence.DEFAULT);
    private final String[] allocationRestrictedTo;
    private RegisterArray cachedRegisters;

    /**
     * @param allocationRestrictedTo if not {@code null}, register allocation will be restricted to
     *            registers whose names appear in this array
     */
    public RegisterAllocationConfig(RegisterConfig registerConfig, String[] allocationRestrictedTo)
    {
        this.registerConfig = registerConfig;
        this.allocationRestrictedTo = allocationRestrictedTo;
    }

    /**
     * Gets the set of registers that can be used by the register allocator for a value of a
     * particular kind.
     */
    public AllocatableRegisters getAllocatableRegisters(PlatformKind kind)
    {
        PlatformKind.Key key = kind.getKey();
        if (categorized.containsKey(key))
        {
            AllocatableRegisters val = categorized.get(key);
            return val;
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
