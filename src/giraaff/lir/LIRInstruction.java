package giraaff.lir;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;

import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.Value;

import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstruction.OperandMode;
import giraaff.lir.StandardOp.LoadConstantOp;
import giraaff.lir.StandardOp.MoveOp;
import giraaff.lir.StandardOp.ValueMoveOp;
import giraaff.lir.asm.CompilationResultBuilder;
import giraaff.lir.gen.LIRGenerationResult;

/**
 * The base class for an {@code LIRInstruction}.
 */
// @class LIRInstruction
public abstract class LIRInstruction
{
    /**
     * Constants denoting how a LIR instruction uses an operand.
     */
    // @enum LIRInstruction.OperandMode
    public enum OperandMode
    {
        /**
         * The value must have been defined before. It is alive before the instruction until the
         * beginning of the instruction, but not necessarily throughout the instruction. A register
         * assigned to it can also be assigned to a {@link #TEMP} or {@link #DEF} operand. The value
         * can be used again after the instruction, so the instruction must not modify the register.
         */
        USE,

        /**
         * The value must have been defined before. It is alive before the instruction and
         * throughout the instruction. A register assigned to it cannot be assigned to a
         * {@link #TEMP} or {@link #DEF} operand. The value can be used again after the instruction,
         * so the instruction must not modify the register.
         */
        ALIVE,

        /**
         * The value must not have been defined before, and must not be used after the instruction.
         * The instruction can do whatever it wants with the register assigned to it (or not use it
         * at all).
         */
        TEMP,

