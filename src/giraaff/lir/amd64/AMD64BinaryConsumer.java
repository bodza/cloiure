package giraaff.lir.amd64;

import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.code.site.DataSectionReference;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.VMConstant;
import jdk.vm.ci.meta.Value;

import giraaff.asm.amd64.AMD64Address;
import giraaff.asm.amd64.AMD64Assembler.AMD64BinaryArithmetic;
import giraaff.asm.amd64.AMD64Assembler.AMD64MIOp;
import giraaff.asm.amd64.AMD64Assembler.AMD64MROp;
import giraaff.asm.amd64.AMD64Assembler.AMD64RMOp;
import giraaff.asm.amd64.AMD64Assembler.OperandSize;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.core.common.NumUtil;
import giraaff.lir.LIRFrameState;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.StandardOp.ImplicitNullCheck;
import giraaff.lir.asm.CompilationResultBuilder;

/**
 * AMD64 LIR instructions that have two input operands, but no output operand.
 */
// @class AMD64BinaryConsumer
public final class AMD64BinaryConsumer
{
    /**
     * Instruction that has two {@link AllocatableValue} operands.
     */
    // @class AMD64BinaryConsumer.Op
    public static final class Op extends AMD64LIRInstruction
    {
        public static final LIRInstructionClass<Op> TYPE = LIRInstructionClass.create(Op.class);

        @Opcode private final AMD64RMOp opcode;
        private final OperandSize size;

        @Use({OperandFlag.REG}) protected AllocatableValue x;
        @Use({OperandFlag.REG, OperandFlag.STACK}) protected AllocatableValue y;

        // @cons
        public Op(AMD64RMOp opcode, OperandSize size, AllocatableValue x, AllocatableValue y)
        {
            super(TYPE);
            this.opcode = opcode;
            this.size = size;

            this.x = x;
            this.y = y;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            if (ValueUtil.isRegister(y))
            {
                opcode.emit(masm, size, ValueUtil.asRegister(x), ValueUtil.asRegister(y));
            }
            else
            {
                opcode.emit(masm, size, ValueUtil.asRegister(x), (AMD64Address) crb.asAddress(y));
            }
        }
    }

    /**
     * Instruction that has one {@link AllocatableValue} operand and one 32-bit immediate operand.
     */
    // @class AMD64BinaryConsumer.ConstOp
    public static class ConstOp extends AMD64LIRInstruction
    {
        public static final LIRInstructionClass<ConstOp> TYPE = LIRInstructionClass.create(ConstOp.class);

        @Opcode private final AMD64MIOp opcode;
        private final OperandSize size;

        @Use({OperandFlag.REG, OperandFlag.STACK}) protected AllocatableValue x;
        private final int y;

        // @cons
        public ConstOp(AMD64BinaryArithmetic opcode, OperandSize size, AllocatableValue x, int y)
        {
            this(opcode.getMIOpcode(size, NumUtil.isByte(y)), size, x, y);
        }

        // @cons
        public ConstOp(AMD64MIOp opcode, OperandSize size, AllocatableValue x, int y)
        {
            this(TYPE, opcode, size, x, y);
        }

        // @cons
        protected ConstOp(LIRInstructionClass<? extends ConstOp> c, AMD64MIOp opcode, OperandSize size, AllocatableValue x, int y)
        {
            super(c);
            this.opcode = opcode;
            this.size = size;

            this.x = x;
            this.y = y;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            if (ValueUtil.isRegister(x))
            {
                opcode.emit(masm, size, ValueUtil.asRegister(x), y);
            }
            else
            {
                opcode.emit(masm, size, (AMD64Address) crb.asAddress(x), y);
            }
        }
    }

    /**
     * Instruction that has one {@link AllocatableValue} operand and one 32-bit immediate operand
     * that needs to be patched at runtime.
     */
    // @class AMD64BinaryConsumer.VMConstOp
    public static final class VMConstOp extends ConstOp
    {
        public static final LIRInstructionClass<VMConstOp> TYPE = LIRInstructionClass.create(VMConstOp.class);

        protected final VMConstant c;

        // @cons
        public VMConstOp(AMD64MIOp opcode, AllocatableValue x, VMConstant c)
        {
            super(TYPE, opcode, OperandSize.DWORD, x, 0xDEADDEAD);
            this.c = c;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            crb.recordInlineDataInCode(c);
            super.emitCode(crb, masm);
        }
    }

    /**
     * Instruction that has one {@link AllocatableValue} operand and one
     * {@link DataSectionReference} operand.
     */
    // @class AMD64BinaryConsumer.DataOp
    public static final class DataOp extends AMD64LIRInstruction
    {
        public static final LIRInstructionClass<DataOp> TYPE = LIRInstructionClass.create(DataOp.class);

        @Opcode private final AMD64RMOp opcode;
        private final OperandSize size;

        @Use({OperandFlag.REG}) protected AllocatableValue x;
        private final Constant y;

        private final int alignment;

        // @cons
        public DataOp(AMD64RMOp opcode, OperandSize size, AllocatableValue x, Constant y)
        {
            this(opcode, size, x, y, size.getBytes());
        }

        // @cons
        public DataOp(AMD64RMOp opcode, OperandSize size, AllocatableValue x, Constant y, int alignment)
        {
            super(TYPE);
            this.opcode = opcode;
            this.size = size;

            this.x = x;
            this.y = y;

            this.alignment = alignment;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            opcode.emit(masm, size, ValueUtil.asRegister(x), (AMD64Address) crb.recordDataReferenceInCode(y, alignment));
        }
    }

