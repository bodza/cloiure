package graalvm.compiler.lir.amd64;

import static jdk.vm.ci.code.ValueUtil.asStackSlot;
import static jdk.vm.ci.code.ValueUtil.isStackSlot;
import static graalvm.compiler.lir.LIRInstruction.OperandFlag.STACK;

import java.util.Arrays;

import org.graalvm.collections.EconomicSet;
import graalvm.compiler.asm.amd64.AMD64MacroAssembler;
import graalvm.compiler.lir.LIRInstructionClass;
import graalvm.compiler.lir.LIRValueUtil;
import graalvm.compiler.lir.Opcode;
import graalvm.compiler.lir.StandardOp.SaveRegistersOp;
import graalvm.compiler.lir.asm.CompilationResultBuilder;
import graalvm.compiler.lir.framemap.FrameMap;

import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterSaveLayout;
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.meta.AllocatableValue;

/**
 * Saves registers to stack slots.
 */
@Opcode("SAVE_REGISTER")
public class AMD64SaveRegistersOp extends AMD64LIRInstruction implements SaveRegistersOp
{
    public static final LIRInstructionClass<AMD64SaveRegistersOp> TYPE = LIRInstructionClass.create(AMD64SaveRegistersOp.class);

    /**
     * The registers (potentially) saved by this operation.
     */
    protected final Register[] savedRegisters;

    /**
     * The slots to which the registers are saved.
     */
    @Def(STACK) protected final AllocatableValue[] slots;

    /**
     * Specifies if {@link #remove(EconomicSet)} should have an effect.
     */
    protected final boolean supportsRemove;

    /**
     *
     * @param savedRegisters the registers saved by this operation which may be subject to
     *            {@linkplain #remove(EconomicSet) pruning}
     * @param savedRegisterLocations the slots to which the registers are saved
     * @param supportsRemove determines if registers can be {@linkplain #remove(EconomicSet) pruned}
     */
    public AMD64SaveRegistersOp(Register[] savedRegisters, AllocatableValue[] savedRegisterLocations, boolean supportsRemove)
    {
        this(TYPE, savedRegisters, savedRegisterLocations, supportsRemove);
    }

    public AMD64SaveRegistersOp(LIRInstructionClass<? extends AMD64SaveRegistersOp> c, Register[] savedRegisters, AllocatableValue[] savedRegisterLocations, boolean supportsRemove)
    {
        super(c);
        this.savedRegisters = savedRegisters;
        this.slots = savedRegisterLocations;
        this.supportsRemove = supportsRemove;
    }

    protected void saveRegister(CompilationResultBuilder crb, AMD64MacroAssembler masm, StackSlot result, Register input)
    {
        AMD64Move.reg2stack((AMD64Kind) result.getPlatformKind(), crb, masm, result, input);
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
    {
        for (int i = 0; i < savedRegisters.length; i++)
        {
            if (savedRegisters[i] != null)
            {
                saveRegister(crb, masm, asStackSlot(slots[i]), savedRegisters[i]);
            }
        }
    }

    public AllocatableValue[] getSlots()
    {
        return slots;
    }

    @Override
    public boolean supportsRemove()
    {
        return supportsRemove;
    }

    @Override
    public int remove(EconomicSet<Register> doNotSave)
    {
        if (!supportsRemove)
        {
            throw new UnsupportedOperationException();
        }
        return prune(doNotSave, savedRegisters);
    }

    static int prune(EconomicSet<Register> toRemove, Register[] registers)
    {
        int pruned = 0;
        for (int i = 0; i < registers.length; i++)
        {
            if (registers[i] != null)
            {
                if (toRemove.contains(registers[i]))
                {
                    registers[i] = null;
                    pruned++;
                }
            }
        }
        return pruned;
    }

    @Override
    public RegisterSaveLayout getMap(FrameMap frameMap)
    {
        int total = 0;
        for (int i = 0; i < savedRegisters.length; i++)
        {
            if (savedRegisters[i] != null)
            {
                total++;
            }
        }
        Register[] keys = new Register[total];
        int[] values = new int[total];
        if (total != 0)
        {
            int mapIndex = 0;
            for (int i = 0; i < savedRegisters.length; i++)
            {
                if (savedRegisters[i] != null)
                {
                    keys[mapIndex] = savedRegisters[i];
                    StackSlot slot = asStackSlot(slots[i]);
                    values[mapIndex] = indexForStackSlot(frameMap, slot);
                    mapIndex++;
                }
            }
        }
        return new RegisterSaveLayout(keys, values);
    }

    /**
     * Computes the index of a stack slot relative to slot 0. This is also the bit index of stack
     * slots in the reference map.
     *
     * @param slot a stack slot
     * @return the index of the stack slot
     */
    private static int indexForStackSlot(FrameMap frameMap, StackSlot slot)
    {
        int value = frameMap.offsetForStackSlot(slot) / frameMap.getTarget().wordSize;
        return value;
    }
}
