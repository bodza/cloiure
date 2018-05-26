package giraaff.lir.amd64;

import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.Value;

import giraaff.asm.Label;
import giraaff.asm.amd64.AMD64Address;
import giraaff.asm.amd64.AMD64Assembler.AMD64MIOp;
import giraaff.asm.amd64.AMD64Assembler.AMD64MOp;
import giraaff.asm.amd64.AMD64Assembler.ConditionFlag;
import giraaff.asm.amd64.AMD64Assembler.OperandSize;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.core.common.CompressEncoding;
import giraaff.core.common.GraalOptions;
import giraaff.core.common.LIRKind;
import giraaff.core.common.spi.LIRKindTool;
import giraaff.core.common.type.DataPointerConstant;
import giraaff.lir.LIRFrameState;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.LIRValueUtil;
import giraaff.lir.Opcode;
import giraaff.lir.StandardOp.LoadConstantOp;
import giraaff.lir.StandardOp.NullCheck;
import giraaff.lir.StandardOp.ValueMoveOp;
import giraaff.lir.asm.CompilationResultBuilder;
import giraaff.options.OptionValues;
import giraaff.util.GraalError;

public class AMD64Move
{
    private abstract static class AbstractMoveOp extends AMD64LIRInstruction implements ValueMoveOp
    {
        public static final LIRInstructionClass<AbstractMoveOp> TYPE = LIRInstructionClass.create(AbstractMoveOp.class);

        private AMD64Kind moveKind;

        protected AbstractMoveOp(LIRInstructionClass<? extends AbstractMoveOp> c, AMD64Kind moveKind)
        {
            super(c);
            this.moveKind = moveKind;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            move(moveKind, crb, masm, getResult(), getInput());
        }
    }

    @Opcode("MOVE")
    public static final class MoveToRegOp extends AbstractMoveOp
    {
        public static final LIRInstructionClass<MoveToRegOp> TYPE = LIRInstructionClass.create(MoveToRegOp.class);

        @Def({OperandFlag.REG, OperandFlag.HINT}) protected AllocatableValue result;
        @Use({OperandFlag.REG, OperandFlag.STACK}) protected AllocatableValue input;

        public MoveToRegOp(AMD64Kind moveKind, AllocatableValue result, AllocatableValue input)
        {
            super(TYPE, moveKind);
            this.result = result;
            this.input = input;
        }

        @Override
        public AllocatableValue getInput()
        {
            return input;
        }

        @Override
        public AllocatableValue getResult()
        {
            return result;
        }
    }

    @Opcode("MOVE")
    public static final class MoveFromRegOp extends AbstractMoveOp
    {
        public static final LIRInstructionClass<MoveFromRegOp> TYPE = LIRInstructionClass.create(MoveFromRegOp.class);

        @Def({OperandFlag.REG, OperandFlag.STACK}) protected AllocatableValue result;
        @Use({OperandFlag.REG, OperandFlag.HINT}) protected AllocatableValue input;

        public MoveFromRegOp(AMD64Kind moveKind, AllocatableValue result, AllocatableValue input)
        {
            super(TYPE, moveKind);
            this.result = result;
            this.input = input;
        }

        @Override
        public AllocatableValue getInput()
        {
            return input;
        }

        @Override
        public AllocatableValue getResult()
        {
            return result;
        }
    }

    @Opcode("MOVE")
    public static class MoveFromConstOp extends AMD64LIRInstruction implements LoadConstantOp
    {
        public static final LIRInstructionClass<MoveFromConstOp> TYPE = LIRInstructionClass.create(MoveFromConstOp.class);

        @Def({OperandFlag.REG, OperandFlag.STACK}) protected AllocatableValue result;
        private final JavaConstant input;

        public MoveFromConstOp(AllocatableValue result, JavaConstant input)
        {
            super(TYPE);
            this.result = result;
            this.input = input;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            if (ValueUtil.isRegister(result))
            {
                const2reg(crb, masm, ValueUtil.asRegister(result), input);
            }
            else
            {
                const2stack(crb, masm, result, input);
            }
        }

        @Override
        public Constant getConstant()
        {
            return input;
        }

