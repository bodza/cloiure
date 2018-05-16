package graalvm.compiler.lir.amd64;

import static graalvm.compiler.asm.amd64.AMD64Assembler.OperandSize.DWORD;
import static graalvm.compiler.lir.LIRInstruction.OperandFlag.COMPOSITE;
import static graalvm.compiler.lir.LIRInstruction.OperandFlag.REG;
import static graalvm.compiler.lir.LIRInstruction.OperandFlag.STACK;
import static jdk.vm.ci.code.ValueUtil.asRegister;
import static jdk.vm.ci.code.ValueUtil.isRegister;
import static jdk.vm.ci.code.ValueUtil.isStackSlot;

import graalvm.compiler.core.common.NumUtil;
import graalvm.compiler.asm.amd64.AMD64Address;
import graalvm.compiler.asm.amd64.AMD64Assembler.AMD64BinaryArithmetic;
import graalvm.compiler.asm.amd64.AMD64Assembler.AMD64MIOp;
import graalvm.compiler.asm.amd64.AMD64Assembler.AMD64MROp;
import graalvm.compiler.asm.amd64.AMD64Assembler.AMD64RMOp;
import graalvm.compiler.asm.amd64.AMD64Assembler.OperandSize;
import graalvm.compiler.asm.amd64.AMD64MacroAssembler;
import graalvm.compiler.lir.LIRFrameState;
import graalvm.compiler.lir.LIRInstructionClass;
import graalvm.compiler.lir.Opcode;
import graalvm.compiler.lir.StandardOp.ImplicitNullCheck;
import graalvm.compiler.lir.asm.CompilationResultBuilder;

import jdk.vm.ci.code.site.DataSectionReference;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.VMConstant;
import jdk.vm.ci.meta.Value;

/**
 * AMD64 LIR instructions that have two input operands, but no output operand.
 */
public class AMD64BinaryConsumer
{
    /**
     * Instruction that has two {@link AllocatableValue} operands.
     */
    public static class Op extends AMD64LIRInstruction
    {
        public static final LIRInstructionClass<Op> TYPE = LIRInstructionClass.create(Op.class);

        @Opcode private final AMD64RMOp opcode;
        private final OperandSize size;

        @Use({REG}) protected AllocatableValue x;
        @Use({REG, STACK}) protected AllocatableValue y;

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
            if (isRegister(y))
            {
                opcode.emit(masm, size, asRegister(x), asRegister(y));
            }
            else
            {
                assert isStackSlot(y);
                opcode.emit(masm, size, asRegister(x), (AMD64Address) crb.asAddress(y));
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

        @Use({REG, STACK}) protected AllocatableValue x;
        private final int y;

        public ConstOp(AMD64BinaryArithmetic opcode, OperandSize size, AllocatableValue x, int y)
        {
            this(opcode.getMIOpcode(size, NumUtil.isByte(y)), size, x, y);
        }

        public ConstOp(AMD64MIOp opcode, OperandSize size, AllocatableValue x, int y)
        {
            this(TYPE, opcode, size, x, y);
        }

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
            if (isRegister(x))
            {
                opcode.emit(masm, size, asRegister(x), y);
            }
            else
            {
                assert isStackSlot(x);
                opcode.emit(masm, size, (AMD64Address) crb.asAddress(x), y);
            }
        }
    }

    /**
     * Instruction that has one {@link AllocatableValue} operand and one 32-bit immediate operand
     * that needs to be patched at runtime.
     */
    public static class VMConstOp extends ConstOp
    {
        public static final LIRInstructionClass<VMConstOp> TYPE = LIRInstructionClass.create(VMConstOp.class);

        protected final VMConstant c;

        public VMConstOp(AMD64MIOp opcode, AllocatableValue x, VMConstant c)
        {
            super(TYPE, opcode, DWORD, x, 0xDEADDEAD);
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
    public static class DataOp extends AMD64LIRInstruction
    {
        public static final LIRInstructionClass<DataOp> TYPE = LIRInstructionClass.create(DataOp.class);

        @Opcode private final AMD64RMOp opcode;
        private final OperandSize size;

        @Use({REG}) protected AllocatableValue x;
        private final Constant y;

        private final int alignment;

        public DataOp(AMD64RMOp opcode, OperandSize size, AllocatableValue x, Constant y)
        {
            this(opcode, size, x, y, size.getBytes());
        }

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
            opcode.emit(masm, size, asRegister(x), (AMD64Address) crb.recordDataReferenceInCode(y, alignment));
        }
    }

    /**
     * Instruction that has an {@link AllocatableValue} as first input and a
     * {@link AMD64AddressValue memory} operand as second input.
     */
    public static class MemoryRMOp extends AMD64LIRInstruction implements ImplicitNullCheck
    {
        public static final LIRInstructionClass<MemoryRMOp> TYPE = LIRInstructionClass.create(MemoryRMOp.class);

        @Opcode private final AMD64RMOp opcode;
        private final OperandSize size;

        @Use({REG}) protected AllocatableValue x;
        @Use({COMPOSITE}) protected AMD64AddressValue y;

        @State protected LIRFrameState state;

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
            if (state != null)
            {
                crb.recordImplicitException(masm.position(), state);
            }
            opcode.emit(masm, size, asRegister(x), y.toAddress());
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
    public static class MemoryMROp extends AMD64LIRInstruction implements ImplicitNullCheck
    {
        public static final LIRInstructionClass<MemoryMROp> TYPE = LIRInstructionClass.create(MemoryMROp.class);

        @Opcode private final AMD64MROp opcode;
        private final OperandSize size;

        @Use({COMPOSITE}) protected AMD64AddressValue x;
        @Use({REG}) protected AllocatableValue y;

        @State protected LIRFrameState state;

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
            if (state != null)
            {
                crb.recordImplicitException(masm.position(), state);
            }
            opcode.emit(masm, size, x.toAddress(), asRegister(y));
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
     * Instruction that has one {@link AMD64AddressValue memory} operand and one 32-bit immediate
     * operand.
     */
    public static class MemoryConstOp extends AMD64LIRInstruction implements ImplicitNullCheck
    {
        public static final LIRInstructionClass<MemoryConstOp> TYPE = LIRInstructionClass.create(MemoryConstOp.class);

        @Opcode private final AMD64MIOp opcode;
        private final OperandSize size;

        @Use({COMPOSITE}) protected AMD64AddressValue x;
        private final int y;

        @State protected LIRFrameState state;

        public MemoryConstOp(AMD64BinaryArithmetic opcode, OperandSize size, AMD64AddressValue x, int y, LIRFrameState state)
        {
            this(opcode.getMIOpcode(size, NumUtil.isByte(y)), size, x, y, state);
        }

        public MemoryConstOp(AMD64MIOp opcode, OperandSize size, AMD64AddressValue x, int y, LIRFrameState state)
        {
            this(TYPE, opcode, size, x, y, state);
        }

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
            if (state != null)
            {
                crb.recordImplicitException(masm.position(), state);
            }
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
    public static class MemoryVMConstOp extends MemoryConstOp
    {
        public static final LIRInstructionClass<MemoryVMConstOp> TYPE = LIRInstructionClass.create(MemoryVMConstOp.class);

        protected final VMConstant c;

        public MemoryVMConstOp(AMD64MIOp opcode, AMD64AddressValue x, VMConstant c, LIRFrameState state)
        {
            super(TYPE, opcode, DWORD, x, 0xDEADDEAD, state);
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
