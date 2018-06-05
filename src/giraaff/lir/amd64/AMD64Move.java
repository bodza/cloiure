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
import giraaff.util.GraalError;

// @class AMD64Move
public final class AMD64Move
{
    // @class AMD64Move.AbstractMoveOp
    private abstract static class AbstractMoveOp extends AMD64LIRInstruction implements ValueMoveOp
    {
        // @def
        public static final LIRInstructionClass<AbstractMoveOp> TYPE = LIRInstructionClass.create(AbstractMoveOp.class);

        // @field
        private AMD64Kind ___moveKind;

        // @cons
        protected AbstractMoveOp(LIRInstructionClass<? extends AbstractMoveOp> __c, AMD64Kind __moveKind)
        {
            super(__c);
            this.___moveKind = __moveKind;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            move(this.___moveKind, __crb, __masm, getResult(), getInput());
        }
    }

    @Opcode
    // @class AMD64Move.MoveToRegOp
    public static final class MoveToRegOp extends AbstractMoveOp
    {
        // @def
        public static final LIRInstructionClass<MoveToRegOp> TYPE = LIRInstructionClass.create(MoveToRegOp.class);

        @Def({OperandFlag.REG, OperandFlag.HINT})
        // @field
        protected AllocatableValue ___result;
        @Use({OperandFlag.REG, OperandFlag.STACK})
        // @field
        protected AllocatableValue ___input;

        // @cons
        public MoveToRegOp(AMD64Kind __moveKind, AllocatableValue __result, AllocatableValue __input)
        {
            super(TYPE, __moveKind);
            this.___result = __result;
            this.___input = __input;
        }

        @Override
        public AllocatableValue getInput()
        {
            return this.___input;
        }

        @Override
        public AllocatableValue getResult()
        {
            return this.___result;
        }
    }

    @Opcode
    // @class AMD64Move.MoveFromRegOp
    public static final class MoveFromRegOp extends AbstractMoveOp
    {
        // @def
        public static final LIRInstructionClass<MoveFromRegOp> TYPE = LIRInstructionClass.create(MoveFromRegOp.class);

        @Def({OperandFlag.REG, OperandFlag.STACK})
        // @field
        protected AllocatableValue ___result;
        @Use({OperandFlag.REG, OperandFlag.HINT})
        // @field
        protected AllocatableValue ___input;

        // @cons
        public MoveFromRegOp(AMD64Kind __moveKind, AllocatableValue __result, AllocatableValue __input)
        {
            super(TYPE, __moveKind);
            this.___result = __result;
            this.___input = __input;
        }

        @Override
        public AllocatableValue getInput()
        {
            return this.___input;
        }

        @Override
        public AllocatableValue getResult()
        {
            return this.___result;
        }
    }

    @Opcode
    // @class AMD64Move.MoveFromConstOp
    public static final class MoveFromConstOp extends AMD64LIRInstruction implements LoadConstantOp
    {
        // @def
        public static final LIRInstructionClass<MoveFromConstOp> TYPE = LIRInstructionClass.create(MoveFromConstOp.class);

        @Def({OperandFlag.REG, OperandFlag.STACK})
        // @field
        protected AllocatableValue ___result;
        // @field
        private final JavaConstant ___input;

        // @cons
        public MoveFromConstOp(AllocatableValue __result, JavaConstant __input)
        {
            super(TYPE);
            this.___result = __result;
            this.___input = __input;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            if (ValueUtil.isRegister(this.___result))
            {
                const2reg(__crb, __masm, ValueUtil.asRegister(this.___result), this.___input);
            }
            else
            {
                const2stack(__crb, __masm, this.___result, this.___input);
            }
        }

        @Override
        public Constant getConstant()
        {
            return this.___input;
        }

        @Override
        public AllocatableValue getResult()
        {
            return this.___result;
        }
    }

    @Opcode
    // @class AMD64Move.AMD64StackMove
    public static final class AMD64StackMove extends AMD64LIRInstruction implements ValueMoveOp
    {
        // @def
        public static final LIRInstructionClass<AMD64StackMove> TYPE = LIRInstructionClass.create(AMD64StackMove.class);

