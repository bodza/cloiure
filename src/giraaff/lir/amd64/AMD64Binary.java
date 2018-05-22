package giraaff.lir.amd64;

import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.code.site.DataSectionReference;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.Value;

import giraaff.asm.amd64.AMD64Address;
import giraaff.asm.amd64.AMD64Assembler.AMD64BinaryArithmetic;
import giraaff.asm.amd64.AMD64Assembler.AMD64MIOp;
import giraaff.asm.amd64.AMD64Assembler.AMD64RMIOp;
import giraaff.asm.amd64.AMD64Assembler.AMD64RMOp;
import giraaff.asm.amd64.AMD64Assembler.AMD64RRMOp;
import giraaff.asm.amd64.AMD64Assembler.OperandSize;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.core.common.NumUtil;
import giraaff.lir.LIRFrameState;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.LIRValueUtil;
import giraaff.lir.Opcode;
import giraaff.lir.StandardOp.ImplicitNullCheck;
import giraaff.lir.asm.CompilationResultBuilder;

/**
 * AMD64 LIR instructions that have two inputs and one output.
 */
public class AMD64Binary
{
    /**
     * Instruction that has two {@link AllocatableValue} operands.
     */
    public static class TwoOp extends AMD64LIRInstruction
    {
        public static final LIRInstructionClass<TwoOp> TYPE = LIRInstructionClass.create(TwoOp.class);

        @Opcode private final AMD64RMOp opcode;
        private final OperandSize size;

        @Def({OperandFlag.REG, OperandFlag.HINT}) protected AllocatableValue result;
        @Use({OperandFlag.REG}) protected AllocatableValue x;
        /**
         * This argument must be Alive to ensure that result and y are not assigned to the same
         * register, which would break the code generation by destroying y too early.
         */
        @Alive({OperandFlag.REG, OperandFlag.STACK}) protected AllocatableValue y;

        public TwoOp(AMD64RMOp opcode, OperandSize size, AllocatableValue result, AllocatableValue x, AllocatableValue y)
        {
            super(TYPE);
            this.opcode = opcode;
            this.size = size;

            this.result = result;
            this.x = x;
            this.y = y;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            AMD64Move.move(crb, masm, result, x);
            if (ValueUtil.isRegister(y))
            {
                opcode.emit(masm, size, ValueUtil.asRegister(result), ValueUtil.asRegister(y));
            }
            else
            {
                opcode.emit(masm, size, ValueUtil.asRegister(result), (AMD64Address) crb.asAddress(y));
            }
        }
    }

    /**
     * Instruction that has three {@link AllocatableValue} operands.
     */
    public static class ThreeOp extends AMD64LIRInstruction
    {
        public static final LIRInstructionClass<ThreeOp> TYPE = LIRInstructionClass.create(ThreeOp.class);

        @Opcode private final AMD64RRMOp opcode;
        private final OperandSize size;

        @Def({OperandFlag.REG}) protected AllocatableValue result;
        @Use({OperandFlag.REG}) protected AllocatableValue x;
        @Use({OperandFlag.REG, OperandFlag.STACK}) protected AllocatableValue y;

        public ThreeOp(AMD64RRMOp opcode, OperandSize size, AllocatableValue result, AllocatableValue x, AllocatableValue y)
        {
            super(TYPE);
            this.opcode = opcode;
            this.size = size;

            this.result = result;
            this.x = x;
            this.y = y;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            if (ValueUtil.isRegister(y))
            {
                opcode.emit(masm, size, ValueUtil.asRegister(result), ValueUtil.asRegister(x), ValueUtil.asRegister(y));
            }
            else
            {
                opcode.emit(masm, size, ValueUtil.asRegister(result), ValueUtil.asRegister(x), (AMD64Address) crb.asAddress(y));
            }
        }
    }

    /**
     * Commutative instruction that has two {@link AllocatableValue} operands.
     */
    public static class CommutativeTwoOp extends AMD64LIRInstruction
    {
        public static final LIRInstructionClass<CommutativeTwoOp> TYPE = LIRInstructionClass.create(CommutativeTwoOp.class);

        @Opcode private final AMD64RMOp opcode;
        private final OperandSize size;

        @Def({OperandFlag.REG, OperandFlag.HINT}) protected AllocatableValue result;
        @Use({OperandFlag.REG, OperandFlag.STACK}) protected AllocatableValue x;
        @Use({OperandFlag.REG, OperandFlag.STACK}) protected AllocatableValue y;

        public CommutativeTwoOp(AMD64RMOp opcode, OperandSize size, AllocatableValue result, AllocatableValue x, AllocatableValue y)
        {
            super(TYPE);
            this.opcode = opcode;
            this.size = size;

            this.result = result;
            this.x = x;
            this.y = y;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            AllocatableValue input;
            if (LIRValueUtil.sameRegister(result, y))
            {
                input = x;
            }
            else
            {
                AMD64Move.move(crb, masm, result, x);
                input = y;
            }

            if (ValueUtil.isRegister(input))
            {
                opcode.emit(masm, size, ValueUtil.asRegister(result), ValueUtil.asRegister(input));
            }
            else
            {
                opcode.emit(masm, size, ValueUtil.asRegister(result), (AMD64Address) crb.asAddress(input));
            }
        }
    }

