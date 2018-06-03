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

/**
 * This class traverses the HIR instructions and generates LIR instructions from them.
 */
// @class LIRGenerator
public abstract class LIRGenerator implements LIRGeneratorTool
{
    // @field
    private final LIRKindTool lirKindTool;

    // @field
    private final CodeGenProviders providers;

    // @field
    private AbstractBlockBase<?> currentBlock;

    // @field
    private LIRGenerationResult res;

    // @field
    protected final ArithmeticLIRGenerator arithmeticLIRGen;
    // @field
    private final MoveFactory moveFactory;

    // @cons
    public LIRGenerator(LIRKindTool __lirKindTool, ArithmeticLIRGenerator __arithmeticLIRGen, MoveFactory __moveFactory, CodeGenProviders __providers, LIRGenerationResult __res)
    {
        super();
        this.lirKindTool = __lirKindTool;
        this.arithmeticLIRGen = __arithmeticLIRGen;
        this.res = __res;
        this.providers = __providers;

        __arithmeticLIRGen.lirGen = this;
        this.moveFactory = __moveFactory;
    }

    @Override
    public ArithmeticLIRGeneratorTool getArithmetic()
    {
        return arithmeticLIRGen;
    }

    @Override
    public MoveFactory getMoveFactory()
    {
        return moveFactory;
    }

    // @field
    private MoveFactory spillMoveFactory;

    @Override
    public MoveFactory getSpillMoveFactory()
    {
        if (spillMoveFactory == null)
        {
            boolean __verify = false;
            if (__verify)
            {
                spillMoveFactory = new VerifyingMoveFactory(moveFactory);
            }
            else
            {
                spillMoveFactory = moveFactory;
            }
        }
        return spillMoveFactory;
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
        return providers;
    }

    @Override
    public MetaAccessProvider getMetaAccess()
    {
        return providers.getMetaAccess();
    }

    @Override
    public CodeCacheProvider getCodeCache()
    {
        return providers.getCodeCache();
    }

    @Override
    public ForeignCallsProvider getForeignCalls()
    {
        return providers.getForeignCalls();
    }

    public LIRKindTool getLIRKindTool()
    {
        return lirKindTool;
    }

    /**
     * Hide {@link #nextVariable()} from other users.
     */
    // @class LIRGenerator.VariableProvider
    public abstract static class VariableProvider
    {
        // @field
        private int numVariables;

        public int numVariables()
        {
            return numVariables;
        }

        private int nextVariable()
        {
            return numVariables++;
        }
    }

    @Override
    public Variable newVariable(ValueKind<?> __valueKind)
    {
        return new Variable(__valueKind, ((VariableProvider) this.res.getLIR()).nextVariable());
    }

    @Override
    public RegisterConfig getRegisterConfig()
    {
        return this.res.getRegisterConfig();
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
        append(moveFactory.createMove(__dst, __src));
    }

    @Override
    public void emitMoveConstant(AllocatableValue __dst, Constant __src)
    {
        append(moveFactory.createLoad(__dst, __src));
    }

    @Override
    public Value emitConstant(LIRKind __kind, Constant __constant)
    {
        if (moveFactory.canInlineConstant(__constant))
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
        if (LIRValueUtil.isConstantValue(__value) && !moveFactory.canInlineConstant(LIRValueUtil.asConstant(__value)))
        {
            return emitMove(__value);
        }
        return __value;
    }

    /**
     * Determines if only oop maps are required for the code generated from the LIR.
     */
    @Override
    public boolean needOnlyOopMaps()
    {
        return false;
    }

    /**
     * Gets the ABI specific operand used to return a value of a given kind from a method.
     *
     * @param javaKind the kind of value being returned
     * @param valueKind the backend type of the value being returned
     * @return the operand representing the ABI defined location used return a value of kind
     *         {@code kind}
     */
    @Override
    public AllocatableValue resultOperandFor(JavaKind __javaKind, ValueKind<?> __valueKind)
    {
        Register __reg = getRegisterConfig().getReturnRegister(__javaKind);
        return __reg.asValue(__valueKind);
    }

    @Override
    public <I extends LIRInstruction> I append(I __op)
    {
        this.res.getLIR().getLIRforBlock(getCurrentBlock()).add(__op);
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
            LIRGenerator.this.currentBlock = __block;
        }

        private void doBlockStart()
        {
            // set up the list of LIR instructions
            LIRGenerator.this.res.getLIR().setLIRforBlock(LIRGenerator.this.currentBlock, new ArrayList<LIRInstruction>());

            LIRGenerator.this.append(new LabelOp(new Label(LIRGenerator.this.currentBlock.getId()), LIRGenerator.this.currentBlock.isAligned()));
        }

        private void doBlockEnd()
        {
            LIRGenerator.this.currentBlock = null;
        }

