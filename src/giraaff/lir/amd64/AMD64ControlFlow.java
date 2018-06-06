package giraaff.lir.amd64;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.amd64.AMD64.CPUFeature;
import jdk.vm.ci.amd64.AMD64Kind;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.Value;

import giraaff.asm.Label;
import giraaff.asm.amd64.AMD64Address;
import giraaff.asm.amd64.AMD64Assembler;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.core.common.calc.Condition;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.LabelRef;
import giraaff.lir.LIROpcode;
import giraaff.lir.StandardOp;
import giraaff.lir.SwitchStrategy;
import giraaff.lir.Variable;
import giraaff.lir.asm.CompilationResultBuilder;
import giraaff.util.GraalError;

// @class AMD64ControlFlow
public final class AMD64ControlFlow
{
    // @class AMD64ControlFlow.ReturnOp
    public static final class ReturnOp extends AMD64BlockEndOp implements StandardOp.BlockEndOp
    {
        // @def
        public static final LIRInstructionClass<AMD64ControlFlow.ReturnOp> TYPE = LIRInstructionClass.create(AMD64ControlFlow.ReturnOp.class);

        @LIRInstruction.Use({LIRInstruction.OperandFlag.REG, LIRInstruction.OperandFlag.ILLEGAL})
        // @field
        protected Value ___x;

        // @cons AMD64ControlFlow.ReturnOp
        public ReturnOp(Value __x)
        {
            super(TYPE);
            this.___x = __x;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            __crb.___frameContext.leave(__crb);
            // We potentially return to the interpreter, and that's an AVX-SSE transition. The only
            // live value at this point should be the return value in either rax, or in xmm0 with
            // the upper half of the register unused, so we don't destroy any value here.
            if (__masm.supports(CPUFeature.AVX))
            {
                __masm.vzeroupper();
            }
            __masm.ret(0);
        }
    }

    // @class AMD64ControlFlow.BranchOp
    public static class BranchOp extends AMD64BlockEndOp implements StandardOp.StandardBranchOp
    {
        // @def
        public static final LIRInstructionClass<AMD64ControlFlow.BranchOp> TYPE = LIRInstructionClass.create(AMD64ControlFlow.BranchOp.class);

        // @field
        protected final AMD64Assembler.ConditionFlag ___condition;
        // @field
        protected final LabelRef ___trueDestination;
        // @field
        protected final LabelRef ___falseDestination;

        // @field
        private final double ___trueDestinationProbability;

        // @cons AMD64ControlFlow.BranchOp
        public BranchOp(Condition __condition, LabelRef __trueDestination, LabelRef __falseDestination, double __trueDestinationProbability)
        {
            this(intCond(__condition), __trueDestination, __falseDestination, __trueDestinationProbability);
        }

        // @cons AMD64ControlFlow.BranchOp
        public BranchOp(AMD64Assembler.ConditionFlag __condition, LabelRef __trueDestination, LabelRef __falseDestination, double __trueDestinationProbability)
        {
            this(TYPE, __condition, __trueDestination, __falseDestination, __trueDestinationProbability);
        }

        // @cons AMD64ControlFlow.BranchOp
        protected BranchOp(LIRInstructionClass<? extends AMD64ControlFlow.BranchOp> __c, AMD64Assembler.ConditionFlag __condition, LabelRef __trueDestination, LabelRef __falseDestination, double __trueDestinationProbability)
        {
            super(__c);
            this.___condition = __condition;
            this.___trueDestination = __trueDestination;
            this.___falseDestination = __falseDestination;
            this.___trueDestinationProbability = __trueDestinationProbability;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            boolean __isNegated = false;
            int __jccPos = __masm.position();
            // The strategy for emitting jumps is: If either trueDestination or falseDestination is the successor
            // block, assume the block scheduler did the correct thing and jcc to the other. Otherwise, we need
            // a jcc followed by a jmp. LIRInstruction.Use the branch probability to make sure it is more likely
            // to branch on the jcc (= less likely to execute both the jcc and the jmp instead of just the jcc).
            // In the case of loops, that means the jcc is the back-edge.
            if (__crb.isSuccessorEdge(this.___trueDestination))
            {
                jcc(__masm, true, this.___falseDestination);
                __isNegated = true;
            }
            else if (__crb.isSuccessorEdge(this.___falseDestination))
            {
                jcc(__masm, false, this.___trueDestination);
            }
            else if (this.___trueDestinationProbability < 0.5)
            {
                jcc(__masm, true, this.___falseDestination);
                __masm.jmp(this.___trueDestination.label());
                __isNegated = true;
            }
            else
            {
                jcc(__masm, false, this.___trueDestination);
                __masm.jmp(this.___falseDestination.label());
            }
            __crb.recordBranch(__jccPos, __isNegated);
        }

