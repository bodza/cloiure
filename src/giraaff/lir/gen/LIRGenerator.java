package giraaff.lir.gen;

import java.util.ArrayList;
import java.util.List;

import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.code.CodeCacheProvider;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterAttributes;
import jdk.vm.ci.code.RegisterConfig;
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.PlatformKind;
import jdk.vm.ci.meta.Value;
import jdk.vm.ci.meta.ValueKind;

import giraaff.asm.Label;
import giraaff.core.common.LIRKind;
import giraaff.core.common.calc.Condition;
import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.core.common.spi.CodeGenProviders;
import giraaff.core.common.spi.ForeignCallLinkage;
import giraaff.core.common.spi.ForeignCallsProvider;
import giraaff.core.common.spi.LIRKindTool;
import giraaff.core.common.type.Stamp;
import giraaff.lir.ConstantValue;
import giraaff.lir.LIR;
import giraaff.lir.LIRFrameState;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRValueUtil;
import giraaff.lir.LabelRef;
import giraaff.lir.StandardOp;
import giraaff.lir.StandardOp.BlockEndOp;
import giraaff.lir.StandardOp.LabelOp;
import giraaff.lir.StandardOp.SaveRegistersOp;
import giraaff.lir.SwitchStrategy;
import giraaff.lir.Variable;

///
// This class traverses the HIR instructions and generates LIR instructions from them.
///
// @class LIRGenerator
public abstract class LIRGenerator implements LIRGeneratorTool
{
    // @field
    private final LIRKindTool ___lirKindTool;

    // @field
    private final CodeGenProviders ___providers;

    // @field
    private AbstractBlockBase<?> ___currentBlock;

    // @field
    private LIRGenerationResult ___res;

    // @field
    protected final ArithmeticLIRGenerator ___arithmeticLIRGen;
    // @field
    private final MoveFactory ___moveFactory;

    // @cons
    public LIRGenerator(LIRKindTool __lirKindTool, ArithmeticLIRGenerator __arithmeticLIRGen, MoveFactory __moveFactory, CodeGenProviders __providers, LIRGenerationResult __res)
    {
        super();
        this.___lirKindTool = __lirKindTool;
        this.___arithmeticLIRGen = __arithmeticLIRGen;
        this.___res = __res;
        this.___providers = __providers;

        __arithmeticLIRGen.___lirGen = this;
        this.___moveFactory = __moveFactory;
    }

    @Override
    public ArithmeticLIRGeneratorTool getArithmetic()
    {
        return this.___arithmeticLIRGen;
    }

    @Override
    public MoveFactory getMoveFactory()
    {
        return this.___moveFactory;
    }

    // @field
    private MoveFactory ___spillMoveFactory;

    @Override
    public MoveFactory getSpillMoveFactory()
    {
        if (this.___spillMoveFactory == null)
        {
            boolean __verify = false;
            if (__verify)
            {
                this.___spillMoveFactory = new VerifyingMoveFactory(this.___moveFactory);
            }
            else
            {
                this.___spillMoveFactory = this.___moveFactory;
            }
        }
        return this.___spillMoveFactory;
    }

    @Override
    public LIRKind getValueKind(JavaKind __javaKind)
    {
        return LIRKind.fromJavaKind(target().arch, __javaKind);
    }

    @Override
    public TargetDescription target()
    {
        return getCodeCache().getTarget();
    }

    @Override
    public CodeGenProviders getProviders()
    {
        return this.___providers;
    }

    @Override
    public MetaAccessProvider getMetaAccess()
    {
        return this.___providers.getMetaAccess();
    }

    @Override
    public CodeCacheProvider getCodeCache()
    {
        return this.___providers.getCodeCache();
    }

    @Override
    public ForeignCallsProvider getForeignCalls()
    {
        return this.___providers.getForeignCalls();
    }

    public LIRKindTool getLIRKindTool()
    {
        return this.___lirKindTool;
    }

    ///
    // Hide {@link #nextVariable()} from other users.
    ///
    // @class LIRGenerator.VariableProvider
    public abstract static class VariableProvider
    {
        // @field
        private int ___numVariables;

        public int numVariables()
        {
            return this.___numVariables;
        }

        private int nextVariable()
        {
            return this.___numVariables++;
        }
    }

    @Override
    public Variable newVariable(ValueKind<?> __valueKind)
    {
        return new Variable(__valueKind, ((VariableProvider) this.___res.getLIR()).nextVariable());
    }

    @Override
    public RegisterConfig getRegisterConfig()
    {
        return this.___res.getRegisterConfig();
    }

    @Override
    public RegisterAttributes attributes(Register __register)
    {
        return getRegisterConfig().getAttributesMap()[__register.number];
    }

    @Override
    public Variable emitMove(Value __input)
    {
        Variable __result = newVariable(__input.getValueKind());
        emitMove(__result, __input);
        return __result;
    }

    @Override
    public void emitMove(AllocatableValue __dst, Value __src)
    {
        append(this.___moveFactory.createMove(__dst, __src));
    }

