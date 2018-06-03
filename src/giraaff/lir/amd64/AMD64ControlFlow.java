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
import giraaff.asm.amd64.AMD64Address.Scale;
import giraaff.asm.amd64.AMD64Assembler.ConditionFlag;
import giraaff.asm.amd64.AMD64MacroAssembler;
import giraaff.core.common.calc.Condition;
import giraaff.lir.LIRInstruction.OperandFlag;
import giraaff.lir.LIRInstructionClass;
import giraaff.lir.LabelRef;
import giraaff.lir.Opcode;
import giraaff.lir.StandardOp;
import giraaff.lir.StandardOp.BlockEndOp;
import giraaff.lir.SwitchStrategy;
import giraaff.lir.SwitchStrategy.BaseSwitchClosure;
import giraaff.lir.Variable;
import giraaff.lir.asm.CompilationResultBuilder;
import giraaff.util.GraalError;

// @class AMD64ControlFlow
public final class AMD64ControlFlow
{
    // @class AMD64ControlFlow.ReturnOp
    public static final class ReturnOp extends AMD64BlockEndOp implements BlockEndOp
    {
        // @def
        public static final LIRInstructionClass<ReturnOp> TYPE = LIRInstructionClass.create(ReturnOp.class);

        @Use({OperandFlag.REG, OperandFlag.ILLEGAL})
        // @field
        protected Value ___x;

        // @cons
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
    public static class BranchOp extends AMD64BlockEndOp implements StandardOp.BranchOp
    {
        // @def
        public static final LIRInstructionClass<BranchOp> TYPE = LIRInstructionClass.create(BranchOp.class);

        // @field
        protected final ConditionFlag ___condition;
        // @field
        protected final LabelRef ___trueDestination;
        // @field
        protected final LabelRef ___falseDestination;

        // @field
        private final double ___trueDestinationProbability;

        // @cons
        public BranchOp(Condition __condition, LabelRef __trueDestination, LabelRef __falseDestination, double __trueDestinationProbability)
        {
            this(intCond(__condition), __trueDestination, __falseDestination, __trueDestinationProbability);
        }

        // @cons
        public BranchOp(ConditionFlag __condition, LabelRef __trueDestination, LabelRef __falseDestination, double __trueDestinationProbability)
        {
            this(TYPE, __condition, __trueDestination, __falseDestination, __trueDestinationProbability);
        }

        // @cons
        protected BranchOp(LIRInstructionClass<? extends BranchOp> __c, ConditionFlag __condition, LabelRef __trueDestination, LabelRef __falseDestination, double __trueDestinationProbability)
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
            // The strategy for emitting jumps is: If either trueDestination or falseDestination is
            // the successor block, assume the block scheduler did the correct thing and jcc to the
            // other. Otherwise, we need a jcc followed by a jmp. Use the branch probability to make
            // sure it is more likely to branch on the jcc (= less likely to execute both the jcc
            // and the jmp instead of just the jcc). In the case of loops, that means the jcc is the
            // back-edge.
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

    // @class AMD64ControlFlow.FloatBranchOp
    public static final class FloatBranchOp extends BranchOp
    {
        // @def
        public static final LIRInstructionClass<FloatBranchOp> TYPE = LIRInstructionClass.create(FloatBranchOp.class);

        // @field
        protected boolean ___unorderedIsTrue;

        // @cons
        public FloatBranchOp(Condition __condition, boolean __unorderedIsTrue, LabelRef __trueDestination, LabelRef __falseDestination, double __trueDestinationProbability)
        {
            super(TYPE, floatCond(__condition), __trueDestination, __falseDestination, __trueDestinationProbability);
            this.___unorderedIsTrue = __unorderedIsTrue;
        }

        @Override
        protected void jcc(AMD64MacroAssembler __masm, boolean __negate, LabelRef __target)
        {
            floatJcc(__masm, __negate ? this.___condition.negate() : this.___condition, __negate ? !this.___unorderedIsTrue : this.___unorderedIsTrue, __target.label());
        }
    }

    // @class AMD64ControlFlow.StrategySwitchOp
    public static class StrategySwitchOp extends AMD64BlockEndOp
    {
        // @def
        public static final LIRInstructionClass<StrategySwitchOp> TYPE = LIRInstructionClass.create(StrategySwitchOp.class);

        // @field
        protected final Constant[] ___keyConstants;
        // @field
        private final LabelRef[] ___keyTargets;
        // @field
        private LabelRef ___defaultTarget;
        @Alive({OperandFlag.REG})
        // @field
        protected Value ___key;
        @Temp({OperandFlag.REG, OperandFlag.ILLEGAL})
        // @field
        protected Value ___scratch;
        // @field
        protected final SwitchStrategy ___strategy;