        protected void jcc(AMD64MacroAssembler __masm, boolean __negate, LabelRef __target)
        {
            __masm.jcc(__negate ? this.___condition.negate() : this.___condition, __target.label());
        }
    }

    // @class AMD64ControlFlow.StrategySwitchOp
    public static class StrategySwitchOp extends AMD64BlockEndOp
    {
        // @def
        public static final LIRInstructionClass<AMD64ControlFlow.StrategySwitchOp> TYPE = LIRInstructionClass.create(AMD64ControlFlow.StrategySwitchOp.class);

        // @field
        protected final Constant[] ___keyConstants;
        // @field
        private final LabelRef[] ___keyTargets;
        // @field
        private LabelRef ___defaultTarget;
        @LIRInstruction.Alive({LIRInstruction.OperandFlag.REG})
        // @field
        protected Value ___key;
        @LIRInstruction.Temp({LIRInstruction.OperandFlag.REG, LIRInstruction.OperandFlag.ILLEGAL})
        // @field
        protected Value ___scratch;
        // @field
        protected final SwitchStrategy ___strategy;

        // @cons AMD64ControlFlow.StrategySwitchOp
        public StrategySwitchOp(SwitchStrategy __strategy, LabelRef[] __keyTargets, LabelRef __defaultTarget, Value __key, Value __scratch)
        {
            this(TYPE, __strategy, __keyTargets, __defaultTarget, __key, __scratch);
        }

        // @cons AMD64ControlFlow.StrategySwitchOp
        protected StrategySwitchOp(LIRInstructionClass<? extends AMD64ControlFlow.StrategySwitchOp> __c, SwitchStrategy __strategy, LabelRef[] __keyTargets, LabelRef __defaultTarget, Value __key, Value __scratch)
        {
            super(__c);
            this.___strategy = __strategy;
            this.___keyConstants = __strategy.getKeyConstants();
            this.___keyTargets = __keyTargets;
            this.___defaultTarget = __defaultTarget;
            this.___key = __key;
            this.___scratch = __scratch;
        }

        @Override
        public void emitCode(final CompilationResultBuilder __crb, final AMD64MacroAssembler __masm)
        {
            this.___strategy.run(new AMD64ControlFlow.StrategySwitchOp.AMD64SwitchClosure(ValueUtil.asRegister(this.___key), __crb, __masm));
        }

        // @class AMD64ControlFlow.StrategySwitchOp.AMD64SwitchClosure
        public class AMD64SwitchClosure extends SwitchStrategy.BaseSwitchClosure
        {
            // @field
            protected final Register ___keyRegister;
            // @field
            protected final CompilationResultBuilder ___crb;
            // @field
            protected final AMD64MacroAssembler ___masm;

            // @cons AMD64ControlFlow.StrategySwitchOp.AMD64SwitchClosure
            protected AMD64SwitchClosure(Register __keyRegister, CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
            {
                super(__crb, __masm, AMD64ControlFlow.StrategySwitchOp.this.___keyTargets, AMD64ControlFlow.StrategySwitchOp.this.___defaultTarget);
                this.___keyRegister = __keyRegister;
                this.___crb = __crb;
                this.___masm = __masm;
            }

            protected void emitComparison(Constant __c)
            {
                JavaConstant __jc = (JavaConstant) __c;
                switch (__jc.getJavaKind())
                {
                    case Int:
                    {
                        long __lc = __jc.asLong();
                        this.___masm.cmpl(this.___keyRegister, (int) __lc);
                        break;
                    }
                    case Long:
                    {
                        this.___masm.cmpq(this.___keyRegister, (AMD64Address) this.___crb.asLongConstRef(__jc));
                        break;
                    }
                    case Object:
                    {
                        AMD64Move.const2reg(this.___crb, this.___masm, ValueUtil.asRegister(AMD64ControlFlow.StrategySwitchOp.this.___scratch), __jc);
                        this.___masm.cmpptr(this.___keyRegister, ValueUtil.asRegister(AMD64ControlFlow.StrategySwitchOp.this.___scratch));
                        break;
                    }
                    default:
                        throw new GraalError("switch only supported for int, long and object");
                }
            }

            @Override
            protected void conditionalJump(int __index, Condition __condition, Label __target)
            {
                emitComparison(AMD64ControlFlow.StrategySwitchOp.this.___keyConstants[__index]);
                this.___masm.jcc(intCond(__condition), __target);
            }
        }
    }