        @Override
        public AllocatableValue getResult()
        {
            return result;
        }
    }

    @Opcode("STACKMOVE")
    public static final class AMD64StackMove extends AMD64LIRInstruction implements ValueMoveOp
    {
        public static final LIRInstructionClass<AMD64StackMove> TYPE = LIRInstructionClass.create(AMD64StackMove.class);

        @Def({OperandFlag.STACK}) protected AllocatableValue result;
        @Use({OperandFlag.STACK, OperandFlag.HINT}) protected AllocatableValue input;
        @Alive({OperandFlag.STACK, OperandFlag.UNINITIALIZED}) private AllocatableValue backupSlot;

        private Register scratch;

        public AMD64StackMove(AllocatableValue result, AllocatableValue input, Register scratch, AllocatableValue backupSlot)
        {
            super(TYPE);
            this.result = result;
            this.input = input;
            this.backupSlot = backupSlot;
            this.scratch = scratch;
        }

        @Override
        public AllocatableValue getInput()
        {
            return input;
        }

        @Override
        public AllocatableValue getResult()
        {
            return result;
        }

        public Register getScratchRegister()
        {
            return scratch;
        }

        public AllocatableValue getBackupSlot()
        {
            return backupSlot;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            AMD64Kind backupKind = (AMD64Kind) backupSlot.getPlatformKind();
            if (backupKind.isXMM())
            {
                // graal doesn't use vector values, so it's safe to backup using DOUBLE
                backupKind = AMD64Kind.DOUBLE;
            }

            // backup scratch register
            reg2stack(backupKind, crb, masm, backupSlot, scratch);
            // move stack slot
            stack2reg((AMD64Kind) getInput().getPlatformKind(), crb, masm, scratch, getInput());
            reg2stack((AMD64Kind) getResult().getPlatformKind(), crb, masm, getResult(), scratch);
            // restore scratch register
            stack2reg(backupKind, crb, masm, scratch, backupSlot);
        }
    }

    @Opcode("MULTISTACKMOVE")
    public static final class AMD64MultiStackMove extends AMD64LIRInstruction
    {
        public static final LIRInstructionClass<AMD64MultiStackMove> TYPE = LIRInstructionClass.create(AMD64MultiStackMove.class);

        @Def({OperandFlag.STACK}) protected AllocatableValue[] results;
        @Use({OperandFlag.STACK}) protected Value[] inputs;
        @Alive({OperandFlag.STACK, OperandFlag.UNINITIALIZED}) private AllocatableValue backupSlot;

        private Register scratch;

        public AMD64MultiStackMove(AllocatableValue[] results, Value[] inputs, Register scratch, AllocatableValue backupSlot)
        {
            super(TYPE);
            this.results = results;
            this.inputs = inputs;
            this.backupSlot = backupSlot;
            this.scratch = scratch;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            AMD64Kind backupKind = (AMD64Kind) backupSlot.getPlatformKind();
            if (backupKind.isXMM())
            {
                // graal doesn't use vector values, so it's safe to backup using DOUBLE
                backupKind = AMD64Kind.DOUBLE;
            }

            // backup scratch register
            move(backupKind, crb, masm, backupSlot, scratch.asValue(backupSlot.getValueKind()));
            for (int i = 0; i < results.length; i++)
            {
                Value input = inputs[i];
                AllocatableValue result = results[i];
                // move stack slot
                move((AMD64Kind) input.getPlatformKind(), crb, masm, scratch.asValue(input.getValueKind()), input);
                move((AMD64Kind) result.getPlatformKind(), crb, masm, result, scratch.asValue(result.getValueKind()));
            }
            // restore scratch register
            move(backupKind, crb, masm, scratch.asValue(backupSlot.getValueKind()), backupSlot);
        }
    }

    @Opcode("STACKMOVE")
    public static final class AMD64PushPopStackMove extends AMD64LIRInstruction implements ValueMoveOp
    {
        public static final LIRInstructionClass<AMD64PushPopStackMove> TYPE = LIRInstructionClass.create(AMD64PushPopStackMove.class);

        @Def({OperandFlag.STACK}) protected AllocatableValue result;
        @Use({OperandFlag.STACK, OperandFlag.HINT}) protected AllocatableValue input;
        private final OperandSize size;

