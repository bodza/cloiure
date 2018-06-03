package giraaff.lir.amd64;

import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Value;

import giraaff.asm.amd64.AMD64Address;
import giraaff.asm.amd64.AMD64Assembler.AMD64MOp;
import giraaff.asm.amd64.AMD64Assembler.AMD64MROp;
import giraaff.asm.amd64.AMD64Assembler.AMD64RMOp;
import giraaff.asm.amd64.AMD64Assembler.OperandSize;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.lir.LIRFrameState;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.Opcode;
import giraaff.lir.StandardOp.ImplicitNullCheck;
import giraaff.lir.asm.CompilationResultBuilder;

///
// AMD64 LIR instructions that have one input and one output.
///
// @class AMD64Unary
public final class AMD64Unary
{
    ///
    // Instruction with a single operand that is both input and output.
    ///
    // @class AMD64Unary.MOp
    public static final class MOp extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<MOp> TYPE = LIRInstructionClass.create(MOp.class);

        @Opcode
        // @field
        private final AMD64MOp ___opcode;
        // @field
        private final OperandSize ___size;

        @Def({OperandFlag.REG, OperandFlag.HINT})
        // @field
        protected AllocatableValue ___result;
        @Use({OperandFlag.REG, OperandFlag.STACK})
        // @field
        protected AllocatableValue ___value;

        // @cons
        public MOp(AMD64MOp __opcode, OperandSize __size, AllocatableValue __result, AllocatableValue __value)
        {
            super(TYPE);
            this.___opcode = __opcode;
            this.___size = __size;

            this.___result = __result;
            this.___value = __value;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            AMD64Move.move(__crb, __masm, this.___result, this.___value);
            this.___opcode.emit(__masm, this.___size, ValueUtil.asRegister(this.___result));
        }
    }

    ///
    // Instruction with separate input and output operands, and an operand encoding of RM.
    ///
    // @class AMD64Unary.RMOp
    public static final class RMOp extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<RMOp> TYPE = LIRInstructionClass.create(RMOp.class);

        @Opcode
        // @field
        private final AMD64RMOp ___opcode;
        // @field
        private final OperandSize ___size;

        @Def({OperandFlag.REG})
        // @field
        protected AllocatableValue ___result;
        @Use({OperandFlag.REG, OperandFlag.STACK})
        // @field
        protected AllocatableValue ___value;

        // @cons
        public RMOp(AMD64RMOp __opcode, OperandSize __size, AllocatableValue __result, AllocatableValue __value)
        {
            super(TYPE);
            this.___opcode = __opcode;
            this.___size = __size;

            this.___result = __result;
            this.___value = __value;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            if (ValueUtil.isRegister(this.___value))
            {
                this.___opcode.emit(__masm, this.___size, ValueUtil.asRegister(this.___result), ValueUtil.asRegister(this.___value));
            }
            else
            {
                this.___opcode.emit(__masm, this.___size, ValueUtil.asRegister(this.___result), (AMD64Address) __crb.asAddress(this.___value));
            }
        }
    }

    ///
    // Instruction with separate input and output operands, and an operand encoding of MR.
    ///
    // @class AMD64Unary.MROp
    public static final class MROp extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<MROp> TYPE = LIRInstructionClass.create(MROp.class);

        @Opcode
        // @field
        private final AMD64MROp ___opcode;
        // @field
        private final OperandSize ___size;

        @Def({OperandFlag.REG, OperandFlag.STACK})
        // @field
        protected AllocatableValue ___result;
        @Use({OperandFlag.REG})
        // @field
        protected AllocatableValue ___value;

        // @cons
        public MROp(AMD64MROp __opcode, OperandSize __size, AllocatableValue __result, AllocatableValue __value)
        {
            super(TYPE);
            this.___opcode = __opcode;
            this.___size = __size;

            this.___result = __result;
            this.___value = __value;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            if (ValueUtil.isRegister(this.___result))
            {
                this.___opcode.emit(__masm, this.___size, ValueUtil.asRegister(this.___result), ValueUtil.asRegister(this.___value));
            }
            else
            {
                this.___opcode.emit(__masm, this.___size, (AMD64Address) __crb.asAddress(this.___result), ValueUtil.asRegister(this.___value));
            }
        }
    }

    ///
    // Instruction with a {@link AMD64AddressValue memory} operand.
    ///
    // @class AMD64Unary.MemoryOp
    public static final class MemoryOp extends AMD64LIRInstruction implements ImplicitNullCheck
    {
        // @def
        public static final LIRInstructionClass<MemoryOp> TYPE = LIRInstructionClass.create(MemoryOp.class);

        @Opcode
        // @field
        private final AMD64RMOp ___opcode;
        // @field
        private final OperandSize ___size;

        @Def({OperandFlag.REG})
        // @field
        protected AllocatableValue ___result;
        @Use({OperandFlag.COMPOSITE})
        // @field
        protected AMD64AddressValue ___input;

        // @State
        // @field
        protected LIRFrameState ___state;

        // @cons
        public MemoryOp(AMD64RMOp __opcode, OperandSize __size, AllocatableValue __result, AMD64AddressValue __input, LIRFrameState __state)
        {
            super(TYPE);
            this.___opcode = __opcode;
            this.___size = __size;

            this.___result = __result;
            this.___input = __input;

            this.___state = __state;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            this.___opcode.emit(__masm, this.___size, ValueUtil.asRegister(this.___result), this.___input.toAddress());
        }

        @Override
        public boolean makeNullCheckFor(Value __value, LIRFrameState __nullCheckState, int __implicitNullCheckLimit)
        {
            if (this.___state == null && this.___input.isValidImplicitNullCheckFor(__value, __implicitNullCheckLimit))
            {
                this.___state = __nullCheckState;
                return true;
            }
            return false;
        }
    }
}