    /**
     * Commutative instruction that has three {@link AllocatableValue} operands.
     */
    public static class CommutativeThreeOp extends AMD64LIRInstruction
    {
        public static final LIRInstructionClass<CommutativeThreeOp> TYPE = LIRInstructionClass.create(CommutativeThreeOp.class);

        @Opcode private final AMD64RRMOp opcode;
        private final OperandSize size;

        @Def({OperandFlag.REG}) protected AllocatableValue result;
        @Use({OperandFlag.REG}) protected AllocatableValue x;
        @Use({OperandFlag.REG, OperandFlag.STACK}) protected AllocatableValue y;

        public CommutativeThreeOp(AMD64RRMOp opcode, OperandSize size, AllocatableValue result, AllocatableValue x, AllocatableValue y)
        {
            super(TYPE);
            this.opcode = opcode;
            this.size = size;

            this.result = result;
            this.x = x;
            this.y = y;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            if (ValueUtil.isRegister(y))
            {
                opcode.emit(masm, size, ValueUtil.asRegister(result), ValueUtil.asRegister(x), ValueUtil.asRegister(y));
            }
            else
            {
                opcode.emit(masm, size, ValueUtil.asRegister(result), ValueUtil.asRegister(x), (AMD64Address) crb.asAddress(y));
            }
        }
    }

    /**
     * Instruction that has one {@link AllocatableValue} operand and one 32-bit immediate operand.
     */
    public static class ConstOp extends AMD64LIRInstruction
    {
        public static final LIRInstructionClass<ConstOp> TYPE = LIRInstructionClass.create(ConstOp.class);

        @Opcode private final AMD64MIOp opcode;
        private final OperandSize size;

        @Def({OperandFlag.REG, OperandFlag.HINT}) protected AllocatableValue result;
        @Use({OperandFlag.REG}) protected AllocatableValue x;
        private final int y;

        public ConstOp(AMD64BinaryArithmetic opcode, OperandSize size, AllocatableValue result, AllocatableValue x, int y)
        {
            this(opcode.getMIOpcode(size, NumUtil.isByte(y)), size, result, x, y);
        }

        public ConstOp(AMD64MIOp opcode, OperandSize size, AllocatableValue result, AllocatableValue x, int y)
        {
            super(TYPE);
            this.opcode = opcode;
            this.size = size;

            this.result = result;
            this.x = x;
            this.y = y;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            AMD64Move.move(crb, masm, result, x);
            opcode.emit(masm, size, ValueUtil.asRegister(result), y);
        }
    }

    /**
     * Instruction that has one {@link AllocatableValue} operand and one
     * {@link DataSectionReference} operand.
     */
    public static class DataTwoOp extends AMD64LIRInstruction
    {
        public static final LIRInstructionClass<DataTwoOp> TYPE = LIRInstructionClass.create(DataTwoOp.class);

        @Opcode private final AMD64RMOp opcode;
        private final OperandSize size;

        @Def({OperandFlag.REG, OperandFlag.HINT}) protected AllocatableValue result;
        @Use({OperandFlag.REG}) protected AllocatableValue x;
        private final JavaConstant y;

        private final int alignment;

        public DataTwoOp(AMD64RMOp opcode, OperandSize size, AllocatableValue result, AllocatableValue x, JavaConstant y)
        {
            this(opcode, size, result, x, y, y.getJavaKind().getByteCount());
        }

        public DataTwoOp(AMD64RMOp opcode, OperandSize size, AllocatableValue result, AllocatableValue x, JavaConstant y, int alignment)
        {
            super(TYPE);
            this.opcode = opcode;
            this.size = size;

            this.result = result;
            this.x = x;
            this.y = y;

            this.alignment = alignment;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            AMD64Move.move(crb, masm, result, x);
            opcode.emit(masm, size, ValueUtil.asRegister(result), (AMD64Address) crb.recordDataReferenceInCode(y, alignment));
        }
    }

    /**
     * Instruction that has two {@link AllocatableValue} operands and one
     * {@link DataSectionReference} operand.
     */
    public static class DataThreeOp extends AMD64LIRInstruction
    {
        public static final LIRInstructionClass<DataThreeOp> TYPE = LIRInstructionClass.create(DataThreeOp.class);

        @Opcode private final AMD64RRMOp opcode;
        private final OperandSize size;

        @Def({OperandFlag.REG}) protected AllocatableValue result;
        @Use({OperandFlag.REG}) protected AllocatableValue x;
        private final JavaConstant y;

        private final int alignment;

        public DataThreeOp(AMD64RRMOp opcode, OperandSize size, AllocatableValue result, AllocatableValue x, JavaConstant y)
        {
            this(opcode, size, result, x, y, y.getJavaKind().getByteCount());
        }