    // @class AMD64ControlFlow.TableSwitchOp
    public static final class TableSwitchOp extends AMD64BlockEndOp
    {
        // @def
        public static final LIRInstructionClass<AMD64ControlFlow.TableSwitchOp> TYPE = LIRInstructionClass.create(AMD64ControlFlow.TableSwitchOp.class);

        // @field
        private final int ___lowKey;
        // @field
        private final LabelRef ___defaultTarget;
        // @field
        private final LabelRef[] ___targets;
        @LIRInstruction.Use
        // @field
        protected Value ___index;
        @LIRInstruction.Temp({LIRInstruction.OperandFlag.REG, LIRInstruction.OperandFlag.HINT})
        // @field
        protected Value ___idxScratch;
        @LIRInstruction.Temp
        // @field
        protected Value ___scratch;

        // @cons AMD64ControlFlow.TableSwitchOp
        public TableSwitchOp(final int __lowKey, final LabelRef __defaultTarget, final LabelRef[] __targets, Value __index, Variable __scratch, Variable __idxScratch)
        {
            super(TYPE);
            this.___lowKey = __lowKey;
            this.___defaultTarget = __defaultTarget;
            this.___targets = __targets;
            this.___index = __index;
            this.___scratch = __scratch;
            this.___idxScratch = __idxScratch;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            Register __indexReg = ValueUtil.asRegister(this.___index, AMD64Kind.DWORD);
            Register __idxScratchReg = ValueUtil.asRegister(this.___idxScratch, AMD64Kind.DWORD);
            Register __scratchReg = ValueUtil.asRegister(this.___scratch, AMD64Kind.QWORD);

            if (!__indexReg.equals(__idxScratchReg))
            {
                __masm.movl(__idxScratchReg, __indexReg);
            }

            // compare index against jump table bounds
            int __highKey = this.___lowKey + this.___targets.length - 1;
            if (this.___lowKey != 0)
            {
                // subtract the low value from the switch value
                __masm.subl(__idxScratchReg, this.___lowKey);
                __masm.cmpl(__idxScratchReg, __highKey - this.___lowKey);
            }
            else
            {
                __masm.cmpl(__idxScratchReg, __highKey);
            }

            // jump to default target if index is not within the jump table
            if (this.___defaultTarget != null)
            {
                __masm.jcc(AMD64Assembler.ConditionFlag.Above, this.___defaultTarget.label());
            }

            // set scratch to address of jump table
            __masm.leaq(__scratchReg, new AMD64Address(AMD64.rip, 0));
            final int __afterLea = __masm.position();

            // load jump table entry into scratch and jump to it
            __masm.movslq(__idxScratchReg, new AMD64Address(__scratchReg, __idxScratchReg, AMD64Address.Scale.Times4, 0));
            __masm.addq(__scratchReg, __idxScratchReg);
            __masm.jmp(__scratchReg);

            // inserting padding, so that jump table address is 4-byte aligned
            if ((__masm.position() & 0x3) != 0)
            {
                __masm.nop(4 - (__masm.position() & 0x3));
            }

            // patch LEA instruction above now that we know the position of the jump table
            // TODO this is ugly and should be done differently
            final int __jumpTablePos = __masm.position();
            final int __leaDisplacementPosition = __afterLea - 4;
            __masm.emitInt(__jumpTablePos - __afterLea, __leaDisplacementPosition);

            // emit jump table entries
            for (LabelRef __target : this.___targets)
            {
                Label __label = __target.label();
                int __offsetToJumpTableBase = __masm.position() - __jumpTablePos;
                if (__label.isBound())
                {
                    int __imm32 = __label.position() - __jumpTablePos;
                    __masm.emitInt(__imm32);
                }
                else
                {
                    __label.addPatchAt(__masm.position());

                    __masm.emitByte(0); // pseudo-opcode for jump table entry
                    __masm.emitShort(__offsetToJumpTableBase);
                    __masm.emitByte(0); // padding to make jump table entry 4 bytes wide
                }
            }
        }
    }

    @LIROpcode
    // @class AMD64ControlFlow.CondSetOp
    public static final class CondSetOp extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<AMD64ControlFlow.CondSetOp> TYPE = LIRInstructionClass.create(AMD64ControlFlow.CondSetOp.class);

        @LIRInstruction.Def({LIRInstruction.OperandFlag.REG, LIRInstruction.OperandFlag.HINT})
        // @field
        protected Value ___result;
        // @field
        private final AMD64Assembler.ConditionFlag ___condition;