    @Override
    public void emitMoveConstant(AllocatableValue __dst, Constant __src)
    {
        append(this.___moveFactory.createLoad(__dst, __src));
    }

    @Override
    public Value emitConstant(LIRKind __kind, Constant __constant)
    {
        if (this.___moveFactory.canInlineConstant(__constant))
        {
            return new ConstantValue(toRegisterKind(__kind), __constant);
        }
        else
        {
            return emitLoadConstant(toRegisterKind(__kind), __constant);
        }
    }

    @Override
    public Value emitJavaConstant(JavaConstant __constant)
    {
        return emitConstant(getValueKind(__constant.getJavaKind()), __constant);
    }

    @Override
    public AllocatableValue emitLoadConstant(ValueKind<?> __kind, Constant __constant)
    {
        Variable __result = newVariable(__kind);
        emitMoveConstant(__result, __constant);
        return __result;
    }

    @Override
    public AllocatableValue asAllocatable(Value __value)
    {
        if (ValueUtil.isAllocatableValue(__value))
        {
            return ValueUtil.asAllocatableValue(__value);
        }
        else if (LIRValueUtil.isConstantValue(__value))
        {
            return emitLoadConstant(__value.getValueKind(), LIRValueUtil.asConstant(__value));
        }
        else
        {
            return emitMove(__value);
        }
    }

    @Override
    public Variable load(Value __value)
    {
        if (!LIRValueUtil.isVariable(__value))
        {
            return emitMove(__value);
        }
        return (Variable) __value;
    }

    @Override
    public Value loadNonConst(Value __value)
    {
        if (LIRValueUtil.isConstantValue(__value) && !this.___moveFactory.canInlineConstant(LIRValueUtil.asConstant(__value)))
        {
            return emitMove(__value);
        }
        return __value;
    }

    ///
    // Determines if only oop maps are required for the code generated from the LIR.
    ///
    @Override
    public boolean needOnlyOopMaps()
    {
        return false;
    }

    ///
    // Gets the ABI specific operand used to return a value of a given kind from a method.
    //
    // @param javaKind the kind of value being returned
    // @param valueKind the backend type of the value being returned
    // @return the operand representing the ABI defined location used return a value of kind
    //         {@code kind}
    ///
    @Override
    public AllocatableValue resultOperandFor(JavaKind __javaKind, ValueKind<?> __valueKind)
    {
        Register __reg = getRegisterConfig().getReturnRegister(__javaKind);
        return __reg.asValue(__valueKind);
    }

    @Override
    public <I extends LIRInstruction> I append(I __op)
    {
        this.___res.getLIR().getLIRforBlock(getCurrentBlock()).add(__op);
        return __op;
    }

    @Override
    public boolean hasBlockEnd(AbstractBlockBase<?> __block)
    {
        ArrayList<LIRInstruction> __ops = getResult().getLIR().getLIRforBlock(__block);
        if (__ops.size() == 0)
        {
            return false;
        }
        return __ops.get(__ops.size() - 1) instanceof BlockEndOp;
    }

    // @class LIRGenerator.BlockScopeImpl
    // @closure
    private final class BlockScopeImpl implements BlockScope
    {
        // @cons
        private BlockScopeImpl(AbstractBlockBase<?> __block)
        {
            super();
            LIRGenerator.this.___currentBlock = __block;
        }

        private void doBlockStart()
        {
            // set up the list of LIR instructions
            LIRGenerator.this.___res.getLIR().setLIRforBlock(LIRGenerator.this.___currentBlock, new ArrayList<LIRInstruction>());

            LIRGenerator.this.append(new LabelOp(new Label(LIRGenerator.this.___currentBlock.getId()), LIRGenerator.this.___currentBlock.isAligned()));
        }

        private void doBlockEnd()
        {
            LIRGenerator.this.___currentBlock = null;
        }

        @Override
        public AbstractBlockBase<?> getCurrentBlock()
        {
            return LIRGenerator.this.___currentBlock;
        }

        @Override
        public void close()
        {
            doBlockEnd();
        }
    }

    @Override
    public final BlockScope getBlockScope(AbstractBlockBase<?> __block)
    {
        BlockScopeImpl __blockScope = new BlockScopeImpl(__block);
        __blockScope.doBlockStart();
        return __blockScope;
    }

    @Override
    public void emitIncomingValues(Value[] __params)
    {
        ((LabelOp) this.___res.getLIR().getLIRforBlock(getCurrentBlock()).get(0)).setIncomingValues(__params);
    }

    @Override
    public abstract void emitJump(LabelRef __label);

    @Override
    public abstract void emitCompareBranch(PlatformKind __cmpKind, Value __left, Value __right, Condition __cond, LabelRef __trueDestination, LabelRef __falseDestination, double __trueDestinationProbability);

    @Override
    public abstract void emitOverflowCheckBranch(LabelRef __overflow, LabelRef __noOverflow, LIRKind __cmpKind, double __overflowProbability);

    @Override
    public abstract void emitIntegerTestBranch(Value __left, Value __right, LabelRef __trueDestination, LabelRef __falseDestination, double __trueSuccessorProbability);