        public AMD64PushPopStackMove(OperandSize size, AllocatableValue result, AllocatableValue input)
        {
            super(TYPE);
            this.result = result;
            this.input = input;
            this.size = size;
        }

        @Override
        public AllocatableValue getInput()
        {
            return input;
        }

        @Override
        public AllocatableValue getResult()
        {
            return result;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            AMD64MOp.PUSH.emit(masm, size, (AMD64Address) crb.asAddress(input));
            AMD64MOp.POP.emit(masm, size, (AMD64Address) crb.asAddress(result));
        }
    }

    public static final class LeaOp extends AMD64LIRInstruction
    {
        public static final LIRInstructionClass<LeaOp> TYPE = LIRInstructionClass.create(LeaOp.class);

        @Def({OperandFlag.REG}) protected AllocatableValue result;
        @Use({OperandFlag.COMPOSITE, OperandFlag.UNINITIALIZED}) protected AMD64AddressValue address;
        private final OperandSize size;

        public LeaOp(AllocatableValue result, AMD64AddressValue address, OperandSize size)
        {
            super(TYPE);
            this.result = result;
            this.address = address;
            this.size = size;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            if (size == OperandSize.QWORD)
            {
                masm.leaq(ValueUtil.asRegister(result, AMD64Kind.QWORD), address.toAddress());
            }
            else
            {
                masm.lead(ValueUtil.asRegister(result, AMD64Kind.DWORD), address.toAddress());
            }
        }
    }

    public static final class LeaDataOp extends AMD64LIRInstruction
    {
        public static final LIRInstructionClass<LeaDataOp> TYPE = LIRInstructionClass.create(LeaDataOp.class);

        @Def({OperandFlag.REG}) protected AllocatableValue result;
        private final DataPointerConstant data;

        public LeaDataOp(AllocatableValue result, DataPointerConstant data)
        {
            super(TYPE);
            this.result = result;
            this.data = data;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            masm.leaq(ValueUtil.asRegister(result), (AMD64Address) crb.recordDataReferenceInCode(data));
        }
    }

    public static final class StackLeaOp extends AMD64LIRInstruction
    {
        public static final LIRInstructionClass<StackLeaOp> TYPE = LIRInstructionClass.create(StackLeaOp.class);

        @Def({OperandFlag.REG}) protected AllocatableValue result;
        @Use({OperandFlag.STACK, OperandFlag.UNINITIALIZED}) protected AllocatableValue slot;

        public StackLeaOp(AllocatableValue result, AllocatableValue slot)
        {
            super(TYPE);
            this.result = result;
            this.slot = slot;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            masm.leaq(ValueUtil.asRegister(result, AMD64Kind.QWORD), (AMD64Address) crb.asAddress(slot));
        }
    }

    public static final class MembarOp extends AMD64LIRInstruction
    {
        public static final LIRInstructionClass<MembarOp> TYPE = LIRInstructionClass.create(MembarOp.class);

        private final int barriers;

        public MembarOp(final int barriers)
        {
            super(TYPE);
            this.barriers = barriers;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            masm.membar(barriers);
        }
    }

    public static final class NullCheckOp extends AMD64LIRInstruction implements NullCheck
    {
        public static final LIRInstructionClass<NullCheckOp> TYPE = LIRInstructionClass.create(NullCheckOp.class);

        @Use({OperandFlag.COMPOSITE}) protected AMD64AddressValue address;
        // @State
        protected LIRFrameState state;

        public NullCheckOp(AMD64AddressValue address, LIRFrameState state)
        {
            super(TYPE);
            this.address = address;
            this.state = state;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            masm.nullCheck(address.toAddress());
        }

        @Override
        public Value getCheckedValue()
        {
            return address.base;
        }

        @Override
        public LIRFrameState getState()
        {
            return state;
        }
    }

    @Opcode("CAS")
    public static final class CompareAndSwapOp extends AMD64LIRInstruction
    {
        public static final LIRInstructionClass<CompareAndSwapOp> TYPE = LIRInstructionClass.create(CompareAndSwapOp.class);

