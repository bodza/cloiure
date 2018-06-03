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

/**
 * AMD64 LIR instructions that have one input and one output.
 */
// @class AMD64Unary
public final class AMD64Unary
{
    /**
     * Instruction with a single operand that is both input and output.
     */
    // @class AMD64Unary.MOp
    public static final class MOp extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<MOp> TYPE = LIRInstructionClass.create(MOp.class);

        @Opcode
        // @field
        private final AMD64MOp opcode;
        // @field
        private final OperandSize size;

        @Def({OperandFlag.REG, OperandFlag.HINT})
        // @field
        protected AllocatableValue result;
        @Use({OperandFlag.REG, OperandFlag.STACK})
        // @field
        protected AllocatableValue value;

        // @cons
        public MOp(AMD64MOp __opcode, OperandSize __size, AllocatableValue __result, AllocatableValue __value)
        {
            super(TYPE);
            this.opcode = __opcode;
            this.size = __size;

            this.result = __result;
            this.value = __value;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            AMD64Move.move(__crb, __masm, result, value);
            opcode.emit(__masm, size, ValueUtil.asRegister(result));
        }
    }

    /**
     * Instruction with separate input and output operands, and an operand encoding of RM.
     */
    // @class AMD64Unary.RMOp
    public static final class RMOp extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<RMOp> TYPE = LIRInstructionClass.create(RMOp.class);

        @Opcode
        // @field
        private final AMD64RMOp opcode;
        // @field
        private final OperandSize size;

        @Def({OperandFlag.REG})
        // @field
        protected AllocatableValue result;
        @Use({OperandFlag.REG, OperandFlag.STACK})
        // @field
        protected AllocatableValue value;

        // @cons
        public RMOp(AMD64RMOp __opcode, OperandSize __size, AllocatableValue __result, AllocatableValue __value)
        {
            super(TYPE);
            this.opcode = __opcode;
            this.size = __size;

            this.result = __result;
            this.value = __value;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            if (ValueUtil.isRegister(value))
            {
                opcode.emit(__masm, size, ValueUtil.asRegister(result), ValueUtil.asRegister(value));
            }
            else
            {
                opcode.emit(__masm, size, ValueUtil.asRegister(result), (AMD64Address) __crb.asAddress(value));
            }
        }
    }

    /**
     * Instruction with separate input and output operands, and an operand encoding of MR.
     */
    // @class AMD64Unary.MROp
    public static final class MROp extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<MROp> TYPE = LIRInstructionClass.create(MROp.class);

        @Opcode
        // @field
        private final AMD64MROp opcode;
        // @field
        private final OperandSize size;

        @Def({OperandFlag.REG, OperandFlag.STACK})
        // @field
        protected AllocatableValue result;
        @Use({OperandFlag.REG})
        // @field
        protected AllocatableValue value;

        // @cons
        public MROp(AMD64MROp __opcode, OperandSize __size, AllocatableValue __result, AllocatableValue __value)
        {
            super(TYPE);
            this.opcode = __opcode;
            this.size = __size;

            this.result = __result;
            this.value = __value;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            if (ValueUtil.isRegister(result))
            {
                opcode.emit(__masm, size, ValueUtil.asRegister(result), ValueUtil.asRegister(value));
            }
            else
            {
                opcode.emit(__masm, size, (AMD64Address) __crb.asAddress(result), ValueUtil.asRegister(value));
            }
        }
    }

    /**
     * Instruction with a {@link AMD64AddressValue memory} operand.
     */
    // @class AMD64Unary.MemoryOp
    public static final class MemoryOp extends AMD64LIRInstruction implements ImplicitNullCheck
    {
        // @def
        public static final LIRInstructionClass<MemoryOp> TYPE = LIRInstructionClass.create(MemoryOp.class);

        @Opcode
        // @field
        private final AMD64RMOp opcode;
        // @field
        private final OperandSize size;

        @Def({OperandFlag.REG})
        // @field
        protected AllocatableValue result;
        @Use({OperandFlag.COMPOSITE})
        // @field
        protected AMD64AddressValue input;

        // @State
        // @field
        protected LIRFrameState state;

        // @cons
        public MemoryOp(AMD64RMOp __opcode, OperandSize __size, AllocatableValue __result, AMD64AddressValue __input, LIRFrameState __state)
        {
            super(TYPE);
            this.opcode = __opcode;
            this.size = __size;

            this.result = __result;
            this.input = __input;

            this.state = __state;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            opcode.emit(__masm, size, ValueUtil.asRegister(result), input.toAddress());
        }

        @Override
        public boolean makeNullCheckFor(Value __value, LIRFrameState __nullCheckState, int __implicitNullCheckLimit)
        {
            if (state == null && input.isValidImplicitNullCheckFor(__value, __implicitNullCheckLimit))
            {
                state = __nullCheckState;
                return true;
            }
            return false;
        }
    }
}