    @Override
    public abstract Variable emitConditionalMove(PlatformKind __cmpKind, Value __leftVal, Value __right, Condition __cond, Value __trueValue, Value __falseValue);

    @Override
    public abstract Variable emitIntegerTestMove(Value __leftVal, Value __right, Value __trueValue, Value __falseValue);

    ///
    // Emits the single call operation at the heart of generating LIR for a
    // {@linkplain #emitForeignCall(ForeignCallLinkage, LIRFrameState, Value...) foreign call}.
    ///
    protected abstract void emitForeignCallOp(ForeignCallLinkage __linkage, Value __result, Value[] __arguments, Value[] __temps, LIRFrameState __info);

    @Override
    public Variable emitForeignCall(ForeignCallLinkage __linkage, LIRFrameState __frameState, Value... __args)
    {
        LIRFrameState __state = null;
        if (__linkage.needsDebugInfo())
        {
            if (__frameState != null)
            {
                __state = __frameState;
            }
            else
            {
                __state = LIRFrameState.NO_STATE;
            }
        }

        // move the arguments into the correct location
        CallingConvention __linkageCc = __linkage.getOutgoingCallingConvention();
        this.___res.getFrameMapBuilder().callsMethod(__linkageCc);
        Value[] __argLocations = new Value[__args.length];
        for (int __i = 0; __i < __args.length; __i++)
        {
            Value __arg = __args[__i];
            AllocatableValue __loc = __linkageCc.getArgument(__i);
            emitMove(__loc, __arg);
            __argLocations[__i] = __loc;
        }
        this.___res.setForeignCall(true);
        emitForeignCallOp(__linkage, __linkageCc.getReturn(), __argLocations, __linkage.getTemporaries(), __state);

        if (ValueUtil.isLegal(__linkageCc.getReturn()))
        {
            return emitMove(__linkageCc.getReturn());
        }
        else
        {
            return null;
        }
    }

    @Override
    public void emitStrategySwitch(JavaConstant[] __keyConstants, double[] __keyProbabilities, LabelRef[] __keyTargets, LabelRef __defaultTarget, Variable __value)
    {
        int __keyCount = __keyConstants.length;
        SwitchStrategy __strategy = SwitchStrategy.getBestStrategy(__keyProbabilities, __keyConstants, __keyTargets);
        long __valueRange = __keyConstants[__keyCount - 1].asLong() - __keyConstants[0].asLong() + 1;
        double __tableSwitchDensity = __keyCount / (double) __valueRange;
        // This heuristic tries to find a compromise between the effort for the best switch strategy
        // and the density of a tableswitch. If the effort for the strategy is at least 4, then a
        // tableswitch is preferred if better than a certain value that starts at 0.5 and lowers
        // gradually with additional effort.
        if (__strategy.getAverageEffort() < 4 || __tableSwitchDensity < (1 / Math.sqrt(__strategy.getAverageEffort())))
        {
            emitStrategySwitch(__strategy, __value, __keyTargets, __defaultTarget);
        }
        else
        {
            int __minValue = __keyConstants[0].asInt();
            LabelRef[] __targets = new LabelRef[(int) __valueRange];
            for (int __i = 0; __i < __valueRange; __i++)
            {
                __targets[__i] = __defaultTarget;
            }
            for (int __i = 0; __i < __keyCount; __i++)
            {
                __targets[__keyConstants[__i].asInt() - __minValue] = __keyTargets[__i];
            }
            emitTableSwitch(__minValue, __defaultTarget, __targets, __value);
        }
    }

    @Override
    public abstract void emitStrategySwitch(SwitchStrategy __strategy, Variable __key, LabelRef[] __keyTargets, LabelRef __defaultTarget);

    protected abstract void emitTableSwitch(int __lowKey, LabelRef __defaultTarget, LabelRef[] __targets, Value __key);

    @Override
    public void beforeRegisterAllocation()
    {
    }

    @Override
    public LIRKind getLIRKind(Stamp __stamp)
    {
        return __stamp.getLIRKind(this.___lirKindTool);
    }

    protected LIRKind getAddressKind(Value __base, long __displacement, Value __index)
    {
        if (LIRKind.isValue(__base) && (__index.equals(Value.ILLEGAL) || LIRKind.isValue(__index)))
        {
            return LIRKind.value(target().arch.getWordKind());
        }
        else if (__base.getValueKind() instanceof LIRKind && __base.getValueKind(LIRKind.class).isReference(0) && __displacement == 0L && __index.equals(Value.ILLEGAL))
        {
            return LIRKind.reference(target().arch.getWordKind());
        }
        else
        {
            return LIRKind.unknownReference(target().arch.getWordKind());
        }
    }

    @Override
    public AbstractBlockBase<?> getCurrentBlock()
    {
        return this.___currentBlock;
    }

    @Override
    public LIRGenerationResult getResult()
    {
        return this.___res;
    }

    @Override
    public void emitBlackhole(Value __operand)
    {
        append(new StandardOp.BlackholeOp(__operand));
    }
}