        private final AMD64Kind accessKind;

        @Def protected AllocatableValue result;
        @Use({OperandFlag.COMPOSITE}) protected AMD64AddressValue address;
        @Use protected AllocatableValue cmpValue;
        @Use protected AllocatableValue newValue;

        public CompareAndSwapOp(AMD64Kind accessKind, AllocatableValue result, AMD64AddressValue address, AllocatableValue cmpValue, AllocatableValue newValue)
        {
            super(TYPE);
            this.accessKind = accessKind;
            this.result = result;
            this.address = address;
            this.cmpValue = cmpValue;
            this.newValue = newValue;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            if (crb.target.isMP)
            {
                masm.lock();
            }
            switch (accessKind)
            {
                case DWORD:
                    masm.cmpxchgl(ValueUtil.asRegister(newValue), address.toAddress());
                    break;
                case QWORD:
                    masm.cmpxchgq(ValueUtil.asRegister(newValue), address.toAddress());
                    break;
                default:
                    throw GraalError.shouldNotReachHere();
            }
        }
    }

    @Opcode("ATOMIC_READ_AND_ADD")
    public static final class AtomicReadAndAddOp extends AMD64LIRInstruction
    {
        public static final LIRInstructionClass<AtomicReadAndAddOp> TYPE = LIRInstructionClass.create(AtomicReadAndAddOp.class);

        private final AMD64Kind accessKind;

        @Def protected AllocatableValue result;
        @Alive({OperandFlag.COMPOSITE}) protected AMD64AddressValue address;
        @Use protected AllocatableValue delta;

        public AtomicReadAndAddOp(AMD64Kind accessKind, AllocatableValue result, AMD64AddressValue address, AllocatableValue delta)
        {
            super(TYPE);
            this.accessKind = accessKind;
            this.result = result;
            this.address = address;
            this.delta = delta;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            move(accessKind, crb, masm, result, delta);
            if (crb.target.isMP)
            {
                masm.lock();
            }
            switch (accessKind)
            {
                case DWORD:
                    masm.xaddl(address.toAddress(), ValueUtil.asRegister(result));
                    break;
                case QWORD:
                    masm.xaddq(address.toAddress(), ValueUtil.asRegister(result));
                    break;
                default:
                    throw GraalError.shouldNotReachHere();
            }
        }
    }

    @Opcode("ATOMIC_READ_AND_WRITE")
    public static final class AtomicReadAndWriteOp extends AMD64LIRInstruction
    {
        public static final LIRInstructionClass<AtomicReadAndWriteOp> TYPE = LIRInstructionClass.create(AtomicReadAndWriteOp.class);

        private final AMD64Kind accessKind;

        @Def protected AllocatableValue result;
        @Alive({OperandFlag.COMPOSITE}) protected AMD64AddressValue address;
        @Use protected AllocatableValue newValue;

        public AtomicReadAndWriteOp(AMD64Kind accessKind, AllocatableValue result, AMD64AddressValue address, AllocatableValue newValue)
        {
            super(TYPE);
            this.accessKind = accessKind;
            this.result = result;
            this.address = address;
            this.newValue = newValue;
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            move(accessKind, crb, masm, result, newValue);
            switch (accessKind)
            {
                case DWORD:
                    masm.xchgl(ValueUtil.asRegister(result), address.toAddress());
                    break;
                case QWORD:
                    masm.xchgq(ValueUtil.asRegister(result), address.toAddress());
                    break;
                default:
                    throw GraalError.shouldNotReachHere();
            }
        }
    }

    public static void move(CompilationResultBuilder crb, AMD64MacroAssembler masm, Value result, Value input)
    {
        move((AMD64Kind) result.getPlatformKind(), crb, masm, result, input);
    }

