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
        // @def
        public static final LIRInstructionClass<Op> TYPE = LIRInstructionClass.create(Op.class);

        @Opcode
        // @field
        private final AMD64RMOp opcode;
        // @field
        private final OperandSize size;

        @Use({OperandFlag.REG})
        // @field
        protected AllocatableValue x;
        @Use({OperandFlag.REG, OperandFlag.STACK})
        // @field
        protected AllocatableValue y;

        // @cons
        public Op(AMD64RMOp __opcode, OperandSize __size, AllocatableValue __x, AllocatableValue __y)
        {
            super(TYPE);
            this.opcode = __opcode;
            this.size = __size;

            this.x = __x;
            this.y = __y;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            if (ValueUtil.isRegister(y))
            {
                opcode.emit(__masm, size, ValueUtil.asRegister(x), ValueUtil.asRegister(y));
            }
            else
            {
                opcode.emit(__masm, size, ValueUtil.asRegister(x), (AMD64Address) __crb.asAddress(y));
            }
        }
    }

    /**
     * Instruction that has one {@link AllocatableValue} operand and one 32-bit immediate operand.
     */
    // @class AMD64BinaryConsumer.ConstOp
    public static class ConstOp extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<ConstOp> TYPE = LIRInstructionClass.create(ConstOp.class);

        @Opcode
        // @field
        private final AMD64MIOp opcode;
        // @field
        private final OperandSize size;

        @Use({OperandFlag.REG, OperandFlag.STACK})
        // @field
        protected AllocatableValue x;
        // @field
        private final int y;

        // @cons
        public ConstOp(AMD64BinaryArithmetic __opcode, OperandSize __size, AllocatableValue __x, int __y)
        {
            this(__opcode.getMIOpcode(__size, NumUtil.isByte(__y)), __size, __x, __y);
        }

        // @cons
        public ConstOp(AMD64MIOp __opcode, OperandSize __size, AllocatableValue __x, int __y)
        {
            this(TYPE, __opcode, __size, __x, __y);
        }

        // @cons
        protected ConstOp(LIRInstructionClass<? extends ConstOp> __c, AMD64MIOp __opcode, OperandSize __size, AllocatableValue __x, int __y)
        {
            super(__c);
            this.opcode = __opcode;
            this.size = __size;

            this.x = __x;
            this.y = __y;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            if (ValueUtil.isRegister(x))
            {
                opcode.emit(__masm, size, ValueUtil.asRegister(x), y);
            }
            else
            {
                opcode.emit(__masm, size, (AMD64Address) __crb.asAddress(x), y);
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
        // @def
        public static final LIRInstructionClass<VMConstOp> TYPE = LIRInstructionClass.create(VMConstOp.class);

        // @field
        protected final VMConstant c;

        // @cons
        public VMConstOp(AMD64MIOp __opcode, AllocatableValue __x, VMConstant __c)
        {
            super(TYPE, __opcode, OperandSize.DWORD, __x, 0xDEADDEAD);
            this.c = __c;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            __crb.recordInlineDataInCode(c);
            super.emitCode(__crb, __masm);
        }
    }

    /**
     * Instruction that has one {@link AllocatableValue} operand and one
     * {@link DataSectionReference} operand.
     */
    // @class AMD64BinaryConsumer.DataOp
    public static final class DataOp extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<DataOp> TYPE = LIRInstructionClass.create(DataOp.class);

        @Opcode
        // @field
        private final AMD64RMOp opcode;
        // @field
        private final OperandSize size;

        @Use({OperandFlag.REG})
        // @field
        protected AllocatableValue x;
        // @field
        private final Constant y;

        // @field
        private final int alignment;

        // @cons
        public DataOp(AMD64RMOp __opcode, OperandSize __size, AllocatableValue __x, Constant __y)
        {
            this(__opcode, __size, __x, __y, __size.getBytes());
        }

        // @cons
        public DataOp(AMD64RMOp __opcode, OperandSize __size, AllocatableValue __x, Constant __y, int __alignment)
        {
            super(TYPE);
            this.opcode = __opcode;
            this.size = __size;

            this.x = __x;
            this.y = __y;

            this.alignment = __alignment;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            opcode.emit(__masm, size, ValueUtil.asRegister(x), (AMD64Address) __crb.recordDataReferenceInCode(y, alignment));
        }
    }

    /**
     * Instruction that has an {@link AllocatableValue} as first input and a
     * {@link AMD64AddressValue memory} operand as second input.
     */
    // @class AMD64BinaryConsumer.MemoryRMOp
    public static final class MemoryRMOp extends AMD64LIRInstruction implements ImplicitNullCheck
    {
        // @def
        public static final LIRInstructionClass<MemoryRMOp> TYPE = LIRInstructionClass.create(MemoryRMOp.class);

        @Opcode
        // @field
        private final AMD64RMOp opcode;
        // @field
        private final OperandSize size;

        @Use({OperandFlag.REG})
        // @field
        protected AllocatableValue x;
        @Use({OperandFlag.COMPOSITE})
        // @field
        protected AMD64AddressValue y;

        // @State
        // @field
        protected LIRFrameState state;

        // @cons
        public MemoryRMOp(AMD64RMOp __opcode, OperandSize __size, AllocatableValue __x, AMD64AddressValue __y, LIRFrameState __state)
        {
            super(TYPE);
            this.opcode = __opcode;
            this.size = __size;

            this.x = __x;
            this.y = __y;

            this.state = __state;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            opcode.emit(__masm, size, ValueUtil.asRegister(x), y.toAddress());
        }

        @Override
        public boolean makeNullCheckFor(Value __value, LIRFrameState __nullCheckState, int __implicitNullCheckLimit)
        {
            if (state == null && y.isValidImplicitNullCheckFor(__value, __implicitNullCheckLimit))
            {
                state = __nullCheckState;
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
        // @def
        public static final LIRInstructionClass<MemoryMROp> TYPE = LIRInstructionClass.create(MemoryMROp.class);

        @Opcode
        // @field
        private final AMD64MROp opcode;
        // @field
        private final OperandSize size;

        @Use({OperandFlag.COMPOSITE})
        // @field
        protected AMD64AddressValue x;
        @Use({OperandFlag.REG})
        // @field
        protected AllocatableValue y;

        // @State
        // @field
        protected LIRFrameState state;

        // @cons
        public MemoryMROp(AMD64MROp __opcode, OperandSize __size, AMD64AddressValue __x, AllocatableValue __y, LIRFrameState __state)
        {
            super(TYPE);
            this.opcode = __opcode;
            this.size = __size;

            this.x = __x;
            this.y = __y;

            this.state = __state;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            opcode.emit(__masm, size, x.toAddress(), ValueUtil.asRegister(y));
        }

        @Override
        public boolean makeNullCheckFor(Value __value, LIRFrameState __nullCheckState, int __implicitNullCheckLimit)
        {
            if (state == null && x.isValidImplicitNullCheckFor(__value, __implicitNullCheckLimit))
            {
                state = __nullCheckState;
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
        // @def
        public static final LIRInstructionClass<MemoryConstOp> TYPE = LIRInstructionClass.create(MemoryConstOp.class);

        @Opcode
        // @field
        private final AMD64MIOp opcode;
        // @field
        private final OperandSize size;

        @Use({OperandFlag.COMPOSITE})
        // @field
        protected AMD64AddressValue x;
        // @field
        private final int y;

        // @State
        // @field
        protected LIRFrameState state;

        // @cons
        public MemoryConstOp(AMD64BinaryArithmetic __opcode, OperandSize __size, AMD64AddressValue __x, int __y, LIRFrameState __state)
        {
            this(__opcode.getMIOpcode(__size, NumUtil.isByte(__y)), __size, __x, __y, __state);
        }

        // @cons
        public MemoryConstOp(AMD64MIOp __opcode, OperandSize __size, AMD64AddressValue __x, int __y, LIRFrameState __state)
        {
            this(TYPE, __opcode, __size, __x, __y, __state);
        }

        // @cons
        protected MemoryConstOp(LIRInstructionClass<? extends MemoryConstOp> __c, AMD64MIOp __opcode, OperandSize __size, AMD64AddressValue __x, int __y, LIRFrameState __state)
        {
            super(__c);
            this.opcode = __opcode;
            this.size = __size;

            this.x = __x;
            this.y = __y;

            this.state = __state;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            opcode.emit(__masm, size, x.toAddress(), y);
        }

        @Override
        public boolean makeNullCheckFor(Value __value, LIRFrameState __nullCheckState, int __implicitNullCheckLimit)
        {
            if (state == null && x.isValidImplicitNullCheckFor(__value, __implicitNullCheckLimit))
            {
                state = __nullCheckState;
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
        // @def
        public static final LIRInstructionClass<MemoryVMConstOp> TYPE = LIRInstructionClass.create(MemoryVMConstOp.class);

        // @field
        protected final VMConstant c;

        // @cons
        public MemoryVMConstOp(AMD64MIOp __opcode, AMD64AddressValue __x, VMConstant __c, LIRFrameState __state)
        {
            super(TYPE, __opcode, OperandSize.DWORD, __x, 0xDEADDEAD, __state);
            this.c = __c;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            __crb.recordInlineDataInCode(c);
            super.emitCode(__crb, __masm);
        }
    }
}
