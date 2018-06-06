package giraaff.core.amd64;

import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.code.Architecture;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterArray;
import jdk.vm.ci.code.RegisterConfig;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.PlatformKind;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;

import giraaff.asm.amd64.AMD64Assembler;
import giraaff.core.common.LIRKind;
import giraaff.lir.VirtualStackSlot;
import giraaff.lir.amd64.AMD64LIRInstruction;
import giraaff.lir.amd64.AMD64Move;
import giraaff.lir.framemap.FrameMapBuilder;
import giraaff.lir.gen.LIRGeneratorTool;

// @class AMD64MoveFactoryBase
public abstract class AMD64MoveFactoryBase implements LIRGeneratorTool.MoveFactory
{
    // @field
    private final AMD64MoveFactoryBase.BackupSlotProvider ___backupSlotProvider;

    // @class AMD64MoveFactoryBase.RegisterBackupPair
    private static final class RegisterBackupPair
    {
        // @field
        public final Register ___register;
        // @field
        public final VirtualStackSlot ___backupSlot;

        // @cons AMD64MoveFactoryBase.RegisterBackupPair
        RegisterBackupPair(Register __register, VirtualStackSlot __backupSlot)
        {
            super();
            this.___register = __register;
            this.___backupSlot = __backupSlot;
        }
    }

    // @class AMD64MoveFactoryBase.BackupSlotProvider
    public static final class BackupSlotProvider
    {
        // @field
        private final FrameMapBuilder ___frameMapBuilder;
        // @field
        private EconomicMap<PlatformKind.Key, AMD64MoveFactoryBase.RegisterBackupPair> ___categorized;

        // @cons AMD64MoveFactoryBase.BackupSlotProvider
        public BackupSlotProvider(FrameMapBuilder __frameMapBuilder)
        {
            super();
            this.___frameMapBuilder = __frameMapBuilder;
        }

        protected AMD64MoveFactoryBase.RegisterBackupPair getScratchRegister(PlatformKind __kind)
        {
            PlatformKind.Key __key = __kind.getKey();
            if (this.___categorized == null)
            {
                this.___categorized = EconomicMap.create(Equivalence.DEFAULT);
            }
            else if (this.___categorized.containsKey(__key))
            {
                return this.___categorized.get(__key);
            }

            RegisterConfig __registerConfig = this.___frameMapBuilder.getRegisterConfig();

            RegisterArray __availableRegister = __registerConfig.filterAllocatableRegisters(__kind, __registerConfig.getAllocatableRegisters());
            Register __scratchRegister = __availableRegister.get(0);

            Architecture __arch = this.___frameMapBuilder.getCodeCache().getTarget().arch;
            LIRKind __largestKind = LIRKind.value(__arch.getLargestStorableKind(__scratchRegister.getRegisterCategory()));
            VirtualStackSlot __backupSlot = this.___frameMapBuilder.allocateSpillSlot(__largestKind);

            AMD64MoveFactoryBase.RegisterBackupPair __value = new AMD64MoveFactoryBase.RegisterBackupPair(__scratchRegister, __backupSlot);
            this.___categorized.put(__key, __value);

            return __value;
        }
    }

    // @cons AMD64MoveFactoryBase
    public AMD64MoveFactoryBase(AMD64MoveFactoryBase.BackupSlotProvider __backupSlotProvider)
    {
        super();
        this.___backupSlotProvider = __backupSlotProvider;
    }

    @Override
    public final AMD64LIRInstruction createStackMove(AllocatableValue __result, AllocatableValue __input)
    {
        AMD64Kind __kind = (AMD64Kind) __result.getPlatformKind();
        switch (__kind.getSizeInBytes())
        {
            case 2:
                return new AMD64Move.AMD64PushPopStackMove(AMD64Assembler.OperandSize.WORD, __result, __input);
            case 8:
                return new AMD64Move.AMD64PushPopStackMove(AMD64Assembler.OperandSize.QWORD, __result, __input);
            default:
            {
                AMD64MoveFactoryBase.RegisterBackupPair __backup = this.___backupSlotProvider.getScratchRegister(__input.getPlatformKind());
                Register __scratchRegister = __backup.___register;
                VirtualStackSlot __backupSlot = __backup.___backupSlot;
                return createStackMove(__result, __input, __scratchRegister, __backupSlot);
            }
        }
    }

    public abstract AMD64LIRInstruction createStackMove(AllocatableValue __result, AllocatableValue __input, Register __scratchRegister, AllocatableValue __backupSlot);
}
