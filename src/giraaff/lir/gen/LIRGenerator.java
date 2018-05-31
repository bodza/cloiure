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
    private final LIRKindTool lirKindTool;

    private final CodeGenProviders providers;

    private AbstractBlockBase<?> currentBlock;

    private LIRGenerationResult res;

    protected final ArithmeticLIRGenerator arithmeticLIRGen;
    private final MoveFactory moveFactory;

    // @cons
    public LIRGenerator(LIRKindTool lirKindTool, ArithmeticLIRGenerator arithmeticLIRGen, MoveFactory moveFactory, CodeGenProviders providers, LIRGenerationResult res)
    {
        super();
        this.lirKindTool = lirKindTool;
        this.arithmeticLIRGen = arithmeticLIRGen;
        this.res = res;
        this.providers = providers;

        arithmeticLIRGen.lirGen = this;
        this.moveFactory = moveFactory;
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

    private MoveFactory spillMoveFactory;

    @Override
    public MoveFactory getSpillMoveFactory()
    {
        if (spillMoveFactory == null)
        {
            boolean verify = false;
            if (verify)
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
    public LIRKind getValueKind(JavaKind javaKind)
    {
        return LIRKind.fromJavaKind(target().arch, javaKind);
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
    public Variable newVariable(ValueKind<?> valueKind)
    {
        return new Variable(valueKind, ((VariableProvider) res.getLIR()).nextVariable());
    }

    @Override
    public RegisterConfig getRegisterConfig()
    {
        return res.getRegisterConfig();
    }

    @Override
    public RegisterAttributes attributes(Register register)
    {
        return getRegisterConfig().getAttributesMap()[register.number];
    }

    @Override
    public Variable emitMove(Value input)
    {
        Variable result = newVariable(input.getValueKind());
        emitMove(result, input);
        return result;
    }

    @Override
    public void emitMove(AllocatableValue dst, Value src)
    {
        append(moveFactory.createMove(dst, src));
    }

    @Override
    public void emitMoveConstant(AllocatableValue dst, Constant src)
    {
        append(moveFactory.createLoad(dst, src));
    }

    @Override
    public Value emitConstant(LIRKind kind, Constant constant)
    {
        if (moveFactory.canInlineConstant(constant))
        {
            return new ConstantValue(toRegisterKind(kind), constant);
        }
        else
        {
            return emitLoadConstant(toRegisterKind(kind), constant);
        }
    }

    @Override
    public Value emitJavaConstant(JavaConstant constant)
    {
        return emitConstant(getValueKind(constant.getJavaKind()), constant);
    }

    @Override
    public AllocatableValue emitLoadConstant(ValueKind<?> kind, Constant constant)
    {
        Variable result = newVariable(kind);
        emitMoveConstant(result, constant);
        return result;
    }

    @Override
    public AllocatableValue asAllocatable(Value value)
    {
        if (ValueUtil.isAllocatableValue(value))
        {
            return ValueUtil.asAllocatableValue(value);
        }
        else if (LIRValueUtil.isConstantValue(value))
        {
            return emitLoadConstant(value.getValueKind(), LIRValueUtil.asConstant(value));
        }
        else
        {
            return emitMove(value);
        }
    }

    @Override
    public Variable load(Value value)
    {
        if (!LIRValueUtil.isVariable(value))
        {
            return emitMove(value);
        }
        return (Variable) value;
    }

    @Override
    public Value loadNonConst(Value value)
    {
        if (LIRValueUtil.isConstantValue(value) && !moveFactory.canInlineConstant(LIRValueUtil.asConstant(value)))
        {
            return emitMove(value);
        }
        return value;
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
    public AllocatableValue resultOperandFor(JavaKind javaKind, ValueKind<?> valueKind)
    {
        Register reg = getRegisterConfig().getReturnRegister(javaKind);
        return reg.asValue(valueKind);
    }

    @Override
    public <I extends LIRInstruction> I append(I op)
    {
        LIR lir = res.getLIR();
        ArrayList<LIRInstruction> lirForBlock = lir.getLIRforBlock(getCurrentBlock());
        lirForBlock.add(op);
        return op;
    }

    @Override
    public boolean hasBlockEnd(AbstractBlockBase<?> block)
    {
        ArrayList<LIRInstruction> ops = getResult().getLIR().getLIRforBlock(block);
        if (ops.size() == 0)
        {
            return false;
        }
        return ops.get(ops.size() - 1) instanceof BlockEndOp;
    }

    // @class LIRGenerator.BlockScopeImpl
    private final class BlockScopeImpl extends BlockScope
    {
        // @cons
        private BlockScopeImpl(AbstractBlockBase<?> block)
        {
            super();
            currentBlock = block;
        }

        private void doBlockStart()
        {
            // set up the list of LIR instructions
            res.getLIR().setLIRforBlock(currentBlock, new ArrayList<LIRInstruction>());

            append(new LabelOp(new Label(currentBlock.getId()), currentBlock.isAligned()));
        }

        private void doBlockEnd()
        {
            currentBlock = null;
        }

        @Override
        public AbstractBlockBase<?> getCurrentBlock()
        {
            return currentBlock;
        }

        @Override
        public void close()
        {
            doBlockEnd();
        }
    }

    @Override
    public final BlockScope getBlockScope(AbstractBlockBase<?> block)
    {
        BlockScopeImpl blockScope = new BlockScopeImpl(block);
        blockScope.doBlockStart();
        return blockScope;
    }

    @Override
    public void emitIncomingValues(Value[] params)
    {
        ((LabelOp) res.getLIR().getLIRforBlock(getCurrentBlock()).get(0)).setIncomingValues(params);
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
    public Variable emitForeignCall(ForeignCallLinkage linkage, LIRFrameState frameState, Value... args)
    {
        LIRFrameState state = null;
        if (linkage.needsDebugInfo())
        {
            if (frameState != null)
            {
                state = frameState;
            }
            else
            {
                state = LIRFrameState.NO_STATE;
            }
        }

        // move the arguments into the correct location
        CallingConvention linkageCc = linkage.getOutgoingCallingConvention();
        res.getFrameMapBuilder().callsMethod(linkageCc);
        Value[] argLocations = new Value[args.length];
        for (int i = 0; i < args.length; i++)
        {
            Value arg = args[i];
            AllocatableValue loc = linkageCc.getArgument(i);
            emitMove(loc, arg);
            argLocations[i] = loc;
        }
        res.setForeignCall(true);
        emitForeignCallOp(linkage, linkageCc.getReturn(), argLocations, linkage.getTemporaries(), state);

        if (ValueUtil.isLegal(linkageCc.getReturn()))
        {
            return emitMove(linkageCc.getReturn());
        }
        else
        {
            return null;
        }
    }

    @Override
    public void emitStrategySwitch(JavaConstant[] keyConstants, double[] keyProbabilities, LabelRef[] keyTargets, LabelRef defaultTarget, Variable value)
    {
        int keyCount = keyConstants.length;
        SwitchStrategy strategy = SwitchStrategy.getBestStrategy(keyProbabilities, keyConstants, keyTargets);
        long valueRange = keyConstants[keyCount - 1].asLong() - keyConstants[0].asLong() + 1;
        double tableSwitchDensity = keyCount / (double) valueRange;
        /*
         * This heuristic tries to find a compromise between the effort for the best switch strategy
         * and the density of a tableswitch. If the effort for the strategy is at least 4, then a
         * tableswitch is preferred if better than a certain value that starts at 0.5 and lowers
         * gradually with additional effort.
         */
        if (strategy.getAverageEffort() < 4 || tableSwitchDensity < (1 / Math.sqrt(strategy.getAverageEffort())))
        {
            emitStrategySwitch(strategy, value, keyTargets, defaultTarget);
        }
        else
        {
            int minValue = keyConstants[0].asInt();
            LabelRef[] targets = new LabelRef[(int) valueRange];
            for (int i = 0; i < valueRange; i++)
            {
                targets[i] = defaultTarget;
            }
            for (int i = 0; i < keyCount; i++)
            {
                targets[keyConstants[i].asInt() - minValue] = keyTargets[i];
            }
            emitTableSwitch(minValue, defaultTarget, targets, value);
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
    public LIRKind getLIRKind(Stamp stamp)
    {
        return stamp.getLIRKind(lirKindTool);
    }

    protected LIRKind getAddressKind(Value base, long displacement, Value index)
    {
        if (LIRKind.isValue(base) && (index.equals(Value.ILLEGAL) || LIRKind.isValue(index)))
        {
            return LIRKind.value(target().arch.getWordKind());
        }
        else if (base.getValueKind() instanceof LIRKind && base.getValueKind(LIRKind.class).isReference(0) && displacement == 0L && index.equals(Value.ILLEGAL))
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
        return currentBlock;
    }

    @Override
    public LIRGenerationResult getResult()
    {
        return res;
    }

    @Override
    public void emitBlackhole(Value operand)
    {
        append(new StandardOp.BlackholeOp(operand));
    }

    @Override
    public abstract SaveRegistersOp createZapRegisters(Register[] zappedRegisters, JavaConstant[] zapValues);

    @Override
    public SaveRegistersOp createZapRegisters()
    {
        Register[] zappedRegisters = getResult().getFrameMap().getRegisterConfig().getAllocatableRegisters().toArray();
        JavaConstant[] zapValues = new JavaConstant[zappedRegisters.length];
        for (int i = 0; i < zappedRegisters.length; i++)
        {
            PlatformKind kind = target().arch.getLargestStorableKind(zappedRegisters[i].getRegisterCategory());
            zapValues[i] = zapValueForKind(kind);
        }
        return createZapRegisters(zappedRegisters, zapValues);
    }

    @Override
    public abstract LIRInstruction createZapArgumentSpace(StackSlot[] zappedStack, JavaConstant[] zapValues);

    @Override
    public LIRInstruction zapArgumentSpace()
    {
        List<StackSlot> slots = null;
        for (AllocatableValue arg : res.getCallingConvention().getArguments())
        {
            if (ValueUtil.isStackSlot(arg))
            {
                if (slots == null)
                {
                    slots = new ArrayList<>();
                }
                slots.add((StackSlot) arg);
            }
        }
        if (slots == null)
        {
            return null;
        }
        StackSlot[] zappedStack = slots.toArray(new StackSlot[slots.size()]);
        JavaConstant[] zapValues = new JavaConstant[zappedStack.length];
        for (int i = 0; i < zappedStack.length; i++)
        {
            PlatformKind kind = zappedStack[i].getPlatformKind();
            zapValues[i] = zapValueForKind(kind);
        }
        return createZapArgumentSpace(zappedStack, zapValues);
    }
}