        // @cons
        public StrategySwitchOp(SwitchStrategy __strategy, LabelRef[] __keyTargets, LabelRef __defaultTarget, Value __key, Value __scratch)
        {
            this(TYPE, __strategy, __keyTargets, __defaultTarget, __key, __scratch);
        }

        // @cons
        protected StrategySwitchOp(LIRInstructionClass<? extends StrategySwitchOp> __c, SwitchStrategy __strategy, LabelRef[] __keyTargets, LabelRef __defaultTarget, Value __key, Value __scratch)
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
            this.___strategy.run(new SwitchClosure(ValueUtil.asRegister(this.___key), __crb, __masm));
        }

        // @class AMD64ControlFlow.StrategySwitchOp.SwitchClosure
        public class SwitchClosure extends BaseSwitchClosure
        {
            // @field
            protected final Register ___keyRegister;
            // @field
            protected final CompilationResultBuilder ___crb;
            // @field
            protected final AMD64MacroAssembler ___masm;

            // @cons
            protected SwitchClosure(Register __keyRegister, CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
            {
                super(__crb, __masm, StrategySwitchOp.this.___keyTargets, StrategySwitchOp.this.___defaultTarget);
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
                        AMD64Move.const2reg(this.___crb, this.___masm, ValueUtil.asRegister(StrategySwitchOp.this.___scratch), __jc);
                        this.___masm.cmpptr(this.___keyRegister, ValueUtil.asRegister(StrategySwitchOp.this.___scratch));
                        break;
                    }
                    default:
                        throw new GraalError("switch only supported for int, long and object");
                }
            }

            @Override
            protected void conditionalJump(int __index, Condition __condition, Label __target)
            {
                emitComparison(StrategySwitchOp.this.___keyConstants[__index]);
                this.___masm.jcc(intCond(__condition), __target);
            }
        }
    }

    // @class AMD64ControlFlow.TableSwitchOp
    public static final class TableSwitchOp extends AMD64BlockEndOp
    {
        // @def
        public static final LIRInstructionClass<TableSwitchOp> TYPE = LIRInstructionClass.create(TableSwitchOp.class);

        // @field
        private final int ___lowKey;
        // @field
        private final LabelRef ___defaultTarget;
        // @field
        private final LabelRef[] ___targets;
        @Use
        // @field
        protected Value ___index;
        @Temp({OperandFlag.REG, OperandFlag.HINT})
        // @field
        protected Value ___idxScratch;
        @Temp
        // @field
        protected Value ___scratch;

        // @cons
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
                __masm.jcc(ConditionFlag.Above, this.___defaultTarget.label());
            }

            // set scratch to address of jump table
            __masm.leaq(__scratchReg, new AMD64Address(AMD64.rip, 0));
            final int __afterLea = __masm.position();

            // load jump table entry into scratch and jump to it
            __masm.movslq(__idxScratchReg, new AMD64Address(__scratchReg, __idxScratchReg, Scale.Times4, 0));
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

    @Opcode
    // @class AMD64ControlFlow.CondSetOp
    public static final class CondSetOp extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<CondSetOp> TYPE = LIRInstructionClass.create(CondSetOp.class);

        @Def({OperandFlag.REG, OperandFlag.HINT})
        // @field
        protected Value ___result;
        // @field
        private final ConditionFlag ___condition;

        // @cons
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

    @Opcode
    // @class AMD64ControlFlow.FloatCondSetOp
    public static final class FloatCondSetOp extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<FloatCondSetOp> TYPE = LIRInstructionClass.create(FloatCondSetOp.class);

        @Def({OperandFlag.REG, OperandFlag.HINT})
        // @field
        protected Value ___result;
        // @field
        private final ConditionFlag ___condition;