    public static void move(AMD64Kind moveKind, CompilationResultBuilder crb, AMD64MacroAssembler masm, Value result, Value input)
    {
        if (ValueUtil.isRegister(input))
        {
            if (ValueUtil.isRegister(result))
            {
                reg2reg(moveKind, masm, result, input);
            }
            else if (ValueUtil.isStackSlot(result))
            {
                reg2stack(moveKind, crb, masm, result, ValueUtil.asRegister(input));
            }
            else
            {
                throw GraalError.shouldNotReachHere();
            }
        }
        else if (ValueUtil.isStackSlot(input))
        {
            if (ValueUtil.isRegister(result))
            {
                stack2reg(moveKind, crb, masm, ValueUtil.asRegister(result), input);
            }
            else
            {
                throw GraalError.shouldNotReachHere();
            }
        }
        else if (LIRValueUtil.isJavaConstant(input))
        {
            if (ValueUtil.isRegister(result))
            {
                const2reg(crb, masm, ValueUtil.asRegister(result), LIRValueUtil.asJavaConstant(input));
            }
            else if (ValueUtil.isStackSlot(result))
            {
                const2stack(crb, masm, result, LIRValueUtil.asJavaConstant(input));
            }
            else
            {
                throw GraalError.shouldNotReachHere();
            }
        }
        else
        {
            throw GraalError.shouldNotReachHere();
        }
    }

    private static void reg2reg(AMD64Kind kind, AMD64MacroAssembler masm, Value result, Value input)
    {
        if (ValueUtil.asRegister(input).equals(ValueUtil.asRegister(result)))
        {
            return;
        }
        switch (kind)
        {
            case BYTE:
            case WORD:
            case DWORD:
                masm.movl(ValueUtil.asRegister(result), ValueUtil.asRegister(input));
                break;
            case QWORD:
                masm.movq(ValueUtil.asRegister(result), ValueUtil.asRegister(input));
                break;
            case SINGLE:
                masm.movflt(ValueUtil.asRegister(result, AMD64Kind.SINGLE), ValueUtil.asRegister(input, AMD64Kind.SINGLE));
                break;
            case DOUBLE:
                masm.movdbl(ValueUtil.asRegister(result, AMD64Kind.DOUBLE), ValueUtil.asRegister(input, AMD64Kind.DOUBLE));
                break;
            default:
                throw GraalError.shouldNotReachHere("kind=" + kind);
        }
    }

