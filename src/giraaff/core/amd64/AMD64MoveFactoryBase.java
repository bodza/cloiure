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
    private final BackupSlotProvider backupSlotProvider;

    // @class AMD64MoveFactoryBase.RegisterBackupPair
    private static final class RegisterBackupPair
    {
        public final Register register;
        public final VirtualStackSlot backupSlot;

        // @cons
        RegisterBackupPair(Register register, VirtualStackSlot backupSlot)
        {
            super();
            this.register = register;
            this.backupSlot = backupSlot;
        }
    }

    // @class AMD64MoveFactoryBase.BackupSlotProvider
    public static final class BackupSlotProvider
    {
        private final FrameMapBuilder frameMapBuilder;
        private EconomicMap<PlatformKind.Key, RegisterBackupPair> categorized;

        // @cons
        public BackupSlotProvider(FrameMapBuilder frameMapBuilder)
        {
            super();
            this.frameMapBuilder = frameMapBuilder;
        }

        protected RegisterBackupPair getScratchRegister(PlatformKind kind)
        {
            PlatformKind.Key key = kind.getKey();
            if (categorized == null)
            {
                categorized = EconomicMap.create(Equivalence.DEFAULT);
            }
            else if (categorized.containsKey(key))
            {
                return categorized.get(key);
            }

            RegisterConfig registerConfig = frameMapBuilder.getRegisterConfig();

            RegisterArray availableRegister = registerConfig.filterAllocatableRegisters(kind, registerConfig.getAllocatableRegisters());
            Register scratchRegister = availableRegister.get(0);

            Architecture arch = frameMapBuilder.getCodeCache().getTarget().arch;
            LIRKind largestKind = LIRKind.value(arch.getLargestStorableKind(scratchRegister.getRegisterCategory()));
            VirtualStackSlot backupSlot = frameMapBuilder.allocateSpillSlot(largestKind);

            RegisterBackupPair value = new RegisterBackupPair(scratchRegister, backupSlot);
            categorized.put(key, value);

            return value;
        }
    }

    // @cons
    public AMD64MoveFactoryBase(BackupSlotProvider backupSlotProvider)
    {
        super();
        this.backupSlotProvider = backupSlotProvider;
    }

    @Override
    public final AMD64LIRInstruction createStackMove(AllocatableValue result, AllocatableValue input)
    {
        AMD64Kind kind = (AMD64Kind) result.getPlatformKind();
        switch (kind.getSizeInBytes())
        {
            case 2:
                return new AMD64PushPopStackMove(OperandSize.WORD, result, input);
            case 8:
                return new AMD64PushPopStackMove(OperandSize.QWORD, result, input);
            default:
                RegisterBackupPair backup = backupSlotProvider.getScratchRegister(input.getPlatformKind());
                Register scratchRegister = backup.register;
                VirtualStackSlot backupSlot = backup.backupSlot;
                return createStackMove(result, input, scratchRegister, backupSlot);
        }
    }

    public abstract AMD64LIRInstruction createStackMove(AllocatableValue result, AllocatableValue input, Register scratchRegister, AllocatableValue backupSlot);
}
