package giraaff.lir.amd64;

import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.code.site.DataSectionReference;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.Value;

import giraaff.asm.amd64.AMD64Address;
import giraaff.asm.amd64.AMD64Assembler;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.core.common.NumUtil;
import giraaff.lir.LIRFrameState;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.LIRValueUtil;
import giraaff.lir.LIROpcode;
import giraaff.lir.StandardOp;
import giraaff.lir.asm.CompilationResultBuilder;

///
// AMD64 LIR instructions that have two inputs and one output.
///
// @class AMD64Binary
public final class AMD64Binary
{
    ///
    // Instruction that has two {@link AllocatableValue} operands.
    ///
    // @class AMD64Binary.TwoOp
    public static final class TwoOp extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<AMD64Binary.TwoOp> TYPE = LIRInstructionClass.create(AMD64Binary.TwoOp.class);

        @LIROpcode
        // @field
        private final AMD64Assembler.AMD64RMOp ___opcode;
        // @field
        private final AMD64Assembler.OperandSize ___size;

        @LIRInstruction.Def({LIRInstruction.OperandFlag.REG, LIRInstruction.OperandFlag.HINT})
        // @field
        protected AllocatableValue ___result;
        @LIRInstruction.Use({LIRInstruction.OperandFlag.REG})
        // @field
        protected AllocatableValue ___x;
        ///
        // This argument must be LIRInstruction.Alive to ensure that result and y are not assigned
        // to the same register, which would break the code generation by destroying y too early.
        ///
        @LIRInstruction.Alive({LIRInstruction.OperandFlag.REG, LIRInstruction.OperandFlag.STACK})
        // @field
        protected AllocatableValue ___y;