        /**
         * The value must not have been defined before. The instruction has to assign a value to the
         * register. The value can (and most likely will) be used after the instruction.
         */
        DEF,
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public static @interface Use
    {
        OperandFlag[] value() default OperandFlag.REG;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public static @interface Alive
    {
        OperandFlag[] value() default OperandFlag.REG;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public static @interface Temp
    {
        OperandFlag[] value() default OperandFlag.REG;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public static @interface Def
    {
        OperandFlag[] value() default OperandFlag.REG;
    }

    /**
     * Flags for an operand.
     */
    // @enum LIRInstruction.OperandFlag
    public enum OperandFlag
    {
        /**
         * The value can be a {@link RegisterValue}.
         */
        REG,

        /**
         * The value can be a {@link StackSlot}.
         */
        STACK,

        /**
         * The value can be a {@link CompositeValue}.
         */
        COMPOSITE,

        /**
         * The value can be a {@link JavaConstant}.
         */
        CONST,

        /**
         * The value can be {@link Value#ILLEGAL}.
         */
        ILLEGAL,

        /**
         * The register allocator should try to assign a certain register to improve code quality.
         * Use {@link LIRInstruction#forEachRegisterHint} to access the register hints.
         */
        HINT,

        /**
         * The value can be uninitialized, e.g. a stack slot that has not written to before. This
         * is only used to avoid false positives in verification code.
         */
        UNINITIALIZED,

        /**
         * Outgoing block value.
         */
        OUTGOING,
    }

    /**
     * For validity checking of the operand flags defined by instruction subclasses.
     */
    // @def
    protected static final EnumMap<OperandMode, EnumSet<OperandFlag>> ALLOWED_FLAGS;

    static
    {
        ALLOWED_FLAGS = new EnumMap<>(OperandMode.class);
        ALLOWED_FLAGS.put(OperandMode.USE, EnumSet.of(OperandFlag.REG, OperandFlag.STACK, OperandFlag.COMPOSITE, OperandFlag.CONST, OperandFlag.ILLEGAL, OperandFlag.HINT, OperandFlag.UNINITIALIZED));
        ALLOWED_FLAGS.put(OperandMode.ALIVE, EnumSet.of(OperandFlag.REG, OperandFlag.STACK, OperandFlag.COMPOSITE, OperandFlag.CONST, OperandFlag.ILLEGAL, OperandFlag.HINT, OperandFlag.UNINITIALIZED, OperandFlag.OUTGOING));
        ALLOWED_FLAGS.put(OperandMode.TEMP, EnumSet.of(OperandFlag.REG, OperandFlag.STACK, OperandFlag.COMPOSITE, OperandFlag.ILLEGAL, OperandFlag.HINT));
        ALLOWED_FLAGS.put(OperandMode.DEF, EnumSet.of(OperandFlag.REG, OperandFlag.STACK, OperandFlag.COMPOSITE, OperandFlag.ILLEGAL, OperandFlag.HINT));
    }

    /**
     * The flags of the base and index value of an address.
     */
    // @def
    protected static final EnumSet<OperandFlag> ADDRESS_FLAGS = EnumSet.of(OperandFlag.REG, OperandFlag.ILLEGAL);

    // @field
    private final LIRInstructionClass<?> instructionClass;

    /**
     * Instruction id for register allocation.
     */
    // @field
    private int id;

    /**
     * Constructs a new LIR instruction.
     */
    // @cons
    public LIRInstruction(LIRInstructionClass<? extends LIRInstruction> __c)
    {
        super();
        instructionClass = __c;
        id = -1;
    }

    public abstract void emitCode(CompilationResultBuilder crb);

    public final int id()
    {
        return id;
    }

    public final void setId(int __id)
    {
        this.id = __id;
    }

    public final String name()
    {
        return instructionClass.getOpcode(this);
    }

    public final boolean hasOperands()
    {
        return instructionClass.hasOperands() || destroysCallerSavedRegisters();
    }

    public boolean destroysCallerSavedRegisters()
    {
        return false;
    }

    // InstructionValueProcedures
    public final void forEachInput(InstructionValueProcedure __proc)
    {
        instructionClass.forEachUse(this, __proc);
    }

    public final void forEachAlive(InstructionValueProcedure __proc)
    {
        instructionClass.forEachAlive(this, __proc);
    }

    public final void forEachTemp(InstructionValueProcedure __proc)
    {
        instructionClass.forEachTemp(this, __proc);
    }

    public final void forEachOutput(InstructionValueProcedure __proc)
    {
        instructionClass.forEachDef(this, __proc);
    }

    // ValueProcedures
    public final void forEachInput(ValueProcedure __proc)
    {
        instructionClass.forEachUse(this, __proc);
    }

    public final void forEachAlive(ValueProcedure __proc)
    {
        instructionClass.forEachAlive(this, __proc);
    }

    public final void forEachTemp(ValueProcedure __proc)
    {
        instructionClass.forEachTemp(this, __proc);
    }

    public final void forEachOutput(ValueProcedure __proc)
    {
        instructionClass.forEachDef(this, __proc);
    }

    // InstructionValueConsumers
    public final void visitEachInput(InstructionValueConsumer __proc)
    {
        instructionClass.visitEachUse(this, __proc);
    }

    public final void visitEachAlive(InstructionValueConsumer __proc)
    {
        instructionClass.visitEachAlive(this, __proc);
    }

    public final void visitEachTemp(InstructionValueConsumer __proc)
    {
        instructionClass.visitEachTemp(this, __proc);
    }

    public final void visitEachOutput(InstructionValueConsumer __proc)
    {
        instructionClass.visitEachDef(this, __proc);
    }

    // ValueConsumers
    public final void visitEachInput(ValueConsumer __proc)
    {
        instructionClass.visitEachUse(this, __proc);
    }

    public final void visitEachAlive(ValueConsumer __proc)
    {
        instructionClass.visitEachAlive(this, __proc);
    }

    public final void visitEachTemp(ValueConsumer __proc)
    {
        instructionClass.visitEachTemp(this, __proc);
    }

    public final void visitEachOutput(ValueConsumer __proc)
    {
        instructionClass.visitEachDef(this, __proc);
    }

    @SuppressWarnings("unused")
    public final Value forEachRegisterHint(Value __value, OperandMode __mode, InstructionValueProcedure __proc)
    {
        return instructionClass.forEachRegisterHint(this, __mode, __proc);
    }

    @SuppressWarnings("unused")
    public final Value forEachRegisterHint(Value __value, OperandMode __mode, ValueProcedure __proc)
    {
        return instructionClass.forEachRegisterHint(this, __mode, __proc);
    }

    /**
     * Returns {@code true} if the instruction is a {@link MoveOp}.
     *
     * This function is preferred to {@code instanceof MoveOp} since the type check is more
     * expensive than reading a field from {@link LIRInstructionClass}.
     */
    public final boolean isMoveOp()
    {
        return instructionClass.isMoveOp();
    }

    /**
     * Returns {@code true} if the instruction is a {@link ValueMoveOp}.
     *
     * This function is preferred to {@code instanceof ValueMoveOp} since the type check is more
     * expensive than reading a field from {@link LIRInstructionClass}.
     */
    public final boolean isValueMoveOp()
    {
        return instructionClass.isValueMoveOp();
    }

    /**
     * Returns {@code true} if the instruction is a {@link LoadConstantOp}.
     *
     * This function is preferred to {@code instanceof LoadConstantOp} since the type check is more
     * expensive than reading a field from {@link LIRInstructionClass}.
     */
    public final boolean isLoadConstantOp()
    {
        return instructionClass.isLoadConstantOp();
    }

    /**
     * Utility method to add stack arguments to a list of temporaries. Useful for modeling calling
     * conventions that kill outgoing argument space.
     *
     * @return additional temporaries
     */
    protected static Value[] addStackSlotsToTemporaries(Value[] __parameters, Value[] __temporaries)
    {
        int __extraTemps = 0;
        for (Value __p : __parameters)
        {
            if (ValueUtil.isStackSlot(__p))
            {
                __extraTemps++;
            }
        }
        if (__extraTemps != 0)
        {
            int __index = __temporaries.length;
            Value[] __newTemporaries = Arrays.copyOf(__temporaries, __temporaries.length + __extraTemps);
            for (Value __p : __parameters)
            {
                if (ValueUtil.isStackSlot(__p))
                {
                    __newTemporaries[__index++] = __p;
                }
            }
            return __newTemporaries;
        }
        return __temporaries;
    }

    public LIRInstructionClass<?> getLIRInstructionClass()
    {
        return instructionClass;
    }

    @Override
    public int hashCode()
    {
        return id;
    }
}
