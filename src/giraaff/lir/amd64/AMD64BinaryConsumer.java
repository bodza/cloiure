package giraaff.lir.amd64;

import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.code.site.DataSectionReference;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.VMConstant;
import jdk.vm.ci.meta.Value;

import giraaff.asm.amd64.AMD64Address;
import giraaff.asm.amd64.AMD64Assembler;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.core.common.NumUtil;
import giraaff.lir.LIRFrameState;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.LIROpcode;
import giraaff.lir.StandardOp;
import giraaff.lir.asm.CompilationResultBuilder;

///
// AMD64 LIR instructions that have two input operands, but no output operand.
///
// @class AMD64BinaryConsumer
public final class AMD64BinaryConsumer
{
    ///
    // Instruction that has two {@link AllocatableValue} operands.
    ///
    // @class AMD64BinaryConsumer.ConsumerOp
    public static final class ConsumerOp extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<AMD64BinaryConsumer.ConsumerOp> TYPE = LIRInstructionClass.create(AMD64BinaryConsumer.ConsumerOp.class);

        @LIROpcode
        // @field
        private final AMD64Assembler.AMD64RMOp ___opcode;
        // @field
        private final AMD64Assembler.OperandSize ___size;

        @LIRInstruction.Use({LIRInstruction.OperandFlag.REG})
        // @field
        protected AllocatableValue ___x;
        @LIRInstruction.Use({LIRInstruction.OperandFlag.REG, LIRInstruction.OperandFlag.STACK})
        // @field
        protected AllocatableValue ___y;

