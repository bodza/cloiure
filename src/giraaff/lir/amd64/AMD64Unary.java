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
public class AMD64Unary
{
    /**
     * Instruction with a single operand that is both input and output.
     */
    public static class MOp extends AMD64LIRInstruction
    {
        public static final LIRInstructionClass<MOp> TYPE = LIRInstructionClass.create(MOp.class);

        @Opcode private final AMD64MOp opcode;
        private final OperandSize size;

        @Def({OperandFlag.REG, OperandFlag.HINT}) protected AllocatableValue result;
        @Use({OperandFlag.REG, OperandFlag.STACK}) protected AllocatableValue value;

        public MOp(AMD64MOp opcode, OperandSize size, AllocatableValue result, AllocatableValue value)
        {
            super(TYPE);
            this.opcode = opcode;
            this.size = size;

            this.result = result;
            this.value = value;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            AMD64Move.move(crb, masm, result, value);
            opcode.emit(masm, size, ValueUtil.asRegister(result));
        }
    }

    /**
     * Instruction with separate input and output operands, and an operand encoding of RM.
     */
    public static class RMOp extends AMD64LIRInstruction
    {
        public static final LIRInstructionClass<RMOp> TYPE = LIRInstructionClass.create(RMOp.class);

        @Opcode private final AMD64RMOp opcode;
        private final OperandSize size;

        @Def({OperandFlag.REG}) protected AllocatableValue result;
        @Use({OperandFlag.REG, OperandFlag.STACK}) protected AllocatableValue value;

        public RMOp(AMD64RMOp opcode, OperandSize size, AllocatableValue result, AllocatableValue value)
        {
            super(TYPE);
            this.opcode = opcode;
            this.size = size;

            this.result = result;
            this.value = value;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            if (ValueUtil.isRegister(value))
            {
                opcode.emit(masm, size, ValueUtil.asRegister(result), ValueUtil.asRegister(value));
            }
            else
            {
                opcode.emit(masm, size, ValueUtil.asRegister(result), (AMD64Address) crb.asAddress(value));
            }
        }
    }

    /**
     * Instruction with separate input and output operands, and an operand encoding of MR.
     */
    public static class MROp extends AMD64LIRInstruction
    {
        public static final LIRInstructionClass<MROp> TYPE = LIRInstructionClass.create(MROp.class);

        @Opcode private final AMD64MROp opcode;
        private final OperandSize size;

        @Def({OperandFlag.REG, OperandFlag.STACK}) protected AllocatableValue result;
        @Use({OperandFlag.REG}) protected AllocatableValue value;

        public MROp(AMD64MROp opcode, OperandSize size, AllocatableValue result, AllocatableValue value)
        {
            super(TYPE);
            this.opcode = opcode;
            this.size = size;

            this.result = result;
            this.value = value;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            if (ValueUtil.isRegister(result))
            {
                opcode.emit(masm, size, ValueUtil.asRegister(result), ValueUtil.asRegister(value));
            }
            else
            {
                opcode.emit(masm, size, (AMD64Address) crb.asAddress(result), ValueUtil.asRegister(value));
            }
        }
    }

    /**
     * Instruction with a {@link AMD64AddressValue memory} operand.
     */
    public static class MemoryOp extends AMD64LIRInstruction implements ImplicitNullCheck
    {
        public static final LIRInstructionClass<MemoryOp> TYPE = LIRInstructionClass.create(MemoryOp.class);

        @Opcode private final AMD64RMOp opcode;
        private final OperandSize size;

        @Def({OperandFlag.REG}) protected AllocatableValue result;
        @Use({OperandFlag.COMPOSITE}) protected AMD64AddressValue input;

        @State protected LIRFrameState state;

        public MemoryOp(AMD64RMOp opcode, OperandSize size, AllocatableValue result, AMD64AddressValue input, LIRFrameState state)
        {
            super(TYPE);
            this.opcode = opcode;
            this.size = size;

            this.result = result;
            this.input = input;

            this.state = state;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            if (state != null)
            {
                crb.recordImplicitException(masm.position(), state);
            }
            opcode.emit(masm, size, ValueUtil.asRegister(result), input.toAddress());
        }

        @Override
        public boolean makeNullCheckFor(Value value, LIRFrameState nullCheckState, int implicitNullCheckLimit)
        {
            if (state == null && input.isValidImplicitNullCheckFor(value, implicitNullCheckLimit))
            {
                state = nullCheckState;
                return true;
            }
            return false;
        }
    }
}
