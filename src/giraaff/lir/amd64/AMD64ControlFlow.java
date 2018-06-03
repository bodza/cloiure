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
        protected Value x;

        // @cons
        public ReturnOp(Value __x)
        {
            super(TYPE);
            this.x = __x;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            __crb.frameContext.leave(__crb);
            /*
             * We potentially return to the interpreter, and that's an AVX-SSE transition. The only
             * live value at this point should be the return value in either rax, or in xmm0 with
             * the upper half of the register unused, so we don't destroy any value here.
             */
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
        protected final ConditionFlag condition;
        // @field
        protected final LabelRef trueDestination;
        // @field
        protected final LabelRef falseDestination;

        // @field
        private final double trueDestinationProbability;

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
            this.condition = __condition;
            this.trueDestination = __trueDestination;
            this.falseDestination = __falseDestination;
            this.trueDestinationProbability = __trueDestinationProbability;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            boolean __isNegated = false;
            int __jccPos = __masm.position();
            /*
             * The strategy for emitting jumps is: If either trueDestination or falseDestination is
             * the successor block, assume the block scheduler did the correct thing and jcc to the
             * other. Otherwise, we need a jcc followed by a jmp. Use the branch probability to make
             * sure it is more likely to branch on the jcc (= less likely to execute both the jcc
             * and the jmp instead of just the jcc). In the case of loops, that means the jcc is the
             * back-edge.
             */
            if (__crb.isSuccessorEdge(trueDestination))
            {
                jcc(__masm, true, falseDestination);
                __isNegated = true;
            }
            else if (__crb.isSuccessorEdge(falseDestination))
            {
                jcc(__masm, false, trueDestination);
            }
            else if (trueDestinationProbability < 0.5)
            {
                jcc(__masm, true, falseDestination);
                __masm.jmp(trueDestination.label());
                __isNegated = true;
            }
            else
            {
                jcc(__masm, false, trueDestination);
                __masm.jmp(falseDestination.label());
            }
            __crb.recordBranch(__jccPos, __isNegated);
        }

        protected void jcc(AMD64MacroAssembler __masm, boolean __negate, LabelRef __target)
        {
            __masm.jcc(__negate ? condition.negate() : condition, __target.label());
        }
    }

    // @class AMD64ControlFlow.FloatBranchOp
    public static final class FloatBranchOp extends BranchOp
    {
        // @def
        public static final LIRInstructionClass<FloatBranchOp> TYPE = LIRInstructionClass.create(FloatBranchOp.class);

        // @field
        protected boolean unorderedIsTrue;

        // @cons
        public FloatBranchOp(Condition __condition, boolean __unorderedIsTrue, LabelRef __trueDestination, LabelRef __falseDestination, double __trueDestinationProbability)
        {
            super(TYPE, floatCond(__condition), __trueDestination, __falseDestination, __trueDestinationProbability);
            this.unorderedIsTrue = __unorderedIsTrue;
        }

        @Override
        protected void jcc(AMD64MacroAssembler __masm, boolean __negate, LabelRef __target)
        {
            floatJcc(__masm, __negate ? condition.negate() : condition, __negate ? !unorderedIsTrue : unorderedIsTrue, __target.label());
        }
    }

    // @class AMD64ControlFlow.StrategySwitchOp
    public static class StrategySwitchOp extends AMD64BlockEndOp
    {
        // @def
        public static final LIRInstructionClass<StrategySwitchOp> TYPE = LIRInstructionClass.create(StrategySwitchOp.class);

        // @field
        protected final Constant[] keyConstants;
        // @field
        private final LabelRef[] keyTargets;
        // @field
        private LabelRef defaultTarget;
        @Alive({OperandFlag.REG})
        // @field
        protected Value key;
        @Temp({OperandFlag.REG, OperandFlag.ILLEGAL})
        // @field
        protected Value scratch;
        // @field
        protected final SwitchStrategy strategy;

        // @cons
        public StrategySwitchOp(SwitchStrategy __strategy, LabelRef[] __keyTargets, LabelRef __defaultTarget, Value __key, Value __scratch)
        {
            this(TYPE, __strategy, __keyTargets, __defaultTarget, __key, __scratch);
        }

        // @cons
        protected StrategySwitchOp(LIRInstructionClass<? extends StrategySwitchOp> __c, SwitchStrategy __strategy, LabelRef[] __keyTargets, LabelRef __defaultTarget, Value __key, Value __scratch)
        {
            super(__c);
            this.strategy = __strategy;
            this.keyConstants = __strategy.getKeyConstants();
            this.keyTargets = __keyTargets;
            this.defaultTarget = __defaultTarget;
            this.key = __key;
            this.scratch = __scratch;
        }

        @Override
        public void emitCode(final CompilationResultBuilder __crb, final AMD64MacroAssembler __masm)
        {
            strategy.run(new SwitchClosure(ValueUtil.asRegister(key), __crb, __masm));
        }

        // @class AMD64ControlFlow.StrategySwitchOp.SwitchClosure
        public class SwitchClosure extends BaseSwitchClosure
        {
            // @field
            protected final Register keyRegister;
            // @field
            protected final CompilationResultBuilder crb;
            // @field
            protected final AMD64MacroAssembler masm;

            // @cons
            protected SwitchClosure(Register __keyRegister, CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
            {
                super(__crb, __masm, keyTargets, defaultTarget);
                this.keyRegister = __keyRegister;
                this.crb = __crb;
                this.masm = __masm;
            }

            protected void emitComparison(Constant __c)
            {
                JavaConstant __jc = (JavaConstant) __c;
                switch (__jc.getJavaKind())
                {
                    case Int:
                    {
                        long __lc = __jc.asLong();
                        masm.cmpl(keyRegister, (int) __lc);
                        break;
                    }
                    case Long:
                        masm.cmpq(keyRegister, (AMD64Address) crb.asLongConstRef(__jc));
                        break;
                    case Object:
                        AMD64Move.const2reg(crb, masm, ValueUtil.asRegister(scratch), __jc);
                        masm.cmpptr(keyRegister, ValueUtil.asRegister(scratch));
                        break;
                    default:
                        throw new GraalError("switch only supported for int, long and object");
                }
            }

            @Override
            protected void conditionalJump(int __index, Condition __condition, Label __target)
            {
                emitComparison(keyConstants[__index]);
                masm.jcc(intCond(__condition), __target);
            }
        }
    }

    // @class AMD64ControlFlow.TableSwitchOp
    public static final class TableSwitchOp extends AMD64BlockEndOp
    {
        // @def
        public static final LIRInstructionClass<TableSwitchOp> TYPE = LIRInstructionClass.create(TableSwitchOp.class);

        // @field
        private final int lowKey;
        // @field
        private final LabelRef defaultTarget;
        // @field
        private final LabelRef[] targets;
        @Use
        // @field
        protected Value index;
        @Temp({OperandFlag.REG, OperandFlag.HINT})
        // @field
        protected Value idxScratch;
        @Temp
        // @field
        protected Value scratch;

        // @cons
        public TableSwitchOp(final int __lowKey, final LabelRef __defaultTarget, final LabelRef[] __targets, Value __index, Variable __scratch, Variable __idxScratch)
        {
            super(TYPE);
            this.lowKey = __lowKey;
            this.defaultTarget = __defaultTarget;
            this.targets = __targets;
            this.index = __index;
            this.scratch = __scratch;
            this.idxScratch = __idxScratch;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            Register __indexReg = ValueUtil.asRegister(index, AMD64Kind.DWORD);
            Register __idxScratchReg = ValueUtil.asRegister(idxScratch, AMD64Kind.DWORD);
            Register __scratchReg = ValueUtil.asRegister(scratch, AMD64Kind.QWORD);

            if (!__indexReg.equals(__idxScratchReg))
            {
                __masm.movl(__idxScratchReg, __indexReg);
            }

            // compare index against jump table bounds
            int __highKey = lowKey + targets.length - 1;
            if (lowKey != 0)
            {
                // subtract the low value from the switch value
                __masm.subl(__idxScratchReg, lowKey);
                __masm.cmpl(__idxScratchReg, __highKey - lowKey);
            }
            else
            {
                __masm.cmpl(__idxScratchReg, __highKey);
            }

            // jump to default target if index is not within the jump table
            if (defaultTarget != null)
            {
                __masm.jcc(ConditionFlag.Above, defaultTarget.label());
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
            for (LabelRef __target : targets)
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
        protected Value result;
        // @field
        private final ConditionFlag condition;

        // @cons
        public CondSetOp(Variable __result, Condition __condition)
        {
            super(TYPE);
            this.result = __result;
            this.condition = intCond(__condition);
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            setcc(__masm, result, condition);
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
        protected Value result;
        // @field
        private final ConditionFlag condition;

        // @cons
        public FloatCondSetOp(Variable __result, Condition __condition)
        {
            super(TYPE);
            this.result = __result;
            this.condition = floatCond(__condition);
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            setcc(__masm, result, condition);
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
        protected Value result;
        @Alive({OperandFlag.REG})
        // @field
        protected Value trueValue;
        @Use({OperandFlag.REG, OperandFlag.STACK, OperandFlag.CONST})
        // @field
        protected Value falseValue;
        // @field
        private final ConditionFlag condition;

        // @cons
        public CondMoveOp(Variable __result, Condition __condition, AllocatableValue __trueValue, Value __falseValue)
        {
            super(TYPE);
            this.result = __result;
            this.condition = intCond(__condition);
            this.trueValue = __trueValue;
            this.falseValue = __falseValue;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            cmove(__crb, __masm, result, false, condition, false, trueValue, falseValue);
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
        protected Value result;
        @Alive({OperandFlag.REG})
        // @field
        protected Value trueValue;
        @Alive({OperandFlag.REG})
        // @field
        protected Value falseValue;
        // @field
        private final ConditionFlag condition;
        // @field
        private final boolean unorderedIsTrue;

        // @cons
        public FloatCondMoveOp(Variable __result, Condition __condition, boolean __unorderedIsTrue, Variable __trueValue, Variable __falseValue)
        {
            super(TYPE);
            this.result = __result;
            this.condition = floatCond(__condition);
            this.unorderedIsTrue = __unorderedIsTrue;
            this.trueValue = __trueValue;
            this.falseValue = __falseValue;
        }

        @Override
        public void emitCode(CompilationResultBuilder __crb, AMD64MacroAssembler __masm)
        {
            cmove(__crb, __masm, result, true, condition, unorderedIsTrue, trueValue, falseValue);
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
                    __masm.cmovl(__cond, ValueUtil.asRegister(__result), ValueUtil.asRegister(__other));
                    break;
                case QWORD:
                    __masm.cmovq(__cond, ValueUtil.asRegister(__result), ValueUtil.asRegister(__other));
                    break;
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
                    __masm.cmovl(__cond, ValueUtil.asRegister(__result), __addr);
                    break;
                case QWORD:
                    __masm.cmovq(__cond, ValueUtil.asRegister(__result), __addr);
                    break;
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
                __masm.setl(__cond, ValueUtil.asRegister(__result));
                break;
            case QWORD:
                __masm.setq(__cond, ValueUtil.asRegister(__result));
                break;
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
