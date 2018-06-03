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
        public static final LIRInstructionClass<TwoOp> TYPE = LIRInstructionClass.create(TwoOp.class);

        @Opcode
        // @field
        private final AMD64RMOp ___opcode;
        // @field
        private final OperandSize ___size;

        @Def({OperandFlag.REG, OperandFlag.HINT})
        // @field
        protected AllocatableValue ___result;
        @Use({OperandFlag.REG})
        // @field
        protected AllocatableValue ___x;
        ///
        // This argument must be Alive to ensure that result and y are not assigned to the
        // same register, which would break the code generation by destroying y too early.
        ///
        @Alive({OperandFlag.REG, OperandFlag.STACK})
        // @field
        protected AllocatableValue ___y;

        // @cons
        public TwoOp(AMD64RMOp __opcode, OperandSize __size, AllocatableValue __result, AllocatableValue __x, AllocatableValue __y)
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
        public static final LIRInstructionClass<ThreeOp> TYPE = LIRInstructionClass.create(ThreeOp.class);

        @Opcode
        // @field
        private final AMD64RRMOp ___opcode;
        // @field
        private final OperandSize ___size;

        @Def({OperandFlag.REG})
        // @field
        protected AllocatableValue ___result;
        @Use({OperandFlag.REG})
        // @field
        protected AllocatableValue ___x;
        @Use({OperandFlag.REG, OperandFlag.STACK})
        // @field
        protected AllocatableValue ___y;

        // @cons
        public ThreeOp(AMD64RRMOp __opcode, OperandSize __size, AllocatableValue __result, AllocatableValue __x, AllocatableValue __y)
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
        public static final LIRInstructionClass<CommutativeTwoOp> TYPE = LIRInstructionClass.create(CommutativeTwoOp.class);

        @Opcode
        // @field
        private final AMD64RMOp ___opcode;
        // @field
        private final OperandSize ___size;

        @Def({OperandFlag.REG, OperandFlag.HINT})
        // @field
        protected AllocatableValue ___result;
        @Use({OperandFlag.REG, OperandFlag.STACK})
        // @field
        protected AllocatableValue ___x;
        @Use({OperandFlag.REG, OperandFlag.STACK})
        // @field
        protected AllocatableValue ___y;

        // @cons
        public CommutativeTwoOp(AMD64RMOp __opcode, OperandSize __size, AllocatableValue __result, AllocatableValue __x, AllocatableValue __y)
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
        public static final LIRInstructionClass<CommutativeThreeOp> TYPE = LIRInstructionClass.create(CommutativeThreeOp.class);

        @Opcode
        // @field
        private final AMD64RRMOp ___opcode;
        // @field
        private final OperandSize ___size;

        @Def({OperandFlag.REG})
        // @field
        protected AllocatableValue ___result;
        @Use({OperandFlag.REG})
        // @field
        protected AllocatableValue ___x;
        @Use({OperandFlag.REG, OperandFlag.STACK})
        // @field
        protected AllocatableValue ___y;

        // @cons
        public CommutativeThreeOp(AMD64RRMOp __opcode, OperandSize __size, AllocatableValue __result, AllocatableValue __x, AllocatableValue __y)
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
        public static final LIRInstructionClass<ConstOp> TYPE = LIRInstructionClass.create(ConstOp.class);

        @Opcode
        // @field
        private final AMD64MIOp ___opcode;
        // @field
        private final OperandSize ___size;

        @Def({OperandFlag.REG, OperandFlag.HINT})
        // @field
        protected AllocatableValue ___result;
        @Use({OperandFlag.REG})
        // @field
        protected AllocatableValue ___x;
        // @field
        private final int ___y;

        // @cons
        public ConstOp(AMD64BinaryArithmetic __opcode, OperandSize __size, AllocatableValue __result, AllocatableValue __x, int __y)
        {
            this(__opcode.getMIOpcode(__size, NumUtil.isByte(__y)), __size, __result, __x, __y);
        }

        // @cons
        public ConstOp(AMD64MIOp __opcode, OperandSize __size, AllocatableValue __result, AllocatableValue __x, int __y)
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
        public static final LIRInstructionClass<DataTwoOp> TYPE = LIRInstructionClass.create(DataTwoOp.class);

        @Opcode
        // @field
        private final AMD64RMOp ___opcode;
        // @field
        private final OperandSize ___size;

        @Def({OperandFlag.REG, OperandFlag.HINT})
        // @field
        protected AllocatableValue ___result;
        @Use({OperandFlag.REG})
        // @field
        protected AllocatableValue ___x;
        // @field
        private final JavaConstant ___y;

        // @field
        private final int ___alignment;

        // @cons
        public DataTwoOp(AMD64RMOp __opcode, OperandSize __size, AllocatableValue __result, AllocatableValue __x, JavaConstant __y)
        {
            this(__opcode, __size, __result, __x, __y, __y.getJavaKind().getByteCount());
        }

        // @cons
        public DataTwoOp(AMD64RMOp __opcode, OperandSize __size, AllocatableValue __result, AllocatableValue __x, JavaConstant __y, int __alignment)
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
        public static final LIRInstructionClass<DataThreeOp> TYPE = LIRInstructionClass.create(DataThreeOp.class);

        @Opcode
        // @field
        private final AMD64RRMOp ___opcode;
        // @field
        private final OperandSize ___size;

        @Def({OperandFlag.REG})
        // @field
        protected AllocatableValue ___result;
        @Use({OperandFlag.REG})
        // @field
        protected AllocatableValue ___x;
        // @field
        private final JavaConstant ___y;

        // @field
        private final int ___alignment;

        // @cons
        public DataThreeOp(AMD64RRMOp __opcode, OperandSize __size, AllocatableValue __result, AllocatableValue __x, JavaConstant __y)
        {
            this(__opcode, __size, __result, __x, __y, __y.getJavaKind().getByteCount());
        }

        // @cons
        public DataThreeOp(AMD64RRMOp __opcode, OperandSize __size, AllocatableValue __result, AllocatableValue __x, JavaConstant __y, int __alignment)
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
    public static final class MemoryTwoOp extends AMD64LIRInstruction implements ImplicitNullCheck
    {
        // @def
        public static final LIRInstructionClass<MemoryTwoOp> TYPE = LIRInstructionClass.create(MemoryTwoOp.class);

        @Opcode
        // @field
        private final AMD64RMOp ___opcode;
        // @field
        private final OperandSize ___size;

        @Def({OperandFlag.REG, OperandFlag.HINT})
        // @field
        protected AllocatableValue ___result;
        @Use({OperandFlag.REG})
        // @field
        protected AllocatableValue ___x;
        @Alive({OperandFlag.COMPOSITE})
        // @field
        protected AMD64AddressValue ___y;

        // @State
        // @field
        protected LIRFrameState ___state;

        // @cons
        public MemoryTwoOp(AMD64RMOp __opcode, OperandSize __size, AllocatableValue __result, AllocatableValue __x, AMD64AddressValue __y, LIRFrameState __state)
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
    public static final class MemoryThreeOp extends AMD64LIRInstruction implements ImplicitNullCheck
    {
        // @def
        public static final LIRInstructionClass<MemoryThreeOp> TYPE = LIRInstructionClass.create(MemoryThreeOp.class);

        @Opcode
        // @field
        private final AMD64RRMOp ___opcode;
        // @field
        private final OperandSize ___size;

        @Def({OperandFlag.REG})
        // @field
        protected AllocatableValue ___result;
        @Use({OperandFlag.REG})
        // @field
        protected AllocatableValue ___x;
        @Use({OperandFlag.COMPOSITE})
        // @field
        protected AMD64AddressValue ___y;

        // @State
        // @field
        protected LIRFrameState ___state;

        // @cons
        public MemoryThreeOp(AMD64RRMOp __opcode, OperandSize __size, AllocatableValue __result, AllocatableValue __x, AMD64AddressValue __y, LIRFrameState __state)
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
        public static final LIRInstructionClass<RMIOp> TYPE = LIRInstructionClass.create(RMIOp.class);

        @Opcode
        // @field
        private final AMD64RMIOp ___opcode;
        // @field
        private final OperandSize ___size;

        @Def({OperandFlag.REG})
        // @field
        protected AllocatableValue ___result;
        @Use({OperandFlag.REG, OperandFlag.STACK})
        // @field
        protected AllocatableValue ___x;
        // @field
        private final int ___y;

        // @cons
        public RMIOp(AMD64RMIOp __opcode, OperandSize __size, AllocatableValue __result, AllocatableValue __x, int __y)
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