    public static void reg2stack(AMD64Kind kind, CompilationResultBuilder crb, AMD64MacroAssembler masm, Value result, Register input)
    {
        AMD64Address dest = (AMD64Address) crb.asAddress(result);
        switch (kind)
        {
            case BYTE:
                masm.movb(dest, input);
                break;
            case WORD:
                masm.movw(dest, input);
                break;
            case DWORD:
                masm.movl(dest, input);
                break;
            case QWORD:
                masm.movq(dest, input);
                break;
            case SINGLE:
                masm.movflt(dest, input);
                break;
            case DOUBLE:
                masm.movsd(dest, input);
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    public static void stack2reg(AMD64Kind kind, CompilationResultBuilder crb, AMD64MacroAssembler masm, Register result, Value input)
    {
        AMD64Address src = (AMD64Address) crb.asAddress(input);
        switch (kind)
        {
            case BYTE:
                masm.movsbl(result, src);
                break;
            case WORD:
                masm.movswl(result, src);
                break;
            case DWORD:
                masm.movl(result, src);
                break;
            case QWORD:
                masm.movq(result, src);
                break;
            case SINGLE:
                masm.movflt(result, src);
                break;
            case DOUBLE:
                masm.movdbl(result, src);
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    public static void const2reg(CompilationResultBuilder crb, AMD64MacroAssembler masm, Register result, JavaConstant input)
    {
        /*
         * Note: we use the kind of the input operand (and not the kind of the result operand)
         * because they don't match in all cases. For example, an object constant can be loaded to a
         * long register when unsafe casts occurred (e.g., for a write barrier where arithmetic
         * operations are then performed on the pointer).
         */
        switch (input.getJavaKind().getStackKind())
        {
            case Int:
                // Do not optimize with an XOR as this instruction may be between
                // a CMP and a Jcc in which case the XOR will modify the condition
                // flags and interfere with the Jcc.
                masm.movl(result, input.asInt());

                break;
            case Long:
                // Do not optimize with an XOR as this instruction may be between
                // a CMP and a Jcc in which case the XOR will modify the condition
                // flags and interfere with the Jcc.
                if (input.asLong() == (int) input.asLong())
                {
                    // Sign extended to long
                    masm.movslq(result, (int) input.asLong());
                }
                else if ((input.asLong() & 0xFFFFFFFFL) == input.asLong())
                {
                    // Zero extended to long
                    masm.movl(result, (int) input.asLong());
                }
                else
                {
                    masm.movq(result, input.asLong());
                }
                break;
            case Float:
                // This is *not* the same as 'constant == 0.0f' in the case where constant is -0.0f
                if (Float.floatToRawIntBits(input.asFloat()) == Float.floatToRawIntBits(0.0f))
                {
                    masm.xorps(result, result);
                }
                else
                {
                    masm.movflt(result, (AMD64Address) crb.asFloatConstRef(input));
                }
                break;
            case Double:
                // This is *not* the same as 'constant == 0.0d' in the case where constant is -0.0d
                if (Double.doubleToRawLongBits(input.asDouble()) == Double.doubleToRawLongBits(0.0d))
                {
                    masm.xorpd(result, result);
                }
                else
                {
                    masm.movdbl(result, (AMD64Address) crb.asDoubleConstRef(input));
                }
                break;
            case Object:
                // Do not optimize with an XOR as this instruction may be between
                // a CMP and a Jcc in which case the XOR will modify the condition
                // flags and interfere with the Jcc.
                if (input.isNull())
                {
                    masm.movq(result, 0x0L);
                }
                else if (crb.target.inlineObjects)
                {
                    crb.recordInlineDataInCode(input);
                    masm.movq(result, 0xDEADDEADDEADDEADL);
                }
                else
                {
                    masm.movq(result, (AMD64Address) crb.recordDataReferenceInCode(input, 0));
                }
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    public static boolean canMoveConst2Stack(JavaConstant input)
    {
        switch (input.getJavaKind().getStackKind())
        {
            case Int:
                break;
            case Long:
                break;
            case Float:
                break;
            case Double:
                break;
            case Object:
                if (input.isNull())
                {
                    return true;
                }
                else
                {
                    return false;
                }
            default:
                return false;
        }
        return true;
    }

    public static void const2stack(CompilationResultBuilder crb, AMD64MacroAssembler masm, Value result, JavaConstant input)
    {
        AMD64Address dest = (AMD64Address) crb.asAddress(result);
        final long imm;
        switch (input.getJavaKind().getStackKind())
        {
            case Int:
                imm = input.asInt();
                break;
            case Long:
                imm = input.asLong();
                break;
            case Float:
                imm = Float.floatToRawIntBits(input.asFloat());
                break;
            case Double:
                imm = Double.doubleToRawLongBits(input.asDouble());
                break;
            case Object:
                if (input.isNull())
                {
                    imm = 0;
                }
                else
                {
                    throw GraalError.shouldNotReachHere("Non-null object constants must be in register");
                }
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }

        switch ((AMD64Kind) result.getPlatformKind())
        {
            case BYTE:
                AMD64MIOp.MOVB.emit(masm, OperandSize.BYTE, dest, (int) imm);
                break;
            case WORD:
                AMD64MIOp.MOV.emit(masm, OperandSize.WORD, dest, (int) imm);
                break;
            case DWORD:
            case SINGLE:
                masm.movl(dest, (int) imm);
                break;
            case QWORD:
            case DOUBLE:
                masm.movlong(dest, imm);
                break;
            default:
                throw GraalError.shouldNotReachHere("Unknown result Kind: " + result.getPlatformKind());
        }
    }

    public abstract static class PointerCompressionOp extends AMD64LIRInstruction
    {
        protected final LIRKindTool lirKindTool;
        protected final CompressEncoding encoding;
        protected final boolean nonNull;

        @Def({OperandFlag.REG, OperandFlag.HINT}) private AllocatableValue result;
        @Use({OperandFlag.REG, OperandFlag.CONST}) private Value input;
        @Alive({OperandFlag.REG, OperandFlag.ILLEGAL, OperandFlag.UNINITIALIZED}) private AllocatableValue baseRegister;

        protected PointerCompressionOp(LIRInstructionClass<? extends PointerCompressionOp> type, AllocatableValue result, Value input, AllocatableValue baseRegister, CompressEncoding encoding, boolean nonNull, LIRKindTool lirKindTool)
        {
            super(type);
            this.result = result;
            this.input = input;
            this.baseRegister = baseRegister;
            this.encoding = encoding;
            this.nonNull = nonNull;
            this.lirKindTool = lirKindTool;
        }

        public static boolean hasBase(OptionValues options, CompressEncoding encoding)
        {
            return encoding.hasBase();
        }

        public final Value getInput()
        {
            return input;
        }

        public final AllocatableValue getResult()
        {
            return result;
        }

        protected final Register getBaseRegister()
        {
            return ValueUtil.asRegister(baseRegister);
        }

        protected final int getShift()
        {
            return encoding.getShift();
        }

        protected final void move(LIRKind kind, CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            AMD64Move.move((AMD64Kind) kind.getPlatformKind(), crb, masm, result, input);
        }
    }

    public static class CompressPointerOp extends PointerCompressionOp
    {
        public static final LIRInstructionClass<CompressPointerOp> TYPE = LIRInstructionClass.create(CompressPointerOp.class);

        public CompressPointerOp(AllocatableValue result, Value input, AllocatableValue baseRegister, CompressEncoding encoding, boolean nonNull, LIRKindTool lirKindTool)
        {
            this(TYPE, result, input, baseRegister, encoding, nonNull, lirKindTool);
        }

        protected CompressPointerOp(LIRInstructionClass<? extends PointerCompressionOp> type, AllocatableValue result, Value input, AllocatableValue baseRegister, CompressEncoding encoding, boolean nonNull, LIRKindTool lirKindTool)
        {
            super(type, result, input, baseRegister, encoding, nonNull, lirKindTool);
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            move(lirKindTool.getObjectKind(), crb, masm);

            Register resReg = ValueUtil.asRegister(getResult());
            if (hasBase(crb.getOptions(), encoding))
            {
                Register baseReg = getBaseRegister();
                if (!nonNull)
                {
                    masm.testq(resReg, resReg);
                    masm.cmovq(ConditionFlag.Equal, resReg, baseReg);
                }
                masm.subq(resReg, baseReg);
            }

            int shift = getShift();
            if (shift != 0)
            {
                masm.shrq(resReg, shift);
            }
        }
    }

    public static class UncompressPointerOp extends PointerCompressionOp
    {
        public static final LIRInstructionClass<UncompressPointerOp> TYPE = LIRInstructionClass.create(UncompressPointerOp.class);

        public UncompressPointerOp(AllocatableValue result, Value input, AllocatableValue baseRegister, CompressEncoding encoding, boolean nonNull, LIRKindTool lirKindTool)
        {
            this(TYPE, result, input, baseRegister, encoding, nonNull, lirKindTool);
        }

        protected UncompressPointerOp(LIRInstructionClass<? extends PointerCompressionOp> type, AllocatableValue result, Value input, AllocatableValue baseRegister, CompressEncoding encoding, boolean nonNull, LIRKindTool lirKindTool)
        {
            super(type, result, input, baseRegister, encoding, nonNull, lirKindTool);
        }

        @Override
        public void emitCode(CompilationResultBuilder crb, AMD64MacroAssembler masm)
        {
            move(lirKindTool.getNarrowOopKind(), crb, masm);
            emitUncompressCode(masm, ValueUtil.asRegister(getResult()), getShift(), hasBase(crb.getOptions(), encoding) ? getBaseRegister() : null, nonNull);
        }

        public static void emitUncompressCode(AMD64MacroAssembler masm, Register resReg, int shift, Register baseReg, boolean nonNull)
        {
            if (shift != 0)
            {
                masm.shlq(resReg, shift);
            }

            if (baseReg != null)
            {
                if (nonNull)
                {
                    masm.addq(resReg, baseReg);
                    return;
                }

                if (shift == 0)
                {
                    // if encoding.shift != 0, the flags are already set by the shlq
                    masm.testq(resReg, resReg);
                }

                Label done = new Label();
                masm.jccb(ConditionFlag.Equal, done);
                masm.addq(resReg, baseReg);
                masm.bind(done);
            }
        }
    }
}