        public DataThreeOp(AMD64RRMOp opcode, OperandSize size, AllocatableValue result, AllocatableValue x, JavaConstant y, int alignment)
        {
            super(TYPE);
            this.opcode = opcode;
            this.size = size;

            this.result = result;
            this.x = x;
            this.y = y;

            this.alignment = alignment;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            opcode.emit(masm, size, ValueUtil.asRegister(result), ValueUtil.asRegister(x), (AMD64Address) crb.recordDataReferenceInCode(y, alignment));
        }
    }

    /**
     * Instruction that has one {@link AllocatableValue} operand and one {@link AMD64AddressValue
     * memory} operand.
     */
    public static class MemoryTwoOp extends AMD64LIRInstruction implements ImplicitNullCheck
    {
        public static final LIRInstructionClass<MemoryTwoOp> TYPE = LIRInstructionClass.create(MemoryTwoOp.class);

        @Opcode private final AMD64RMOp opcode;
        private final OperandSize size;

        @Def({OperandFlag.REG, OperandFlag.HINT}) protected AllocatableValue result;
        @Use({OperandFlag.REG}) protected AllocatableValue x;
        @Alive({OperandFlag.COMPOSITE}) protected AMD64AddressValue y;

        @State protected LIRFrameState state;

        public MemoryTwoOp(AMD64RMOp opcode, OperandSize size, AllocatableValue result, AllocatableValue x, AMD64AddressValue y, LIRFrameState state)
        {
            super(TYPE);
            this.opcode = opcode;
            this.size = size;

            this.result = result;
            this.x = x;
            this.y = y;

            this.state = state;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            AMD64Move.move(crb, masm, result, x);
            if (state != null)
            {
                crb.recordImplicitException(masm.position(), state);
            }
            opcode.emit(masm, size, ValueUtil.asRegister(result), y.toAddress());
        }

        @Override
        public boolean makeNullCheckFor(Value value, LIRFrameState nullCheckState, int implicitNullCheckLimit)
        {
            if (state == null && y.isValidImplicitNullCheckFor(value, implicitNullCheckLimit))
            {
                state = nullCheckState;
                return true;
            }
            return false;
        }
    }

    /**
     * Instruction that has one {@link AllocatableValue} operand and one {@link AMD64AddressValue
     * memory} operand.
     */
    public static class MemoryThreeOp extends AMD64LIRInstruction implements ImplicitNullCheck
    {
        public static final LIRInstructionClass<MemoryThreeOp> TYPE = LIRInstructionClass.create(MemoryThreeOp.class);

        @Opcode private final AMD64RRMOp opcode;
        private final OperandSize size;

        @Def({OperandFlag.REG}) protected AllocatableValue result;
        @Use({OperandFlag.REG}) protected AllocatableValue x;
        @Use({OperandFlag.COMPOSITE}) protected AMD64AddressValue y;

        @State protected LIRFrameState state;

        public MemoryThreeOp(AMD64RRMOp opcode, OperandSize size, AllocatableValue result, AllocatableValue x, AMD64AddressValue y, LIRFrameState state)
        {
            super(TYPE);
            this.opcode = opcode;
            this.size = size;

            this.result = result;
            this.x = x;
            this.y = y;

            this.state = state;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            if (state != null)
            {
                crb.recordImplicitException(masm.position(), state);
            }
            opcode.emit(masm, size, ValueUtil.asRegister(result), ValueUtil.asRegister(x), y.toAddress());
        }

        @Override
        public boolean makeNullCheckFor(Value value, LIRFrameState nullCheckState, int implicitNullCheckLimit)
        {
            if (state == null && y.isValidImplicitNullCheckFor(value, implicitNullCheckLimit))
            {
                state = nullCheckState;
                return true;
            }
            return false;
        }
    }

    /**
     * Instruction with a separate result operand, one {@link AllocatableValue} input and one 32-bit
     * immediate input.
     */
    public static class RMIOp extends AMD64LIRInstruction
    {
        public static final LIRInstructionClass<RMIOp> TYPE = LIRInstructionClass.create(RMIOp.class);

        @Opcode private final AMD64RMIOp opcode;
        private final OperandSize size;

        @Def({OperandFlag.REG}) protected AllocatableValue result;
        @Use({OperandFlag.REG, OperandFlag.STACK}) protected AllocatableValue x;
        private final int y;

        public RMIOp(AMD64RMIOp opcode, OperandSize size, AllocatableValue result, AllocatableValue x, int y)
        {
            super(TYPE);
            this.opcode = opcode;
            this.size = size;

            this.result = result;
            this.x = x;
            this.y = y;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            if (ValueUtil.isRegister(x))
            {
                opcode.emit(masm, size, ValueUtil.asRegister(result), ValueUtil.asRegister(x), y);
            }
            else
            {
                opcode.emit(masm, size, ValueUtil.asRegister(result), (AMD64Address) crb.asAddress(x), y);
            }
        }
    }
}