        // @cons AMD64ControlFlow.CondSetOp
        public CondSetOp(Variable __result, Condition __condition)
        {
            super(TYPE);
            this.___result = __result;
            this.___condition = intCond(__condition);
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            setcc(__masm, this.___result, this.___condition);
        }
    }

    @LIROpcode
    // @class AMD64ControlFlow.CondMoveOp
    public static final class CondMoveOp extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<AMD64ControlFlow.CondMoveOp> TYPE = LIRInstructionClass.create(AMD64ControlFlow.CondMoveOp.class);

        @LIRInstruction.Def({LIRInstruction.OperandFlag.REG, LIRInstruction.OperandFlag.HINT})
        // @field
        protected Value ___result;
        @LIRInstruction.Alive({LIRInstruction.OperandFlag.REG})
        // @field
        protected Value ___trueValue;
        @LIRInstruction.Use({LIRInstruction.OperandFlag.REG, LIRInstruction.OperandFlag.STACK, LIRInstruction.OperandFlag.CONST})
        // @field
        protected Value ___falseValue;
        // @field
        private final AMD64Assembler.ConditionFlag ___condition;

        // @cons AMD64ControlFlow.CondMoveOp
        public CondMoveOp(Variable __result, Condition __condition, AllocatableValue __trueValue, Value __falseValue)
        {
            super(TYPE);
            this.___result = __result;
            this.___condition = intCond(__condition);
            this.___trueValue = __trueValue;
            this.___falseValue = __falseValue;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            AMD64Move.move(__crb, __masm, this.___result, this.___falseValue);
            cmove(__crb, __masm, this.___result, this.___condition, this.___trueValue);
        }
    }

    private static void cmove(CompilationResultBuilder __crb, AMD64MacroAssembler __masm, Value __result, AMD64Assembler.ConditionFlag __cond, Value __other)
    {
        if (ValueUtil.isRegister(__other))
        {
            switch ((AMD64Kind) __other.getPlatformKind())
            {
                case BYTE:
                case WORD:
                case DWORD:
                {
                    __masm.cmovl(__cond, ValueUtil.asRegister(__result), ValueUtil.asRegister(__other));
                    break;
                }
                case QWORD:
                {
                    __masm.cmovq(__cond, ValueUtil.asRegister(__result), ValueUtil.asRegister(__other));
                    break;
                }
                default:
                    throw GraalError.shouldNotReachHere();
            }
        }
        else
        {
            AMD64Address __addr = (AMD64Address) __crb.asAddress(__other);
            switch ((AMD64Kind) __other.getPlatformKind())
            {
                case BYTE:
                case WORD:
                case DWORD:
                {
                    __masm.cmovl(__cond, ValueUtil.asRegister(__result), __addr);
                    break;
                }
                case QWORD:
                {
                    __masm.cmovq(__cond, ValueUtil.asRegister(__result), __addr);
                    break;
                }
                default:
                    throw GraalError.shouldNotReachHere();
            }
        }
    }

    private static void setcc(AMD64MacroAssembler __masm, Value __result, AMD64Assembler.ConditionFlag __cond)
    {
        switch ((AMD64Kind) __result.getPlatformKind())
        {
            case BYTE:
            case WORD:
            case DWORD:
            {
                __masm.setl(__cond, ValueUtil.asRegister(__result));
                break;
            }
            case QWORD:
            {
                __masm.setq(__cond, ValueUtil.asRegister(__result));
                break;
            }
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    private static AMD64Assembler.ConditionFlag intCond(Condition __cond)
    {
        switch (__cond)
        {
            case EQ:
                return AMD64Assembler.ConditionFlag.Equal;
            case NE:
                return AMD64Assembler.ConditionFlag.NotEqual;
            case LT:
                return AMD64Assembler.ConditionFlag.Less;
            case LE:
                return AMD64Assembler.ConditionFlag.LessEqual;
            case GE:
                return AMD64Assembler.ConditionFlag.GreaterEqual;
            case GT:
                return AMD64Assembler.ConditionFlag.Greater;
            case BE:
                return AMD64Assembler.ConditionFlag.BelowEqual;
            case AE:
                return AMD64Assembler.ConditionFlag.AboveEqual;
            case AT:
                return AMD64Assembler.ConditionFlag.Above;
            case BT:
                return AMD64Assembler.ConditionFlag.Below;
            default:
                throw GraalError.shouldNotReachHere();
        }
    }
}