        @Def({OperandFlag.STACK})
        // @field
        protected AllocatableValue ___result;
        @Use({OperandFlag.STACK, OperandFlag.HINT})
        // @field
        protected AllocatableValue ___input;
        @Alive({OperandFlag.STACK, OperandFlag.UNINITIALIZED})
        // @field
        private AllocatableValue ___backupSlot;

        // @field
        private Register ___scratch;

        // @cons
        public AMD64StackMove(AllocatableValue __result, AllocatableValue __input, Register __scratch, AllocatableValue __backupSlot)
        {
            super(TYPE);
            this.___result = __result;
            this.___input = __input;
            this.___backupSlot = __backupSlot;
            this.___scratch = __scratch;
        }

        @Override
        public AllocatableValue getInput()
        {
            return this.___input;
        }

        @Override
        public AllocatableValue getResult()
        {
            return this.___result;
        }

        public Register getScratchRegister()
        {
            return this.___scratch;
        }

        public AllocatableValue getBackupSlot()
        {
            return this.___backupSlot;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            AMD64Kind __backupKind = (AMD64Kind) this.___backupSlot.getPlatformKind();
            if (__backupKind.isXMM())
            {
                // graal doesn't use vector values, so it's safe to backup using DOUBLE
                __backupKind = AMD64Kind.DOUBLE;
            }

            // backup scratch register
            reg2stack(__backupKind, __crb, __masm, this.___backupSlot, this.___scratch);
            // move stack slot
            stack2reg((AMD64Kind) getInput().getPlatformKind(), __crb, __masm, this.___scratch, getInput());
            reg2stack((AMD64Kind) getResult().getPlatformKind(), __crb, __masm, getResult(), this.___scratch);
            // restore scratch register
            stack2reg(__backupKind, __crb, __masm, this.___scratch, this.___backupSlot);
        }
    }

    @Opcode
    // @class AMD64Move.AMD64MultiStackMove
    public static final class AMD64MultiStackMove extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<AMD64MultiStackMove> TYPE = LIRInstructionClass.create(AMD64MultiStackMove.class);

        @Def({OperandFlag.STACK})
        // @field
        protected AllocatableValue[] ___results;
        @Use({OperandFlag.STACK})
        // @field
        protected Value[] ___inputs;
        @Alive({OperandFlag.STACK, OperandFlag.UNINITIALIZED})
        // @field
        private AllocatableValue ___backupSlot;

        // @field
        private Register ___scratch;

