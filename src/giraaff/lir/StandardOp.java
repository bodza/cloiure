package giraaff.lir;

import java.util.ArrayList;
import java.util.EnumSet;

import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterSaveLayout;
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.Value;

import org.graalvm.collections.EconomicSet;

import giraaff.asm.Label;
import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.asm.CompilationResultBuilder;
import giraaff.lir.framemap.FrameMap;
import giraaff.util.GraalError;

/**
 * A collection of machine-independent LIR operations, as well as interfaces to be implemented for
 * specific kinds or LIR operations.
 */
// @class StandardOp
public final class StandardOp
{
    /**
     * A block delimiter. Every well formed block must contain exactly one such operation and it
     * must be the last operation in the block.
     */
    // @iface StandardOp.BlockEndOp
    public interface BlockEndOp
    {
    }

    // @iface StandardOp.NullCheck
    public interface NullCheck
    {
        Value getCheckedValue();

        LIRFrameState getState();
    }

    // @iface StandardOp.ImplicitNullCheck
    public interface ImplicitNullCheck
    {
        boolean makeNullCheckFor(Value value, LIRFrameState nullCheckState, int implicitNullCheckLimit);
    }

    /**
     * LIR operation that defines the position of a label.
     */
    // @class StandardOp.LabelOp
    public static final class LabelOp extends LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<LabelOp> TYPE = LIRInstructionClass.create(LabelOp.class);

        // @def
        public static final EnumSet<OperandFlag> incomingFlags = EnumSet.of(OperandFlag.REG, OperandFlag.STACK);

        /**
         * In the LIR, every register and variable must be defined before it is used. For method
         * parameters that are passed in fixed registers, exception objects passed to the exception
         * handler in a fixed register, or any other use of a fixed register not defined in this
         * method, an artificial definition is necessary. To avoid spill moves to be inserted
         * between the label at the beginning of a block an an actual definition in the second
         * instruction of a block, the registers are defined here in the label.
         */
        @Def({OperandFlag.REG, OperandFlag.STACK})
        // @field
        private Value[] incomingValues;
        // @field
        private final Label label;
        // @field
        private final boolean align;
        // @field
        private int numbPhis;

        // @cons
        public LabelOp(Label __label, boolean __align)
        {
            super(TYPE);
            this.label = __label;
            this.align = __align;
            this.incomingValues = Value.NO_VALUES;
            this.numbPhis = 0;
        }

        public void setPhiValues(Value[] __values)
        {
            setIncomingValues(__values);
            setNumberOfPhis(__values.length);
        }

        private void setNumberOfPhis(int __numPhis)
        {
            numbPhis = __numPhis;
        }

        public int getPhiSize()
        {
            return numbPhis;
        }

        public void setIncomingValues(Value[] __values)
        {
            this.incomingValues = __values;
        }

        public int getIncomingSize()
        {
            return incomingValues.length;
        }

        public Value getIncomingValue(int __idx)
        {
            return incomingValues[__idx];
        }

        public void clearIncomingValues()
        {
            incomingValues = Value.NO_VALUES;
        }

        public void addIncomingValues(Value[] __values)
        {
            if (incomingValues.length == 0)
            {
                setIncomingValues(__values);
                return;
            }
            int __t = incomingValues.length + __values.length;
            Value[] __newArray = new Value[__t];
            System.arraycopy(incomingValues, 0, __newArray, 0, incomingValues.length);
            System.arraycopy(__values, 0, __newArray, incomingValues.length, __values.length);
            incomingValues = __newArray;
        }

        private boolean checkRange(int __idx)
        {
            return __idx < incomingValues.length;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb)
        {
            if (align)
            {
                __crb.asm.align(__crb.target.wordSize * 2);
            }
            __crb.asm.bind(label);
        }

        public Label getLabel()
        {
            return label;
        }

        /**
         * @return true if this label acts as a PhiIn.
         */
        public boolean isPhiIn()
        {
            return getPhiSize() > 0;
        }