    /**
     * Instruction that has an {@link AllocatableValue} as first input and a
     * {@link AMD64AddressValue memory} operand as second input.
     */
    // @class AMD64BinaryConsumer.MemoryRMOp
    public static final class MemoryRMOp extends AMD64LIRInstruction implements ImplicitNullCheck
    {
        public static final LIRInstructionClass<MemoryRMOp> TYPE = LIRInstructionClass.create(MemoryRMOp.class);

        @Opcode private final AMD64RMOp opcode;
        private final OperandSize size;

        @Use({OperandFlag.REG}) protected AllocatableValue x;
        @Use({OperandFlag.COMPOSITE}) protected AMD64AddressValue y;

        // @State
        protected LIRFrameState state;

        // @cons
        public MemoryRMOp(AMD64RMOp opcode, OperandSize size, AllocatableValue x, AMD64AddressValue y, LIRFrameState state)
        {
            super(TYPE);
            this.opcode = opcode;
            this.size = size;

            this.x = x;
            this.y = y;

            this.state = state;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            opcode.emit(masm, size, ValueUtil.asRegister(x), y.toAddress());
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
     * Instruction that has a {@link AMD64AddressValue memory} operand as first input and an
     * {@link AllocatableValue} as second input.
     */
    // @class AMD64BinaryConsumer.MemoryMROp
    public static final class MemoryMROp extends AMD64LIRInstruction implements ImplicitNullCheck
    {
        public static final LIRInstructionClass<MemoryMROp> TYPE = LIRInstructionClass.create(MemoryMROp.class);

        @Opcode private final AMD64MROp opcode;
        private final OperandSize size;

        @Use({OperandFlag.COMPOSITE}) protected AMD64AddressValue x;
        @Use({OperandFlag.REG}) protected AllocatableValue y;

        // @State
        protected LIRFrameState state;

        // @cons
        public MemoryMROp(AMD64MROp opcode, OperandSize size, AMD64AddressValue x, AllocatableValue y, LIRFrameState state)
        {
            super(TYPE);
            this.opcode = opcode;
            this.size = size;

            this.x = x;
            this.y = y;

            this.state = state;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            opcode.emit(masm, size, x.toAddress(), ValueUtil.asRegister(y));
        }

        @Override
        public boolean makeNullCheckFor(Value value, LIRFrameState nullCheckState, int implicitNullCheckLimit)
        {
            if (state == null && x.isValidImplicitNullCheckFor(value, implicitNullCheckLimit))
            {
                state = nullCheckState;
                return true;
            }
            return false;
        }
    }

    /**
     * Instruction that has one {@link AMD64AddressValue memory} operand and one 32-bit immediate operand.
     */
    // @class AMD64BinaryConsumer.MemoryConstOp
    public static class MemoryConstOp extends AMD64LIRInstruction implements ImplicitNullCheck
    {
        public static final LIRInstructionClass<MemoryConstOp> TYPE = LIRInstructionClass.create(MemoryConstOp.class);

        @Opcode private final AMD64MIOp opcode;
        private final OperandSize size;

        @Use({OperandFlag.COMPOSITE}) protected AMD64AddressValue x;
        private final int y;

        // @State
        protected LIRFrameState state;

        // @cons
        public MemoryConstOp(AMD64BinaryArithmetic opcode, OperandSize size, AMD64AddressValue x, int y, LIRFrameState state)
        {
            this(opcode.getMIOpcode(size, NumUtil.isByte(y)), size, x, y, state);
        }

        // @cons
        public MemoryConstOp(AMD64MIOp opcode, OperandSize size, AMD64AddressValue x, int y, LIRFrameState state)
        {
            this(TYPE, opcode, size, x, y, state);
        }

        // @cons
        protected MemoryConstOp(LIRInstructionClass<? extends MemoryConstOp> c, AMD64MIOp opcode, OperandSize size, AMD64AddressValue x, int y, LIRFrameState state)
        {
            super(c);
            this.opcode = opcode;
            this.size = size;

            this.x = x;
            this.y = y;

            this.state = state;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            opcode.emit(masm, size, x.toAddress(), y);
        }

        @Override
        public boolean makeNullCheckFor(Value value, LIRFrameState nullCheckState, int implicitNullCheckLimit)
        {
            if (state == null && x.isValidImplicitNullCheckFor(value, implicitNullCheckLimit))
            {
                state = nullCheckState;
                return true;
            }
            return false;
        }

        public AMD64MIOp getOpcode()
        {
            return opcode;
        }
    }

    /**
     * Instruction that has one {@link AMD64AddressValue memory} operand and one 32-bit immediate
     * operand that needs to be patched at runtime.
     */
    // @class AMD64BinaryConsumer.MemoryVMConstOp
    public static final class MemoryVMConstOp extends MemoryConstOp
    {
        public static final LIRInstructionClass<MemoryVMConstOp> TYPE = LIRInstructionClass.create(MemoryVMConstOp.class);

        protected final VMConstant c;

        // @cons
        public MemoryVMConstOp(AMD64MIOp opcode, AMD64AddressValue x, VMConstant c, LIRFrameState state)
        {
            super(TYPE, opcode, OperandSize.DWORD, x, 0xDEADDEAD, state);
            this.c = c;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            crb.recordInlineDataInCode(c);
            super.emitCode(crb, masm);
        }
    }
}
