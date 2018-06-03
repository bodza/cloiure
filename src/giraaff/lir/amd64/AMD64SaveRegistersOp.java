package giraaff.lir.amd64;

import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterSaveLayout;
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;

import org.graalvm.collections.EconomicSet;

import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.StandardOp.SaveRegistersOp;
import giraaff.lir.asm.CompilationResultBuilder;
import giraaff.lir.framemap.FrameMap;

///
// Saves registers to stack slots.
///
@Opcode
// @class AMD64SaveRegistersOp
public final class AMD64SaveRegistersOp extends AMD64LIRInstruction implements SaveRegistersOp
{
    // @def
    public static final LIRInstructionClass<AMD64SaveRegistersOp> TYPE = LIRInstructionClass.create(AMD64SaveRegistersOp.class);

    ///
    // The registers (potentially) saved by this operation.
    ///
    // @field
    protected final Register[] ___savedRegisters;

    ///
    // The slots to which the registers are saved.
    ///
    @Def(OperandFlag.STACK)
    // @field
    protected final AllocatableValue[] ___slots;

    ///
    // Specifies if {@link #remove(EconomicSet)} should have an effect.
    ///
    // @field
    protected final boolean ___supportsRemove;

    ///
    // @param savedRegisters the registers saved by this operation which may be subject to {@linkplain #remove(EconomicSet) pruning}
    // @param savedRegisterLocations the slots to which the registers are saved
    // @param supportsRemove determines if registers can be {@linkplain #remove(EconomicSet) pruned}
    ///
    // @cons
    public AMD64SaveRegistersOp(Register[] __savedRegisters, AllocatableValue[] __savedRegisterLocations, boolean __supportsRemove)
    {
        this(TYPE, __savedRegisters, __savedRegisterLocations, __supportsRemove);
    }

    // @cons
    public AMD64SaveRegistersOp(LIRInstructionClass<? extends AMD64SaveRegistersOp> __c, Register[] __savedRegisters, AllocatableValue[] __savedRegisterLocations, boolean __supportsRemove)
    {
        super(__c);
        this.___savedRegisters = __savedRegisters;
        this.___slots = __savedRegisterLocations;
        this.___supportsRemove = __supportsRemove;
    }

    protected void saveRegister(CompilationResultBuilder __crb, AMD64MacroAssembler __masm, StackSlot __result, Register __input)
    {
        AMD64Move.reg2stack((AMD64Kind) __result.getPlatformKind(), __crb, __masm, __result, __input);
    }

    @Override
    public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
    {
        for (int __i = 0; __i < this.___savedRegisters.length; __i++)
        {
            if (this.___savedRegisters[__i] != null)
            {
                saveRegister(__crb, __masm, ValueUtil.asStackSlot(this.___slots[__i]), this.___savedRegisters[__i]);
            }
        }
    }

    public AllocatableValue[] getSlots()
    {
        return this.___slots;
    }

    @Override
    public boolean supportsRemove()
    {
        return this.___supportsRemove;
    }

    @Override
    public int remove(EconomicSet<Register> __doNotSave)
    {
        if (!this.___supportsRemove)
        {
            throw new UnsupportedOperationException();
        }
        return prune(__doNotSave, this.___savedRegisters);
    }

    static int prune(EconomicSet<Register> __toRemove, Register[] __registers)
    {
        int __pruned = 0;
        for (int __i = 0; __i < __registers.length; __i++)
        {
            if (__registers[__i] != null)
            {
                if (__toRemove.contains(__registers[__i]))
                {
                    __registers[__i] = null;
                    __pruned++;
                }
            }
        }
        return __pruned;
    }

    @Override
    public RegisterSaveLayout getMap(FrameMap __frameMap)
    {
        int __total = 0;
        for (int __i = 0; __i < this.___savedRegisters.length; __i++)
        {
            if (this.___savedRegisters[__i] != null)
            {
                __total++;
            }
        }
        Register[] __keys = new Register[__total];
        int[] __values = new int[__total];
        if (__total != 0)
        {
            int __mapIndex = 0;
            for (int __i = 0; __i < this.___savedRegisters.length; __i++)
            {
                if (this.___savedRegisters[__i] != null)
                {
                    __keys[__mapIndex] = this.___savedRegisters[__i];
                    StackSlot __slot = ValueUtil.asStackSlot(this.___slots[__i]);
                    __values[__mapIndex] = indexForStackSlot(__frameMap, __slot);
                    __mapIndex++;
                }
            }
        }
        return new RegisterSaveLayout(__keys, __values);
    }

    ///
    // Computes the index of a stack slot relative to slot 0. This is also the bit index of stack
    // slots in the reference map.
    //
    // @param slot a stack slot
    // @return the index of the stack slot
    ///
    private static int indexForStackSlot(FrameMap __frameMap, StackSlot __slot)
    {
        return __frameMap.offsetForStackSlot(__slot) / __frameMap.getTarget().wordSize;
    }
}