        public void forEachIncomingValue(InstructionValueProcedure __proc)
        {
            for (int __i = 0; __i < incomingValues.length; __i++)
            {
                incomingValues[__i] = __proc.doValue(this, incomingValues[__i], OperandMode.DEF, incomingFlags);
            }
        }
    }

    /**
     * LIR operation that is an unconditional jump to a {@link #destination()}.
     */
    // @class StandardOp.JumpOp
    public static final class JumpOp extends LIRInstruction implements BlockEndOp
    {
        // @def
        public static final LIRInstructionClass<JumpOp> TYPE = LIRInstructionClass.create(JumpOp.class);

        // @def
        public static final EnumSet<OperandFlag> outgoingFlags = EnumSet.of(OperandFlag.REG, OperandFlag.STACK, OperandFlag.CONST, OperandFlag.OUTGOING);

        @Alive({OperandFlag.REG, OperandFlag.STACK, OperandFlag.CONST, OperandFlag.OUTGOING})
        // @field
        private Value[] outgoingValues;

        // @field
        private final LabelRef destination;

        // @cons
        public JumpOp(LabelRef __destination)
        {
            this(TYPE, __destination);
        }

        // @cons
        protected JumpOp(LIRInstructionClass<? extends JumpOp> __c, LabelRef __destination)
        {
            super(__c);
            this.destination = __destination;
            this.outgoingValues = Value.NO_VALUES;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb)
        {
            if (!__crb.isSuccessorEdge(destination))
            {
                __crb.asm.jmp(destination.label());
            }
        }

        public LabelRef destination()
        {
            return destination;
        }

        public void setPhiValues(Value[] __values)
        {
            this.outgoingValues = __values;
        }

        public int getPhiSize()
        {
            return outgoingValues.length;
        }

        public Value getOutgoingValue(int __idx)
        {
            return outgoingValues[__idx];
        }

        public void clearOutgoingValues()
        {
            outgoingValues = Value.NO_VALUES;
        }

        private boolean checkRange(int __idx)
        {
            return __idx < outgoingValues.length;
        }
    }

    /**
     * Marker interface for a LIR operation that is a conditional jump.
     */
    // @iface StandardOp.BranchOp
    public interface BranchOp extends BlockEndOp
    {
    }

    /**
     * Marker interface for a LIR operation that moves a value to {@link #getResult()}.
     */
    // @iface StandardOp.MoveOp
    public interface MoveOp
    {
        AllocatableValue getResult();

        static MoveOp asMoveOp(LIRInstruction __op)
        {
            return (MoveOp) __op;
        }

        static boolean isMoveOp(LIRInstruction __op)
        {
            return __op.isMoveOp();
        }
    }

    /**
     * Marker interface for a LIR operation that moves some non-constant value to another location.
     */
    // @iface StandardOp.ValueMoveOp
    public interface ValueMoveOp extends MoveOp
    {
        AllocatableValue getInput();

        static ValueMoveOp asValueMoveOp(LIRInstruction __op)
        {
            return (ValueMoveOp) __op;
        }

        static boolean isValueMoveOp(LIRInstruction __op)
        {
            return __op.isValueMoveOp();
        }
    }

    /**
     * Marker interface for a LIR operation that loads a {@link #getConstant()}.
     */
    // @iface StandardOp.LoadConstantOp
    public interface LoadConstantOp extends MoveOp
    {
        Constant getConstant();

        static LoadConstantOp asLoadConstantOp(LIRInstruction __op)
        {
            return (LoadConstantOp) __op;
        }

        static boolean isLoadConstantOp(LIRInstruction __op)
        {
            return __op.isLoadConstantOp();
        }
    }

    /**
     * An operation that saves registers to the stack. The set of saved registers can be
     * {@linkplain #remove(EconomicSet) pruned} and a mapping from registers to the frame slots in
     * which they are saved can be {@linkplain #getMap(FrameMap) retrieved}.
     */
    // @iface StandardOp.SaveRegistersOp
    public interface SaveRegistersOp
    {
        /**
         * Determines if the {@link #remove(EconomicSet)} operation is supported for this object.
         */
        boolean supportsRemove();

        /**
         * Prunes {@code doNotSave} from the registers saved by this operation.
         *
         * @param doNotSave registers that should not be saved by this operation
         * @return the number of registers pruned
         * @throws UnsupportedOperationException if removal is not {@linkplain #supportsRemove() supported}
         */
        int remove(EconomicSet<Register> doNotSave);

        /**
         * Gets a map from the saved registers saved by this operation to the frame slots in which
         * they are saved.
         *
         * @param frameMap used to {@linkplain FrameMap#offsetForStackSlot(StackSlot) convert} a
         *            virtual slot to a frame slot index
         */
        RegisterSaveLayout getMap(FrameMap frameMap);
    }

    /**
     * A LIR operation that does nothing. If the operation records its position, it can be
     * subsequently {@linkplain #replace(LIR, LIRInstruction) replaced}.
     */
    // @class StandardOp.NoOp
    public static final class NoOp extends LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<NoOp> TYPE = LIRInstructionClass.create(NoOp.class);

        /**
         * The block in which this instruction is located.
         */
        // @field
        final AbstractBlockBase<?> block;

        /**
         * The block index of this instruction.
         */
        // @field
        final int index;

        // @cons
        public NoOp(AbstractBlockBase<?> __block, int __index)
        {
            super(TYPE);
            this.block = __block;
            this.index = __index;
        }

        public void replace(LIR __lir, LIRInstruction __replacement)
        {
            ArrayList<LIRInstruction> __instructions = __lir.getLIRforBlock(block);
            __instructions.set(index, __replacement);
        }

        public void remove(LIR __lir)
        {
            ArrayList<LIRInstruction> __instructions = __lir.getLIRforBlock(block);
            __instructions.remove(index);
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb)
        {
            if (block != null)
            {
                throw new GraalError(this + " should have been replaced");
            }
        }
    }

    @Opcode
    // @class StandardOp.BlackholeOp
    public static final class BlackholeOp extends LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<BlackholeOp> TYPE = LIRInstructionClass.create(BlackholeOp.class);

        @Use({OperandFlag.REG, OperandFlag.STACK, OperandFlag.CONST})
        // @field
        private Value value;

        // @cons
        public BlackholeOp(Value __value)
        {
            super(TYPE);
            this.value = __value;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb)
        {
            // do nothing, just keep value alive until at least here
        }
    }

    // @class StandardOp.BindToRegisterOp
    public static final class BindToRegisterOp extends LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<BindToRegisterOp> TYPE = LIRInstructionClass.create(BindToRegisterOp.class);

        @Use({OperandFlag.REG})
        // @field
        private Value value;

        // @cons
        public BindToRegisterOp(Value __value)
        {
            super(TYPE);
            this.value = __value;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb)
        {
            // do nothing, just keep value alive until at least here
        }
    }

    @Opcode
    // @class StandardOp.SpillRegistersOp
    public static final class SpillRegistersOp extends LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<SpillRegistersOp> TYPE = LIRInstructionClass.create(SpillRegistersOp.class);

        // @cons
        public SpillRegistersOp()
        {
            super(TYPE);
        }

        @Override
        public boolean destroysCallerSavedRegisters()
        {
            return true;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb)
        {
            // do nothing, just keep value alive until at least here
        }
    }

    // @class StandardOp.StackMove
    public static final class StackMove extends LIRInstruction implements ValueMoveOp
    {
        // @def
        public static final LIRInstructionClass<StackMove> TYPE = LIRInstructionClass.create(StackMove.class);

        @Def({OperandFlag.STACK, OperandFlag.HINT})
        // @field
        protected AllocatableValue result;
        @Use({OperandFlag.STACK})
        // @field
        protected AllocatableValue input;

        // @cons
        public StackMove(AllocatableValue __result, AllocatableValue __input)
        {
            super(TYPE);
            this.result = __result;
            this.input = __input;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb)
        {
            throw new GraalError(this + " should have been removed");
        }

        @Override
        public AllocatableValue getInput()
        {
            return input;
        }

        @Override
        public AllocatableValue getResult()
        {
            return result;
        }
    }
}