        @Override
        public AbstractBlockBase<?> getCurrentBlock()
        {
            return LIRGenerator.this.currentBlock;
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
        ((LabelOp) this.res.getLIR().getLIRforBlock(getCurrentBlock()).get(0)).setIncomingValues(__params);
    }

    @Override
    public abstract void emitJump(LabelRef label);

    @Override
    public abstract void emitCompareBranch(PlatformKind cmpKind, Value left, Value right, Condition cond, boolean unorderedIsTrue, LabelRef trueDestination, LabelRef falseDestination, double trueDestinationProbability);

    @Override
    public abstract void emitOverflowCheckBranch(LabelRef overflow, LabelRef noOverflow, LIRKind cmpKind, double overflowProbability);

    @Override
    public abstract void emitIntegerTestBranch(Value left, Value right, LabelRef trueDestination, LabelRef falseDestination, double trueSuccessorProbability);

    @Override
    public abstract Variable emitConditionalMove(PlatformKind cmpKind, Value leftVal, Value right, Condition cond, boolean unorderedIsTrue, Value trueValue, Value falseValue);

    @Override
    public abstract Variable emitIntegerTestMove(Value leftVal, Value right, Value trueValue, Value falseValue);

    /**
     * Emits the single call operation at the heart of generating LIR for a
     * {@linkplain #emitForeignCall(ForeignCallLinkage, LIRFrameState, Value...) foreign call}.
     */
    protected abstract void emitForeignCallOp(ForeignCallLinkage linkage, Value result, Value[] arguments, Value[] temps, LIRFrameState info);

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
        this.res.getFrameMapBuilder().callsMethod(__linkageCc);
        Value[] __argLocations = new Value[__args.length];
        for (int __i = 0; __i < __args.length; __i++)
        {
            Value __arg = __args[__i];
            AllocatableValue __loc = __linkageCc.getArgument(__i);
            emitMove(__loc, __arg);
            __argLocations[__i] = __loc;
        }
        this.res.setForeignCall(true);
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
        /*
         * This heuristic tries to find a compromise between the effort for the best switch strategy
         * and the density of a tableswitch. If the effort for the strategy is at least 4, then a
         * tableswitch is preferred if better than a certain value that starts at 0.5 and lowers
         * gradually with additional effort.
         */
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
    public abstract void emitStrategySwitch(SwitchStrategy strategy, Variable key, LabelRef[] keyTargets, LabelRef defaultTarget);

    protected abstract void emitTableSwitch(int lowKey, LabelRef defaultTarget, LabelRef[] targets, Value key);

    @Override
    public void beforeRegisterAllocation()
    {
    }

    /**
     * Gets a garbage value for a given kind.
     */
    protected abstract JavaConstant zapValueForKind(PlatformKind kind);

    @Override
    public LIRKind getLIRKind(Stamp __stamp)
    {
        return __stamp.getLIRKind(lirKindTool);
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
        return this.currentBlock;
    }

    @Override
    public LIRGenerationResult getResult()
    {
        return this.res;
    }

    @Override
    public void emitBlackhole(Value __operand)
    {
        append(new StandardOp.BlackholeOp(__operand));
    }

    @Override
    public abstract SaveRegistersOp createZapRegisters(Register[] zappedRegisters, JavaConstant[] zapValues);

    @Override
    public SaveRegistersOp createZapRegisters()
    {
        Register[] __zappedRegisters = getResult().getFrameMap().getRegisterConfig().getAllocatableRegisters().toArray();
        JavaConstant[] __zapValues = new JavaConstant[__zappedRegisters.length];
        for (int __i = 0; __i < __zappedRegisters.length; __i++)
        {
            PlatformKind __kind = target().arch.getLargestStorableKind(__zappedRegisters[__i].getRegisterCategory());
            __zapValues[__i] = zapValueForKind(__kind);
        }
        return createZapRegisters(__zappedRegisters, __zapValues);
    }

    @Override
    public abstract LIRInstruction createZapArgumentSpace(StackSlot[] zappedStack, JavaConstant[] zapValues);

    @Override
    public LIRInstruction zapArgumentSpace()
    {
        List<StackSlot> __slots = null;
        for (AllocatableValue __arg : this.res.getCallingConvention().getArguments())
        {
            if (ValueUtil.isStackSlot(__arg))
            {
                if (__slots == null)
                {
                    __slots = new ArrayList<>();
                }
                __slots.add((StackSlot) __arg);
            }
        }
        if (__slots == null)
        {
            return null;
        }
        StackSlot[] __zappedStack = __slots.toArray(new StackSlot[__slots.size()]);
        JavaConstant[] __zapValues = new JavaConstant[__zappedStack.length];
        for (int __i = 0; __i < __zappedStack.length; __i++)
        {
            PlatformKind __kind = __zappedStack[__i].getPlatformKind();
            __zapValues[__i] = zapValueForKind(__kind);
        }
        return createZapArgumentSpace(__zappedStack, __zapValues);
    }
}
