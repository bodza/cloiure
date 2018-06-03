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

import giraaff.asm.amd64.AMD64Assembler.OperandSize;
import giraaff.core.common.LIRKind;
import giraaff.lir.VirtualStackSlot;
import giraaff.lir.amd64.AMD64LIRInstruction;
import giraaff.lir.amd64.AMD64Move.AMD64PushPopStackMove;
import giraaff.lir.framemap.FrameMapBuilder;
import giraaff.lir.gen.LIRGeneratorTool.MoveFactory;

// @class AMD64MoveFactoryBase
public abstract class AMD64MoveFactoryBase implements MoveFactory
{
    // @field
    private final BackupSlotProvider backupSlotProvider;

    // @class AMD64MoveFactoryBase.RegisterBackupPair
    private static final class RegisterBackupPair
    {
        // @field
        public final Register register;
        // @field
        public final VirtualStackSlot backupSlot;

        // @cons
        RegisterBackupPair(Register __register, VirtualStackSlot __backupSlot)
        {
            super();
            this.register = __register;
            this.backupSlot = __backupSlot;
        }
    }

    // @class AMD64MoveFactoryBase.BackupSlotProvider
    public static final class BackupSlotProvider
    {
        // @field
        private final FrameMapBuilder frameMapBuilder;
        // @field
        private EconomicMap<PlatformKind.Key, RegisterBackupPair> categorized;

        // @cons
        public BackupSlotProvider(FrameMapBuilder __frameMapBuilder)
        {
            super();
            this.frameMapBuilder = __frameMapBuilder;
        }

        protected RegisterBackupPair getScratchRegister(PlatformKind __kind)
        {
            PlatformKind.Key __key = __kind.getKey();
            if (categorized == null)
            {
                categorized = EconomicMap.create(Equivalence.DEFAULT);
            }
            else if (categorized.containsKey(__key))
            {
                return categorized.get(__key);
            }

            RegisterConfig __registerConfig = frameMapBuilder.getRegisterConfig();

            RegisterArray __availableRegister = __registerConfig.filterAllocatableRegisters(__kind, __registerConfig.getAllocatableRegisters());
            Register __scratchRegister = __availableRegister.get(0);

            Architecture __arch = frameMapBuilder.getCodeCache().getTarget().arch;
            LIRKind __largestKind = LIRKind.value(__arch.getLargestStorableKind(__scratchRegister.getRegisterCategory()));
            VirtualStackSlot __backupSlot = frameMapBuilder.allocateSpillSlot(__largestKind);

            RegisterBackupPair __value = new RegisterBackupPair(__scratchRegister, __backupSlot);
            categorized.put(__key, __value);

            return __value;
        }
    }

    // @cons
    public AMD64MoveFactoryBase(BackupSlotProvider __backupSlotProvider)
    {
        super();
        this.backupSlotProvider = __backupSlotProvider;
    }

    @Override
    public final AMD64LIRInstruction createStackMove(AllocatableValue __result, AllocatableValue __input)
    {
        AMD64Kind __kind = (AMD64Kind) __result.getPlatformKind();
        switch (__kind.getSizeInBytes())
        {
            case 2:
                return new AMD64PushPopStackMove(OperandSize.WORD, __result, __input);
            case 8:
                return new AMD64PushPopStackMove(OperandSize.QWORD, __result, __input);
            default:
            {
                RegisterBackupPair __backup = backupSlotProvider.getScratchRegister(__input.getPlatformKind());
                Register __scratchRegister = __backup.register;
                VirtualStackSlot __backupSlot = __backup.backupSlot;
                return createStackMove(__result, __input, __scratchRegister, __backupSlot);
            }
        }
    }

    public abstract AMD64LIRInstruction createStackMove(AllocatableValue result, AllocatableValue input, Register scratchRegister, AllocatableValue backupSlot);
}