        // @cons
        public FloatCondSetOp(Variable __result, Condition __condition)
        {
            super(TYPE);
            this.___result = __result;
            this.___condition = floatCond(__condition);
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            setcc(__masm, this.___result, this.___condition);
        }
    }

    @Opcode
    // @class AMD64ControlFlow.CondMoveOp
    public static final class CondMoveOp extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<CondMoveOp> TYPE = LIRInstructionClass.create(CondMoveOp.class);

        @Def({OperandFlag.REG, OperandFlag.HINT})
        // @field
        protected Value ___result;
        @Alive({OperandFlag.REG})
        // @field
        protected Value ___trueValue;
        @Use({OperandFlag.REG, OperandFlag.STACK, OperandFlag.CONST})
        // @field
        protected Value ___falseValue;
        // @field
        private final ConditionFlag ___condition;

        // @cons
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
            cmove(__crb, __masm, this.___result, false, this.___condition, false, this.___trueValue, this.___falseValue);
        }
    }

    @Opcode
    // @class AMD64ControlFlow.FloatCondMoveOp
    public static final class FloatCondMoveOp extends AMD64LIRInstruction
    {
        // @def
        public static final LIRInstructionClass<FloatCondMoveOp> TYPE = LIRInstructionClass.create(FloatCondMoveOp.class);

        @Def({OperandFlag.REG})
        // @field
        protected Value ___result;
        @Alive({OperandFlag.REG})
        // @field
        protected Value ___trueValue;
        @Alive({OperandFlag.REG})
        // @field
        protected Value ___falseValue;
        // @field
        private final ConditionFlag ___condition;
        // @field
        private final boolean ___unorderedIsTrue;

        // @cons
        public FloatCondMoveOp(Variable __result, Condition __condition, boolean __unorderedIsTrue, Variable __trueValue, Variable __falseValue)
        {
            super(TYPE);
            this.___result = __result;
            this.___condition = floatCond(__condition);
            this.___unorderedIsTrue = __unorderedIsTrue;
            this.___trueValue = __trueValue;
            this.___falseValue = __falseValue;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            cmove(__crb, __masm, this.___result, true, this.___condition, this.___unorderedIsTrue, this.___trueValue, this.___falseValue);
        }
    }

    private static void floatJcc(AMD64MacroAssembler __masm, ConditionFlag __condition, boolean __unorderedIsTrue, Label __label)
    {
        Label __endLabel = new Label();
        if (__unorderedIsTrue && !trueOnUnordered(__condition))
        {
            __masm.jcc(ConditionFlag.Parity, __label);
        }
        else if (!__unorderedIsTrue && trueOnUnordered(__condition))
        {
            __masm.jccb(ConditionFlag.Parity, __endLabel);
        }
        __masm.jcc(__condition, __label);
        __masm.bind(__endLabel);
    }

    private static void cmove(CompilationResultBuilder __crb, AMD64MacroAssembler __masm, Value __result, boolean __isFloat, ConditionFlag __condition, boolean __unorderedIsTrue, Value __trueValue, Value __falseValue)
    {
        AMD64Move.move(__crb, __masm, __result, __falseValue);
        cmove(__crb, __masm, __result, __condition, __trueValue);

        if (__isFloat)
        {
            if (__unorderedIsTrue && !trueOnUnordered(__condition))
            {
                cmove(__crb, __masm, __result, ConditionFlag.Parity, __trueValue);
            }
            else if (!__unorderedIsTrue && trueOnUnordered(__condition))
            {
                cmove(__crb, __masm, __result, ConditionFlag.Parity, __falseValue);
            }
        }
    }

    private static void cmove(CompilationResultBuilder __crb, AMD64MacroAssembler __masm, Value __result, ConditionFlag __cond, Value __other)
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

    private static void setcc(AMD64MacroAssembler __masm, Value __result, ConditionFlag __cond)
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

    private static ConditionFlag intCond(Condition __cond)
    {
        switch (__cond)
        {
            case EQ:
                return ConditionFlag.Equal;
            case NE:
                return ConditionFlag.NotEqual;
            case LT:
                return ConditionFlag.Less;
            case LE:
                return ConditionFlag.LessEqual;
            case GE:
                return ConditionFlag.GreaterEqual;
            case GT:
                return ConditionFlag.Greater;
            case BE:
                return ConditionFlag.BelowEqual;
            case AE:
                return ConditionFlag.AboveEqual;
            case AT:
                return ConditionFlag.Above;
            case BT:
                return ConditionFlag.Below;
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    private static ConditionFlag floatCond(Condition __cond)
    {
        switch (__cond)
        {
            case EQ:
                return ConditionFlag.Equal;
            case NE:
                return ConditionFlag.NotEqual;
            case LT:
                return ConditionFlag.Below;
            case LE:
                return ConditionFlag.BelowEqual;
            case GE:
                return ConditionFlag.AboveEqual;
            case GT:
                return ConditionFlag.Above;
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    public static boolean trueOnUnordered(Condition __condition)
    {
        return trueOnUnordered(floatCond(__condition));
    }

    private static boolean trueOnUnordered(ConditionFlag __condition)
    {
        switch (__condition)
        {
            case AboveEqual:
            case NotEqual:
            case Above:
            case Less:
            case Overflow:
                return false;
            case Equal:
            case BelowEqual:
            case Below:
            case GreaterEqual:
            case NoOverflow:
                return true;
            default:
                throw GraalError.shouldNotReachHere();
        }
    }
}