        // @cons AMD64Binary.TwoOp
        public TwoOp(AMD64Assembler.AMD64RMOp __opcode, AMD64Assembler.OperandSize __size, AllocatableValue __result, AllocatableValue __x, AllocatableValue __y)
        {
            super(TYPE);
            this.___opcode = __opcode;
            this.___size = __size;

            this.___result = __result;
            this.___x = __x;
            this.___y = __y;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            AMD64Move.move(__crb, __masm, this.___result, this.___x);
            if (ValueUtil.isRegister(this.___y))
            {
                this.___opcode.emit(__masm, this.___size, ValueUtil.asRegister(this.___result), ValueUtil.asRegister(this.___y));
            }
            else
            {
                this.___opcode.emit(__masm, this.___size, ValueUtil.asRegister(this.___result), (AMD64Address) __crb.asAddress(this.___y));
            }
        }
    }

    ///
    // Instruction that has three {@link AllocatableValue} operands.
    ///
    // @class AMD64Binary.ThreeOp
    public static final class ThreeOp extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<AMD64Binary.ThreeOp> TYPE = LIRInstructionClass.create(AMD64Binary.ThreeOp.class);

        @LIROpcode
        // @field
        private final AMD64Assembler.AMD64RRMOp ___opcode;
        // @field
        private final AMD64Assembler.OperandSize ___size;

        @LIRInstruction.Def({LIRInstruction.OperandFlag.REG})
        // @field
        protected AllocatableValue ___result;
        @LIRInstruction.Use({LIRInstruction.OperandFlag.REG})
        // @field
        protected AllocatableValue ___x;
        @LIRInstruction.Use({LIRInstruction.OperandFlag.REG, LIRInstruction.OperandFlag.STACK})
        // @field
        protected AllocatableValue ___y;

        // @cons AMD64Binary.ThreeOp
        public ThreeOp(AMD64Assembler.AMD64RRMOp __opcode, AMD64Assembler.OperandSize __size, AllocatableValue __result, AllocatableValue __x, AllocatableValue __y)
        {
            super(TYPE);
            this.___opcode = __opcode;
            this.___size = __size;

            this.___result = __result;
            this.___x = __x;
            this.___y = __y;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            if (ValueUtil.isRegister(this.___y))
            {
                this.___opcode.emit(__masm, this.___size, ValueUtil.asRegister(this.___result), ValueUtil.asRegister(this.___x), ValueUtil.asRegister(this.___y));
            }
            else
            {
                this.___opcode.emit(__masm, this.___size, ValueUtil.asRegister(this.___result), ValueUtil.asRegister(this.___x), (AMD64Address) __crb.asAddress(this.___y));
            }
        }
    }

    ///
    // Commutative instruction that has two {@link AllocatableValue} operands.
    ///
    // @class AMD64Binary.CommutativeTwoOp
    public static final class CommutativeTwoOp extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<AMD64Binary.CommutativeTwoOp> TYPE = LIRInstructionClass.create(AMD64Binary.CommutativeTwoOp.class);

        @LIROpcode
        // @field
        private final AMD64Assembler.AMD64RMOp ___opcode;
        // @field
        private final AMD64Assembler.OperandSize ___size;

        @LIRInstruction.Def({LIRInstruction.OperandFlag.REG, LIRInstruction.OperandFlag.HINT})
        // @field
        protected AllocatableValue ___result;
        @LIRInstruction.Use({LIRInstruction.OperandFlag.REG, LIRInstruction.OperandFlag.STACK})
        // @field
        protected AllocatableValue ___x;
        @LIRInstruction.Use({LIRInstruction.OperandFlag.REG, LIRInstruction.OperandFlag.STACK})
        // @field
        protected AllocatableValue ___y;

        // @cons AMD64Binary.CommutativeTwoOp
        public CommutativeTwoOp(AMD64Assembler.AMD64RMOp __opcode, AMD64Assembler.OperandSize __size, AllocatableValue __result, AllocatableValue __x, AllocatableValue __y)
        {
            super(TYPE);
            this.___opcode = __opcode;
            this.___size = __size;

            this.___result = __result;
            this.___x = __x;
            this.___y = __y;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            AllocatableValue __input;
            if (LIRValueUtil.sameRegister(this.___result, this.___y))
            {
                __input = this.___x;
            }
            else
            {
                AMD64Move.move(__crb, __masm, this.___result, this.___x);
                __input = this.___y;
            }

            if (ValueUtil.isRegister(__input))
            {
                this.___opcode.emit(__masm, this.___size, ValueUtil.asRegister(this.___result), ValueUtil.asRegister(__input));
            }
            else
            {
                this.___opcode.emit(__masm, this.___size, ValueUtil.asRegister(this.___result), (AMD64Address) __crb.asAddress(__input));
            }
        }
    }

    ///
    // Commutative instruction that has three {@link AllocatableValue} operands.
    ///
    // @class AMD64Binary.CommutativeThreeOp
    public static final class CommutativeThreeOp extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<AMD64Binary.CommutativeThreeOp> TYPE = LIRInstructionClass.create(AMD64Binary.CommutativeThreeOp.class);

        @LIROpcode
        // @field
        private final AMD64Assembler.AMD64RRMOp ___opcode;
        // @field
        private final AMD64Assembler.OperandSize ___size;

        @LIRInstruction.Def({LIRInstruction.OperandFlag.REG})
        // @field
        protected AllocatableValue ___result;
        @LIRInstruction.Use({LIRInstruction.OperandFlag.REG})
        // @field
        protected AllocatableValue ___x;
        @LIRInstruction.Use({LIRInstruction.OperandFlag.REG, LIRInstruction.OperandFlag.STACK})
        // @field
        protected AllocatableValue ___y;

        // @cons AMD64Binary.CommutativeThreeOp
        public CommutativeThreeOp(AMD64Assembler.AMD64RRMOp __opcode, AMD64Assembler.OperandSize __size, AllocatableValue __result, AllocatableValue __x, AllocatableValue __y)
        {
            super(TYPE);
            this.___opcode = __opcode;
            this.___size = __size;

            this.___result = __result;
            this.___x = __x;
            this.___y = __y;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            if (ValueUtil.isRegister(this.___y))
            {
                this.___opcode.emit(__masm, this.___size, ValueUtil.asRegister(this.___result), ValueUtil.asRegister(this.___x), ValueUtil.asRegister(this.___y));
            }
            else
            {
                this.___opcode.emit(__masm, this.___size, ValueUtil.asRegister(this.___result), ValueUtil.asRegister(this.___x), (AMD64Address) __crb.asAddress(this.___y));
            }
        }
    }

    ///
    // Instruction that has one {@link AllocatableValue} operand and one 32-bit immediate operand.
    ///
    // @class AMD64Binary.ConstOp
    public static final class ConstOp extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<AMD64Binary.ConstOp> TYPE = LIRInstructionClass.create(AMD64Binary.ConstOp.class);

        @LIROpcode
        // @field
        private final AMD64Assembler.AMD64MIOp ___opcode;
        // @field
        private final AMD64Assembler.OperandSize ___size;

        @LIRInstruction.Def({LIRInstruction.OperandFlag.REG, LIRInstruction.OperandFlag.HINT})
        // @field
        protected AllocatableValue ___result;
        @LIRInstruction.Use({LIRInstruction.OperandFlag.REG})
        // @field
        protected AllocatableValue ___x;
        // @field
        private final int ___y;

        // @cons AMD64Binary.ConstOp
        public ConstOp(AMD64Assembler.AMD64BinaryArithmetic __opcode, AMD64Assembler.OperandSize __size, AllocatableValue __result, AllocatableValue __x, int __y)
        {
            this(__opcode.getMIOpcode(__size, NumUtil.isByte(__y)), __size, __result, __x, __y);
        }

        // @cons AMD64Binary.ConstOp
        public ConstOp(AMD64Assembler.AMD64MIOp __opcode, AMD64Assembler.OperandSize __size, AllocatableValue __result, AllocatableValue __x, int __y)
        {
            super(TYPE);
            this.___opcode = __opcode;
            this.___size = __size;

            this.___result = __result;
            this.___x = __x;
            this.___y = __y;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            AMD64Move.move(__crb, __masm, this.___result, this.___x);
            this.___opcode.emit(__masm, this.___size, ValueUtil.asRegister(this.___result), this.___y);
        }
    }

    ///
    // Instruction that has one {@link AllocatableValue} operand and one
    // {@link DataSectionReference} operand.
    ///
    // @class AMD64Binary.DataTwoOp
    public static final class DataTwoOp extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<AMD64Binary.DataTwoOp> TYPE = LIRInstructionClass.create(AMD64Binary.DataTwoOp.class);

        @LIROpcode
        // @field
        private final AMD64Assembler.AMD64RMOp ___opcode;
        // @field
        private final AMD64Assembler.OperandSize ___size;

        @LIRInstruction.Def({LIRInstruction.OperandFlag.REG, LIRInstruction.OperandFlag.HINT})
        // @field
        protected AllocatableValue ___result;
        @LIRInstruction.Use({LIRInstruction.OperandFlag.REG})
        // @field
        protected AllocatableValue ___x;
        // @field
        private final JavaConstant ___y;

        // @field
        private final int ___alignment;

        // @cons AMD64Binary.DataTwoOp
        public DataTwoOp(AMD64Assembler.AMD64RMOp __opcode, AMD64Assembler.OperandSize __size, AllocatableValue __result, AllocatableValue __x, JavaConstant __y)
        {
            this(__opcode, __size, __result, __x, __y, __y.getJavaKind().getByteCount());
        }

        // @cons AMD64Binary.DataTwoOp
        public DataTwoOp(AMD64Assembler.AMD64RMOp __opcode, AMD64Assembler.OperandSize __size, AllocatableValue __result, AllocatableValue __x, JavaConstant __y, int __alignment)
        {
            super(TYPE);
            this.___opcode = __opcode;
            this.___size = __size;

            this.___result = __result;
            this.___x = __x;
            this.___y = __y;

            this.___alignment = __alignment;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            AMD64Move.move(__crb, __masm, this.___result, this.___x);
            this.___opcode.emit(__masm, this.___size, ValueUtil.asRegister(this.___result), (AMD64Address) __crb.recordDataReferenceInCode(this.___y, this.___alignment));
        }
    }

    ///
    // Instruction that has two {@link AllocatableValue} operands and one
    // {@link DataSectionReference} operand.
    ///
    // @class AMD64Binary.DataThreeOp
    public static final class DataThreeOp extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<AMD64Binary.DataThreeOp> TYPE = LIRInstructionClass.create(AMD64Binary.DataThreeOp.class);

        @LIROpcode
        // @field
        private final AMD64Assembler.AMD64RRMOp ___opcode;
        // @field
        private final AMD64Assembler.OperandSize ___size;

        @LIRInstruction.Def({LIRInstruction.OperandFlag.REG})
        // @field
        protected AllocatableValue ___result;
        @LIRInstruction.Use({LIRInstruction.OperandFlag.REG})
        // @field
        protected AllocatableValue ___x;
        // @field
        private final JavaConstant ___y;

        // @field
        private final int ___alignment;

        // @cons AMD64Binary.DataThreeOp
        public DataThreeOp(AMD64Assembler.AMD64RRMOp __opcode, AMD64Assembler.OperandSize __size, AllocatableValue __result, AllocatableValue __x, JavaConstant __y)
        {
            this(__opcode, __size, __result, __x, __y, __y.getJavaKind().getByteCount());
        }

        // @cons AMD64Binary.DataThreeOp
        public DataThreeOp(AMD64Assembler.AMD64RRMOp __opcode, AMD64Assembler.OperandSize __size, AllocatableValue __result, AllocatableValue __x, JavaConstant __y, int __alignment)
        {
            super(TYPE);
            this.___opcode = __opcode;
            this.___size = __size;

            this.___result = __result;
            this.___x = __x;
            this.___y = __y;

            this.___alignment = __alignment;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            this.___opcode.emit(__masm, this.___size, ValueUtil.asRegister(this.___result), ValueUtil.asRegister(this.___x), (AMD64Address) __crb.recordDataReferenceInCode(this.___y, this.___alignment));
        }
    }

    ///
    // Instruction that has one {@link AllocatableValue} operand and one {@link AMD64AddressValue
    // memory} operand.
    ///
    // @class AMD64Binary.MemoryTwoOp
    public static final class MemoryTwoOp extends AMD64LIRInstruction implements StandardOp.ImplicitNullCheck
    {
        // @def
        public static final LIRInstructionClass<AMD64Binary.MemoryTwoOp> TYPE = LIRInstructionClass.create(AMD64Binary.MemoryTwoOp.class);

        @LIROpcode
        // @field
        private final AMD64Assembler.AMD64RMOp ___opcode;
        // @field
        private final AMD64Assembler.OperandSize ___size;

        @LIRInstruction.Def({LIRInstruction.OperandFlag.REG, LIRInstruction.OperandFlag.HINT})
        // @field
        protected AllocatableValue ___result;
        @LIRInstruction.Use({LIRInstruction.OperandFlag.REG})
        // @field
        protected AllocatableValue ___x;
        @LIRInstruction.Alive({LIRInstruction.OperandFlag.COMPOSITE})
        // @field
        protected AMD64AddressValue ___y;

        // @State
        // @field
        protected LIRFrameState ___state;

        // @cons AMD64Binary.MemoryTwoOp
        public MemoryTwoOp(AMD64Assembler.AMD64RMOp __opcode, AMD64Assembler.OperandSize __size, AllocatableValue __result, AllocatableValue __x, AMD64AddressValue __y, LIRFrameState __state)
        {
            super(TYPE);
            this.___opcode = __opcode;
            this.___size = __size;

            this.___result = __result;
            this.___x = __x;
            this.___y = __y;

            this.___state = __state;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            AMD64Move.move(__crb, __masm, this.___result, this.___x);
            this.___opcode.emit(__masm, this.___size, ValueUtil.asRegister(this.___result), this.___y.toAddress());
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
    // Instruction that has one {@link AllocatableValue} operand and one {@link AMD64AddressValue
    // memory} operand.
    ///
    // @class AMD64Binary.MemoryThreeOp
    public static final class MemoryThreeOp extends AMD64LIRInstruction implements StandardOp.ImplicitNullCheck
    {
        // @def
        public static final LIRInstructionClass<AMD64Binary.MemoryThreeOp> TYPE = LIRInstructionClass.create(AMD64Binary.MemoryThreeOp.class);

        @LIROpcode
        // @field
        private final AMD64Assembler.AMD64RRMOp ___opcode;
        // @field
        private final AMD64Assembler.OperandSize ___size;

        @LIRInstruction.Def({LIRInstruction.OperandFlag.REG})
        // @field
        protected AllocatableValue ___result;
        @LIRInstruction.Use({LIRInstruction.OperandFlag.REG})
        // @field
        protected AllocatableValue ___x;
        @LIRInstruction.Use({LIRInstruction.OperandFlag.COMPOSITE})
        // @field
        protected AMD64AddressValue ___y;

        // @State
        // @field
        protected LIRFrameState ___state;

        // @cons AMD64Binary.MemoryThreeOp
        public MemoryThreeOp(AMD64Assembler.AMD64RRMOp __opcode, AMD64Assembler.OperandSize __size, AllocatableValue __result, AllocatableValue __x, AMD64AddressValue __y, LIRFrameState __state)
        {
            super(TYPE);
            this.___opcode = __opcode;
            this.___size = __size;

            this.___result = __result;
            this.___x = __x;
            this.___y = __y;

            this.___state = __state;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            this.___opcode.emit(__masm, this.___size, ValueUtil.asRegister(this.___result), ValueUtil.asRegister(this.___x), this.___y.toAddress());
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
    // Instruction with a separate result operand, one {@link AllocatableValue} input and one 32-bit
    // immediate input.
    ///
    // @class AMD64Binary.RMIOp
    public static final class RMIOp extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<AMD64Binary.RMIOp> TYPE = LIRInstructionClass.create(AMD64Binary.RMIOp.class);

        @LIROpcode
        // @field
        private final AMD64Assembler.AMD64RMIOp ___opcode;
        // @field
        private final AMD64Assembler.OperandSize ___size;

        @LIRInstruction.Def({LIRInstruction.OperandFlag.REG})
        // @field
        protected AllocatableValue ___result;
        @LIRInstruction.Use({LIRInstruction.OperandFlag.REG, LIRInstruction.OperandFlag.STACK})
        // @field
        protected AllocatableValue ___x;
        // @field
        private final int ___y;

        // @cons AMD64Binary.RMIOp
        public RMIOp(AMD64Assembler.AMD64RMIOp __opcode, AMD64Assembler.OperandSize __size, AllocatableValue __result, AllocatableValue __x, int __y)
        {
            super(TYPE);
            this.___opcode = __opcode;
            this.___size = __size;

            this.___result = __result;
            this.___x = __x;
            this.___y = __y;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            if (ValueUtil.isRegister(this.___x))
            {
                this.___opcode.emit(__masm, this.___size, ValueUtil.asRegister(this.___result), ValueUtil.asRegister(this.___x), this.___y);
            }
            else
            {
                this.___opcode.emit(__masm, this.___size, ValueUtil.asRegister(this.___result), (AMD64Address) __crb.asAddress(this.___x), this.___y);
            }
        }
    }
}