        // @cons AMD64BinaryConsumer.ConsumerOp
        public ConsumerOp(AMD64Assembler.AMD64RMOp __opcode, AMD64Assembler.OperandSize __size, AllocatableValue __x, AllocatableValue __y)
        {
            super(TYPE);
            this.___opcode = __opcode;
            this.___size = __size;

            this.___x = __x;
            this.___y = __y;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            if (ValueUtil.isRegister(this.___y))
            {
                this.___opcode.emit(__masm, this.___size, ValueUtil.asRegister(this.___x), ValueUtil.asRegister(this.___y));
            }
            else
            {
                this.___opcode.emit(__masm, this.___size, ValueUtil.asRegister(this.___x), (AMD64Address) __crb.asAddress(this.___y));
            }
        }
    }

    ///
    // Instruction that has one {@link AllocatableValue} operand and one 32-bit immediate operand.
    ///
    // @class AMD64BinaryConsumer.ConsumerConstOp
    public static class ConsumerConstOp extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<AMD64BinaryConsumer.ConsumerConstOp> TYPE = LIRInstructionClass.create(AMD64BinaryConsumer.ConsumerConstOp.class);

        @LIROpcode
        // @field
        private final AMD64Assembler.AMD64MIOp ___opcode;
        // @field
        private final AMD64Assembler.OperandSize ___size;

        @LIRInstruction.Use({LIRInstruction.OperandFlag.REG, LIRInstruction.OperandFlag.STACK})
        // @field
        protected AllocatableValue ___x;
        // @field
        private final int ___y;

        // @cons AMD64BinaryConsumer.ConsumerConstOp
        public ConsumerConstOp(AMD64Assembler.AMD64BinaryArithmetic __opcode, AMD64Assembler.OperandSize __size, AllocatableValue __x, int __y)
        {
            this(__opcode.getMIOpcode(__size, NumUtil.isByte(__y)), __size, __x, __y);
        }

        // @cons AMD64BinaryConsumer.ConsumerConstOp
        public ConsumerConstOp(AMD64Assembler.AMD64MIOp __opcode, AMD64Assembler.OperandSize __size, AllocatableValue __x, int __y)
        {
            this(TYPE, __opcode, __size, __x, __y);
        }

        // @cons AMD64BinaryConsumer.ConsumerConstOp
        protected ConsumerConstOp(LIRInstructionClass<? extends AMD64BinaryConsumer.ConsumerConstOp> __c, AMD64Assembler.AMD64MIOp __opcode, AMD64Assembler.OperandSize __size, AllocatableValue __x, int __y)
        {
            super(__c);
            this.___opcode = __opcode;
            this.___size = __size;

            this.___x = __x;
            this.___y = __y;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            if (ValueUtil.isRegister(this.___x))
            {
                this.___opcode.emit(__masm, this.___size, ValueUtil.asRegister(this.___x), this.___y);
            }
            else
            {
                this.___opcode.emit(__masm, this.___size, (AMD64Address) __crb.asAddress(this.___x), this.___y);
            }
        }
    }

    ///
    // Instruction that has one {@link AllocatableValue} operand and one 32-bit immediate operand
    // that needs to be patched at runtime.
    ///
    // @class AMD64BinaryConsumer.VMConstOp
    public static final class VMConstOp extends AMD64BinaryConsumer.ConsumerConstOp
    {
        // @def
        public static final LIRInstructionClass<AMD64BinaryConsumer.VMConstOp> TYPE = LIRInstructionClass.create(AMD64BinaryConsumer.VMConstOp.class);

        // @field
        protected final VMConstant ___c;

        // @cons AMD64BinaryConsumer.VMConstOp
        public VMConstOp(AMD64Assembler.AMD64MIOp __opcode, AllocatableValue __x, VMConstant __c)
        {
            super(TYPE, __opcode, AMD64Assembler.OperandSize.DWORD, __x, 0xDEADDEAD);
            this.___c = __c;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            __crb.recordInlineDataInCode(this.___c);
            super.emitCode(__crb, __masm);
        }
    }

    ///
    // Instruction that has one {@link AllocatableValue} operand and one
    // {@link DataSectionReference} operand.
    ///
    // @class AMD64BinaryConsumer.DataOp
    public static final class DataOp extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<AMD64BinaryConsumer.DataOp> TYPE = LIRInstructionClass.create(AMD64BinaryConsumer.DataOp.class);

        @LIROpcode
        // @field
        private final AMD64Assembler.AMD64RMOp ___opcode;
        // @field
        private final AMD64Assembler.OperandSize ___size;

        @LIRInstruction.Use({LIRInstruction.OperandFlag.REG})
        // @field
        protected AllocatableValue ___x;
        // @field
        private final Constant ___y;

        // @field
        private final int ___alignment;

        // @cons AMD64BinaryConsumer.DataOp
        public DataOp(AMD64Assembler.AMD64RMOp __opcode, AMD64Assembler.OperandSize __size, AllocatableValue __x, Constant __y)
        {
            this(__opcode, __size, __x, __y, __size.getBytes());
        }

        // @cons AMD64BinaryConsumer.DataOp
        public DataOp(AMD64Assembler.AMD64RMOp __opcode, AMD64Assembler.OperandSize __size, AllocatableValue __x, Constant __y, int __alignment)
        {
            super(TYPE);
            this.___opcode = __opcode;
            this.___size = __size;

            this.___x = __x;
            this.___y = __y;

            this.___alignment = __alignment;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            this.___opcode.emit(__masm, this.___size, ValueUtil.asRegister(this.___x), (AMD64Address) __crb.recordDataReferenceInCode(this.___y, this.___alignment));
        }
    }

    ///
    // Instruction that has an {@link AllocatableValue} as first input and a
    // {@link AMD64AddressValue memory} operand as second input.
    ///
    // @class AMD64BinaryConsumer.MemoryRMOp
    public static final class MemoryRMOp extends AMD64LIRInstruction implements StandardOp.ImplicitNullCheck
    {
        // @def
        public static final LIRInstructionClass<AMD64BinaryConsumer.MemoryRMOp> TYPE = LIRInstructionClass.create(AMD64BinaryConsumer.MemoryRMOp.class);

        @LIROpcode
        // @field
        private final AMD64Assembler.AMD64RMOp ___opcode;
        // @field
        private final AMD64Assembler.OperandSize ___size;

        @LIRInstruction.Use({LIRInstruction.OperandFlag.REG})
        // @field
        protected AllocatableValue ___x;
        @LIRInstruction.Use({LIRInstruction.OperandFlag.COMPOSITE})
        // @field
        protected AMD64AddressValue ___y;

        // @State
        // @field
        protected LIRFrameState ___state;

        // @cons AMD64BinaryConsumer.MemoryRMOp
        public MemoryRMOp(AMD64Assembler.AMD64RMOp __opcode, AMD64Assembler.OperandSize __size, AllocatableValue __x, AMD64AddressValue __y, LIRFrameState __state)
        {
            super(TYPE);
            this.___opcode = __opcode;
            this.___size = __size;

            this.___x = __x;
            this.___y = __y;

            this.___state = __state;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            this.___opcode.emit(__masm, this.___size, ValueUtil.asRegister(this.___x), this.___y.toAddress());
        }

        @Override
        public boolean makeNullCheckFor(Value __value, LIRFrameState __nullCheckState, int __implicitNullCheckLimit)
        {
            if (this.___state == null && this.___y.isValidImplicitNullCheckFor(__value, __implicitNullCheckLimit))
            {
                this.___state = __nullCheckState;
                return true;
            }
            return false;
        }
    }

    ///
    // Instruction that has a {@link AMD64AddressValue memory} operand as first input and an
    // {@link AllocatableValue} as second input.
    ///
    // @class AMD64BinaryConsumer.MemoryMROp
    public static final class MemoryMROp extends AMD64LIRInstruction implements StandardOp.ImplicitNullCheck
    {
        // @def
        public static final LIRInstructionClass<AMD64BinaryConsumer.MemoryMROp> TYPE = LIRInstructionClass.create(AMD64BinaryConsumer.MemoryMROp.class);

        @LIROpcode
        // @field
        private final AMD64Assembler.AMD64MROp ___opcode;
        // @field
        private final AMD64Assembler.OperandSize ___size;

        @LIRInstruction.Use({LIRInstruction.OperandFlag.COMPOSITE})
        // @field
        protected AMD64AddressValue ___x;
        @LIRInstruction.Use({LIRInstruction.OperandFlag.REG})
        // @field
        protected AllocatableValue ___y;

        // @State
        // @field
        protected LIRFrameState ___state;

        // @cons AMD64BinaryConsumer.MemoryMROp
        public MemoryMROp(AMD64Assembler.AMD64MROp __opcode, AMD64Assembler.OperandSize __size, AMD64AddressValue __x, AllocatableValue __y, LIRFrameState __state)
        {
            super(TYPE);
            this.___opcode = __opcode;
            this.___size = __size;

            this.___x = __x;
            this.___y = __y;

            this.___state = __state;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            this.___opcode.emit(__masm, this.___size, this.___x.toAddress(), ValueUtil.asRegister(this.___y));
        }

        @Override
        public boolean makeNullCheckFor(Value __value, LIRFrameState __nullCheckState, int __implicitNullCheckLimit)
        {
            if (this.___state == null && this.___x.isValidImplicitNullCheckFor(__value, __implicitNullCheckLimit))
            {
                this.___state = __nullCheckState;
                return true;
            }
            return false;
        }
    }

    ///
    // Instruction that has one {@link AMD64AddressValue memory} operand and one 32-bit immediate operand.
    ///
    // @class AMD64BinaryConsumer.MemoryConstOp
    public static class MemoryConstOp extends AMD64LIRInstruction implements StandardOp.ImplicitNullCheck
    {
        // @def
        public static final LIRInstructionClass<AMD64BinaryConsumer.MemoryConstOp> TYPE = LIRInstructionClass.create(AMD64BinaryConsumer.MemoryConstOp.class);

        @LIROpcode
        // @field
        private final AMD64Assembler.AMD64MIOp ___opcode;
        // @field
        private final AMD64Assembler.OperandSize ___size;

        @LIRInstruction.Use({LIRInstruction.OperandFlag.COMPOSITE})
        // @field
        protected AMD64AddressValue ___x;
        // @field
        private final int ___y;

        // @State
        // @field
        protected LIRFrameState ___state;

        // @cons AMD64BinaryConsumer.MemoryConstOp
        public MemoryConstOp(AMD64Assembler.AMD64BinaryArithmetic __opcode, AMD64Assembler.OperandSize __size, AMD64AddressValue __x, int __y, LIRFrameState __state)
        {
            this(__opcode.getMIOpcode(__size, NumUtil.isByte(__y)), __size, __x, __y, __state);
        }

        // @cons AMD64BinaryConsumer.MemoryConstOp
        public MemoryConstOp(AMD64Assembler.AMD64MIOp __opcode, AMD64Assembler.OperandSize __size, AMD64AddressValue __x, int __y, LIRFrameState __state)
        {
            this(TYPE, __opcode, __size, __x, __y, __state);
        }

        // @cons AMD64BinaryConsumer.MemoryConstOp
        protected MemoryConstOp(LIRInstructionClass<? extends AMD64BinaryConsumer.MemoryConstOp> __c, AMD64Assembler.AMD64MIOp __opcode, AMD64Assembler.OperandSize __size, AMD64AddressValue __x, int __y, LIRFrameState __state)
        {
            super(__c);
            this.___opcode = __opcode;
            this.___size = __size;

            this.___x = __x;
            this.___y = __y;

            this.___state = __state;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            this.___opcode.emit(__masm, this.___size, this.___x.toAddress(), this.___y);
        }

        @Override
        public boolean makeNullCheckFor(Value __value, LIRFrameState __nullCheckState, int __implicitNullCheckLimit)
        {
            if (this.___state == null && this.___x.isValidImplicitNullCheckFor(__value, __implicitNullCheckLimit))
            {
                this.___state = __nullCheckState;
                return true;
            }
            return false;
        }

        public AMD64Assembler.AMD64MIOp getOpcode()
        {
            return this.___opcode;
        }
    }

    ///
    // Instruction that has one {@link AMD64AddressValue memory} operand and one 32-bit immediate
    // operand that needs to be patched at runtime.
    ///
    // @class AMD64BinaryConsumer.MemoryVMConstOp
    public static final class MemoryVMConstOp extends AMD64BinaryConsumer.MemoryConstOp
    {
        // @def
        public static final LIRInstructionClass<AMD64BinaryConsumer.MemoryVMConstOp> TYPE = LIRInstructionClass.create(AMD64BinaryConsumer.MemoryVMConstOp.class);

        // @field
        protected final VMConstant ___c;

        // @cons AMD64BinaryConsumer.MemoryVMConstOp
        public MemoryVMConstOp(AMD64Assembler.AMD64MIOp __opcode, AMD64AddressValue __x, VMConstant __c, LIRFrameState __state)
        {
            super(TYPE, __opcode, AMD64Assembler.OperandSize.DWORD, __x, 0xDEADDEAD, __state);
            this.___c = __c;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            __crb.recordInlineDataInCode(this.___c);
            super.emitCode(__crb, __masm);
        }
    }
}