        // @cons
        public AMD64MultiStackMove(AllocatableValue[] __results, Value[] __inputs, Register __scratch, AllocatableValue __backupSlot)
        {
            super(TYPE);
            this.___results = __results;
            this.___inputs = __inputs;
            this.___backupSlot = __backupSlot;
            this.___scratch = __scratch;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            AMD64Kind __backupKind = (AMD64Kind) this.___backupSlot.getPlatformKind();
            if (__backupKind.isXMM())
            {
                // graal doesn't use vector values, so it's safe to backup using DOUBLE
                __backupKind = AMD64Kind.DOUBLE;
            }

            // backup scratch register
            move(__backupKind, __crb, __masm, this.___backupSlot, this.___scratch.asValue(this.___backupSlot.getValueKind()));
            for (int __i = 0; __i < this.___results.length; __i++)
            {
                Value __input = this.___inputs[__i];
                AllocatableValue __result = this.___results[__i];
                // move stack slot
                move((AMD64Kind) __input.getPlatformKind(), __crb, __masm, this.___scratch.asValue(__input.getValueKind()), __input);
                move((AMD64Kind) __result.getPlatformKind(), __crb, __masm, __result, this.___scratch.asValue(__result.getValueKind()));
            }
            // restore scratch register
            move(__backupKind, __crb, __masm, this.___scratch.asValue(this.___backupSlot.getValueKind()), this.___backupSlot);
        }
    }

    @Opcode
    // @class AMD64Move.AMD64PushPopStackMove
    public static final class AMD64PushPopStackMove extends AMD64LIRInstruction implements ValueMoveOp
    {
        // @def
        public static final LIRInstructionClass<AMD64PushPopStackMove> TYPE = LIRInstructionClass.create(AMD64PushPopStackMove.class);

        @Def({OperandFlag.STACK})
        // @field
        protected AllocatableValue ___result;
        @Use({OperandFlag.STACK, OperandFlag.HINT})
        // @field
        protected AllocatableValue ___input;
        // @field
        private final OperandSize ___size;

        // @cons
        public AMD64PushPopStackMove(OperandSize __size, AllocatableValue __result, AllocatableValue __input)
        {
            super(TYPE);
            this.___result = __result;
            this.___input = __input;
            this.___size = __size;
        }

        @Override
        public AllocatableValue getInput()
        {
            return this.___input;
        }

        @Override
        public AllocatableValue getResult()
        {
            return this.___result;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            AMD64MOp.PUSH.emit(__masm, this.___size, (AMD64Address) __crb.asAddress(this.___input));
            AMD64MOp.POP.emit(__masm, this.___size, (AMD64Address) __crb.asAddress(this.___result));
        }
    }

    // @class AMD64Move.LeaOp
    public static final class LeaOp extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<LeaOp> TYPE = LIRInstructionClass.create(LeaOp.class);

        @Def({OperandFlag.REG})
        // @field
        protected AllocatableValue ___result;
        @Use({OperandFlag.COMPOSITE, OperandFlag.UNINITIALIZED})
        // @field
        protected AMD64AddressValue ___address;
        // @field
        private final OperandSize ___size;

        // @cons
        public LeaOp(AllocatableValue __result, AMD64AddressValue __address, OperandSize __size)
        {
            super(TYPE);
            this.___result = __result;
            this.___address = __address;
            this.___size = __size;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            if (this.___size == OperandSize.QWORD)
            {
                __masm.leaq(ValueUtil.asRegister(this.___result, AMD64Kind.QWORD), this.___address.toAddress());
            }
            else
            {
                __masm.lead(ValueUtil.asRegister(this.___result, AMD64Kind.DWORD), this.___address.toAddress());
            }
        }
    }

    // @class AMD64Move.LeaDataOp
    public static final class LeaDataOp extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<LeaDataOp> TYPE = LIRInstructionClass.create(LeaDataOp.class);

        @Def({OperandFlag.REG})
        // @field
        protected AllocatableValue ___result;
        // @field
        private final DataPointerConstant ___data;

        // @cons
        public LeaDataOp(AllocatableValue __result, DataPointerConstant __data)
        {
            super(TYPE);
            this.___result = __result;
            this.___data = __data;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            __masm.leaq(ValueUtil.asRegister(this.___result), (AMD64Address) __crb.recordDataReferenceInCode(this.___data));
        }
    }

    // @class AMD64Move.StackLeaOp
    public static final class StackLeaOp extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<StackLeaOp> TYPE = LIRInstructionClass.create(StackLeaOp.class);

        @Def({OperandFlag.REG})
        // @field
        protected AllocatableValue ___result;
        @Use({OperandFlag.STACK, OperandFlag.UNINITIALIZED})
        // @field
        protected AllocatableValue ___slot;

        // @cons
        public StackLeaOp(AllocatableValue __result, AllocatableValue __slot)
        {
            super(TYPE);
            this.___result = __result;
            this.___slot = __slot;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            __masm.leaq(ValueUtil.asRegister(this.___result, AMD64Kind.QWORD), (AMD64Address) __crb.asAddress(this.___slot));
        }
    }

    // @class AMD64Move.MembarOp
    public static final class MembarOp extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<MembarOp> TYPE = LIRInstructionClass.create(MembarOp.class);

        // @field
        private final int ___barriers;

        // @cons
        public MembarOp(final int __barriers)
        {
            super(TYPE);
            this.___barriers = __barriers;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            __masm.membar(this.___barriers);
        }
    }

    // @class AMD64Move.NullCheckOp
    public static final class NullCheckOp extends AMD64LIRInstruction implements NullCheck
    {
        // @def
        public static final LIRInstructionClass<NullCheckOp> TYPE = LIRInstructionClass.create(NullCheckOp.class);

        @Use({OperandFlag.COMPOSITE})
        // @field
        protected AMD64AddressValue ___address;
        // @State
        // @field
        protected LIRFrameState ___state;

        // @cons
        public NullCheckOp(AMD64AddressValue __address, LIRFrameState __state)
        {
            super(TYPE);
            this.___address = __address;
            this.___state = __state;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            __masm.nullCheck(this.___address.toAddress());
        }

        @Override
        public Value getCheckedValue()
        {
            return this.___address.___base;
        }

        @Override
        public LIRFrameState getState()
        {
            return this.___state;
        }
    }

    @Opcode
    // @class AMD64Move.CompareAndSwapOp
    public static final class CompareAndSwapOp extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<CompareAndSwapOp> TYPE = LIRInstructionClass.create(CompareAndSwapOp.class);

        // @field
        private final AMD64Kind ___accessKind;

        @Def
        // @field
        protected AllocatableValue ___result;
        @Use({OperandFlag.COMPOSITE})
        // @field
        protected AMD64AddressValue ___address;
        @Use
        // @field
        protected AllocatableValue ___cmpValue;
        @Use
        // @field
        protected AllocatableValue ___newValue;

        // @cons
        public CompareAndSwapOp(AMD64Kind __accessKind, AllocatableValue __result, AMD64AddressValue __address, AllocatableValue __cmpValue, AllocatableValue __newValue)
        {
            super(TYPE);
            this.___accessKind = __accessKind;
            this.___result = __result;
            this.___address = __address;
            this.___cmpValue = __cmpValue;
            this.___newValue = __newValue;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            if (__crb.___target.isMP)
            {
                __masm.lock();
            }
            switch (this.___accessKind)
            {
                case DWORD:
                {
                    __masm.cmpxchgl(ValueUtil.asRegister(this.___newValue), this.___address.toAddress());
                    break;
                }
                case QWORD:
                {
                    __masm.cmpxchgq(ValueUtil.asRegister(this.___newValue), this.___address.toAddress());
                    break;
                }
                default:
                    throw GraalError.shouldNotReachHere();
            }
        }
    }

    @Opcode
    // @class AMD64Move.AtomicReadAndAddOp
    public static final class AtomicReadAndAddOp extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<AtomicReadAndAddOp> TYPE = LIRInstructionClass.create(AtomicReadAndAddOp.class);

        // @field
        private final AMD64Kind ___accessKind;

        @Def
        // @field
        protected AllocatableValue ___result;
        @Alive({OperandFlag.COMPOSITE})
        // @field
        protected AMD64AddressValue ___address;
        @Use
        // @field
        protected AllocatableValue ___delta;

        // @cons
        public AtomicReadAndAddOp(AMD64Kind __accessKind, AllocatableValue __result, AMD64AddressValue __address, AllocatableValue __delta)
        {
            super(TYPE);
            this.___accessKind = __accessKind;
            this.___result = __result;
            this.___address = __address;
            this.___delta = __delta;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            move(this.___accessKind, __crb, __masm, this.___result, this.___delta);
            if (__crb.___target.isMP)
            {
                __masm.lock();
            }
            switch (this.___accessKind)
            {
                case DWORD:
                {
                    __masm.xaddl(this.___address.toAddress(), ValueUtil.asRegister(this.___result));
                    break;
                }
                case QWORD:
                {
                    __masm.xaddq(this.___address.toAddress(), ValueUtil.asRegister(this.___result));
                    break;
                }
                default:
                    throw GraalError.shouldNotReachHere();
            }
        }
    }

    @Opcode
    // @class AMD64Move.AtomicReadAndWriteOp
    public static final class AtomicReadAndWriteOp extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<AtomicReadAndWriteOp> TYPE = LIRInstructionClass.create(AtomicReadAndWriteOp.class);

        // @field
        private final AMD64Kind ___accessKind;

        @Def
        // @field
        protected AllocatableValue ___result;
        @Alive({OperandFlag.COMPOSITE})
        // @field
        protected AMD64AddressValue ___address;
        @Use
        // @field
        protected AllocatableValue ___newValue;

        // @cons
        public AtomicReadAndWriteOp(AMD64Kind __accessKind, AllocatableValue __result, AMD64AddressValue __address, AllocatableValue __newValue)
        {
            super(TYPE);
            this.___accessKind = __accessKind;
            this.___result = __result;
            this.___address = __address;
            this.___newValue = __newValue;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            move(this.___accessKind, __crb, __masm, this.___result, this.___newValue);
            switch (this.___accessKind)
            {
                case DWORD:
                {
                    __masm.xchgl(ValueUtil.asRegister(this.___result), this.___address.toAddress());
                    break;
                }
                case QWORD:
                {
                    __masm.xchgq(ValueUtil.asRegister(this.___result), this.___address.toAddress());
                    break;
                }
                default:
                    throw GraalError.shouldNotReachHere();
            }
        }
    }

    public static void move(CompilationResultBuilder __crb, AMD64MacroAssembler __masm, Value __result, Value __input)
    {
        move((AMD64Kind) __result.getPlatformKind(), __crb, __masm, __result, __input);
    }

    public static void move(AMD64Kind __moveKind, CompilationResultBuilder __crb, AMD64MacroAssembler __masm, Value __result, Value __input)
    {
        if (ValueUtil.isRegister(__input))
        {
            if (ValueUtil.isRegister(__result))
            {
                reg2reg(__moveKind, __masm, __result, __input);
            }
            else if (ValueUtil.isStackSlot(__result))
            {
                reg2stack(__moveKind, __crb, __masm, __result, ValueUtil.asRegister(__input));
            }
            else
            {
                throw GraalError.shouldNotReachHere();
            }
        }
        else if (ValueUtil.isStackSlot(__input))
        {
            if (ValueUtil.isRegister(__result))
            {
                stack2reg(__moveKind, __crb, __masm, ValueUtil.asRegister(__result), __input);
            }
            else
            {
                throw GraalError.shouldNotReachHere();
            }
        }
        else if (LIRValueUtil.isJavaConstant(__input))
        {
            if (ValueUtil.isRegister(__result))
            {
                const2reg(__crb, __masm, ValueUtil.asRegister(__result), LIRValueUtil.asJavaConstant(__input));
            }
            else if (ValueUtil.isStackSlot(__result))
            {
                const2stack(__crb, __masm, __result, LIRValueUtil.asJavaConstant(__input));
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

    private static void reg2reg(AMD64Kind __kind, AMD64MacroAssembler __masm, Value __result, Value __input)
    {
        if (ValueUtil.asRegister(__input).equals(ValueUtil.asRegister(__result)))
        {
            return;
        }
        switch (__kind)
        {
            case BYTE:
            case WORD:
            case DWORD:
            {
                __masm.movl(ValueUtil.asRegister(__result), ValueUtil.asRegister(__input));
                break;
            }
            case QWORD:
            {
                __masm.movq(ValueUtil.asRegister(__result), ValueUtil.asRegister(__input));
                break;
            }
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    public static void reg2stack(AMD64Kind __kind, CompilationResultBuilder __crb, AMD64MacroAssembler __masm, Value __result, Register __input)
    {
        AMD64Address __dest = (AMD64Address) __crb.asAddress(__result);
        switch (__kind)
        {
            case BYTE:
            {
                __masm.movb(__dest, __input);
                break;
            }
            case WORD:
            {
                __masm.movw(__dest, __input);
                break;
            }
            case DWORD:
            {
                __masm.movl(__dest, __input);
                break;
            }
            case QWORD:
            {
                __masm.movq(__dest, __input);
                break;
            }
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    public static void stack2reg(AMD64Kind __kind, CompilationResultBuilder __crb, AMD64MacroAssembler __masm, Register __result, Value __input)
    {
        AMD64Address __src = (AMD64Address) __crb.asAddress(__input);
        switch (__kind)
        {
            case BYTE:
            {
                __masm.movsbl(__result, __src);
                break;
            }
            case WORD:
            {
                __masm.movswl(__result, __src);
                break;
            }
            case DWORD:
            {
                __masm.movl(__result, __src);
                break;
            }
            case QWORD:
            {
                __masm.movq(__result, __src);
                break;
            }
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    public static void const2reg(CompilationResultBuilder __crb, AMD64MacroAssembler __masm, Register __result, JavaConstant __input)
    {
        // Note: we use the kind of the input operand (and not the kind of the result operand),
        // because they don't match in all cases. For example, an object constant can be loaded to
        // a long register when unsafe casts occurred (e.g. for a write barrier where arithmetic
        // operations are then performed on the pointer).
        switch (__input.getJavaKind().getStackKind())
        {
            case Int:
            {
                // Do not optimize with an XOR, as this instruction may be between a CMP and a Jcc,
                // in which case the XOR will modify the condition flags and interfere with the Jcc.
                __masm.movl(__result, __input.asInt());
                break;
            }
            case Long:
                // Do not optimize with an XOR, as this instruction may be between a CMP and a Jcc,
                // in which case the XOR will modify the condition flags and interfere with the Jcc.
                if (__input.asLong() == (int) __input.asLong())
                {
                    // sign extended to long
                    __masm.movslq(__result, (int) __input.asLong());
                }
                else if ((__input.asLong() & 0xFFFFFFFFL) == __input.asLong())
                {
                    // zero extended to long
                    __masm.movl(__result, (int) __input.asLong());
                }
                else
                {
                    __masm.movq(__result, __input.asLong());
                }
                break;
            case Object:
                // Do not optimize with an XOR, as this instruction may be between a CMP and a Jcc,
                // in which case the XOR will modify the condition flags and interfere with the Jcc.
                if (__input.isNull())
                {
                    __masm.movq(__result, 0x0L);
                }
                else if (__crb.___target.inlineObjects)
                {
                    __crb.recordInlineDataInCode(__input);
                    __masm.movq(__result, 0xDEADDEADDEADDEADL);
                }
                else
                {
                    __masm.movq(__result, (AMD64Address) __crb.recordDataReferenceInCode(__input, 0));
                }
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    public static boolean canMoveConst2Stack(JavaConstant __input)
    {
        switch (__input.getJavaKind().getStackKind())
        {
            case Int:
                break;
            case Long:
                break;
            case Object:
                if (__input.isNull())
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

    public static void const2stack(CompilationResultBuilder __crb, AMD64MacroAssembler __masm, Value __result, JavaConstant __input)
    {
        AMD64Address __dest = (AMD64Address) __crb.asAddress(__result);
        final long __imm;
        switch (__input.getJavaKind().getStackKind())
        {
            case Int:
            {
                __imm = __input.asInt();
                break;
            }
            case Long:
            {
                __imm = __input.asLong();
                break;
            }
            case Object:
                if (__input.isNull())
                {
                    __imm = 0;
                }
                else
                {
                    throw GraalError.shouldNotReachHere("non-null object constants must be in register");
                }
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }

        switch ((AMD64Kind) __result.getPlatformKind())
        {
            case BYTE:
            {
                AMD64MIOp.MOVB.emit(__masm, OperandSize.BYTE, __dest, (int) __imm);
                break;
            }
            case WORD:
            {
                AMD64MIOp.MOV.emit(__masm, OperandSize.WORD, __dest, (int) __imm);
                break;
            }
            case DWORD:
            {
                __masm.movl(__dest, (int) __imm);
                break;
            }
            case QWORD:
            {
                __masm.movlong(__dest, __imm);
                break;
            }
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    // @class AMD64Move.PointerCompressionOp
    public abstract static class PointerCompressionOp extends AMD64LIRInstruction
    {
        // @field
        protected final LIRKindTool ___lirKindTool;
        // @field
        protected final CompressEncoding ___encoding;
        // @field
        protected final boolean ___nonNull;

        @Def({OperandFlag.REG, OperandFlag.HINT})
        // @field
        private AllocatableValue ___result;
        @Use({OperandFlag.REG, OperandFlag.CONST})
        // @field
        private Value ___input;
        @Alive({OperandFlag.REG, OperandFlag.ILLEGAL, OperandFlag.UNINITIALIZED})
        // @field
        private AllocatableValue ___baseRegister;

        // @cons
        protected PointerCompressionOp(LIRInstructionClass<? extends PointerCompressionOp> __type, AllocatableValue __result, Value __input, AllocatableValue __baseRegister, CompressEncoding __encoding, boolean __nonNull, LIRKindTool __lirKindTool)
        {
            super(__type);
            this.___result = __result;
            this.___input = __input;
            this.___baseRegister = __baseRegister;
            this.___encoding = __encoding;
            this.___nonNull = __nonNull;
            this.___lirKindTool = __lirKindTool;
        }

        public static boolean hasBase(CompressEncoding __encoding)
        {
            return __encoding.hasBase();
        }

        public final Value getInput()
        {
            return this.___input;
        }

        public final AllocatableValue getResult()
        {
            return this.___result;
        }

        protected final Register getBaseRegister()
        {
            return ValueUtil.asRegister(this.___baseRegister);
        }

        protected final int getShift()
        {
            return this.___encoding.getShift();
        }

        protected final void move(LIRKind __kind, CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            AMD64Move.move((AMD64Kind) __kind.getPlatformKind(), __crb, __masm, this.___result, this.___input);
        }
    }

    // @class AMD64Move.CompressPointerOp
    public static final class CompressPointerOp extends PointerCompressionOp
    {
        // @def
        public static final LIRInstructionClass<CompressPointerOp> TYPE = LIRInstructionClass.create(CompressPointerOp.class);

        // @cons
        public CompressPointerOp(AllocatableValue __result, Value __input, AllocatableValue __baseRegister, CompressEncoding __encoding, boolean __nonNull, LIRKindTool __lirKindTool)
        {
            this(TYPE, __result, __input, __baseRegister, __encoding, __nonNull, __lirKindTool);
        }

        // @cons
        protected CompressPointerOp(LIRInstructionClass<? extends PointerCompressionOp> __type, AllocatableValue __result, Value __input, AllocatableValue __baseRegister, CompressEncoding __encoding, boolean __nonNull, LIRKindTool __lirKindTool)
        {
            super(__type, __result, __input, __baseRegister, __encoding, __nonNull, __lirKindTool);
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            move(this.___lirKindTool.getObjectKind(), __crb, __masm);

            Register __resReg = ValueUtil.asRegister(getResult());
            if (hasBase(this.___encoding))
            {
                Register __baseReg = getBaseRegister();
                if (!this.___nonNull)
                {
                    __masm.testq(__resReg, __resReg);
                    __masm.cmovq(ConditionFlag.Equal, __resReg, __baseReg);
                }
                __masm.subq(__resReg, __baseReg);
            }

            int __shift = getShift();
            if (__shift != 0)
            {
                __masm.shrq(__resReg, __shift);
            }
        }
    }

    // @class AMD64Move.UncompressPointerOp
    public static final class UncompressPointerOp extends PointerCompressionOp
    {
        // @def
        public static final LIRInstructionClass<UncompressPointerOp> TYPE = LIRInstructionClass.create(UncompressPointerOp.class);

        // @cons
        public UncompressPointerOp(AllocatableValue __result, Value __input, AllocatableValue __baseRegister, CompressEncoding __encoding, boolean __nonNull, LIRKindTool __lirKindTool)
        {
            this(TYPE, __result, __input, __baseRegister, __encoding, __nonNull, __lirKindTool);
        }

        // @cons
        protected UncompressPointerOp(LIRInstructionClass<? extends PointerCompressionOp> __type, AllocatableValue __result, Value __input, AllocatableValue __baseRegister, CompressEncoding __encoding, boolean __nonNull, LIRKindTool __lirKindTool)
        {
            super(__type, __result, __input, __baseRegister, __encoding, __nonNull, __lirKindTool);
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            move(this.___lirKindTool.getNarrowOopKind(), __crb, __masm);
            emitUncompressCode(__masm, ValueUtil.asRegister(getResult()), getShift(), hasBase(this.___encoding) ? getBaseRegister() : null, this.___nonNull);
        }

        public static void emitUncompressCode(AMD64MacroAssembler __masm, Register __resReg, int __shift, Register __baseReg, boolean __nonNull)
        {
            if (__shift != 0)
            {
                __masm.shlq(__resReg, __shift);
            }

            if (__baseReg != null)
            {
                if (__nonNull)
                {
                    __masm.addq(__resReg, __baseReg);
                    return;
                }

                if (__shift == 0)
                {
                    // if encoding.shift != 0, the flags are already set by the shlq
                    __masm.testq(__resReg, __resReg);
                }

                Label __done = new Label();
                __masm.jccb(ConditionFlag.Equal, __done);
                __masm.addq(__resReg, __baseReg);
                __masm.bind(__done);
            }
        }
    }
}
