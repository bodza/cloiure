package giraaff.core.gen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.PlatformKind;
import jdk.vm.ci.meta.Value;

import org.graalvm.collections.UnmodifiableMapCursor;

import giraaff.core.common.LIRKind;
import giraaff.core.common.calc.Condition;
import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.core.common.cfg.BlockMap;
import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.NodeMap;
import giraaff.graph.iterators.NodeIterable;
import giraaff.lir.LIRFrameState;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LabelRef;
import giraaff.lir.StandardOp.JumpOp;
import giraaff.lir.StandardOp.LabelOp;
import giraaff.lir.SwitchStrategy;
import giraaff.lir.Variable;
import giraaff.lir.framemap.FrameMapBuilder;
import giraaff.lir.gen.LIRGenerator;
import giraaff.lir.gen.LIRGeneratorTool;
import giraaff.lir.gen.LIRGeneratorTool.BlockScope;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.AbstractEndNode;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.DeoptimizingNode;
import giraaff.nodes.DirectCallTargetNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FrameState;
import giraaff.nodes.IfNode;
import giraaff.nodes.IndirectCallTargetNode;
import giraaff.nodes.Invoke;
import giraaff.nodes.InvokeWithExceptionNode;
import giraaff.nodes.LogicConstantNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.LoopEndNode;
import giraaff.nodes.LoweredCallTargetNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ParameterNode;
import giraaff.nodes.PhiNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.ValuePhiNode;
import giraaff.nodes.calc.CompareNode;
import giraaff.nodes.calc.ConditionalNode;
import giraaff.nodes.calc.IntegerTestNode;
import giraaff.nodes.calc.IsNullNode;
import giraaff.nodes.cfg.Block;
import giraaff.nodes.cfg.ControlFlowGraph;
import giraaff.nodes.extended.IntegerSwitchNode;
import giraaff.nodes.extended.SwitchNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.util.GraalError;

///
// This class traverses the HIR instructions and generates LIR instructions from them.
///
// @class NodeLIRBuilder
public abstract class NodeLIRBuilder implements NodeLIRBuilderTool
{
    // @field
    private final NodeMap<Value> ___nodeOperands;
    // @field
    private final LockStackHolder ___lockStackHolder;

    // @field
    protected final LIRGenerator ___gen;

    // @field
    private ValueNode ___currentInstruction;

    // @cons
    public NodeLIRBuilder(StructuredGraph __graph, LIRGeneratorTool __gen)
    {
        super();
        this.___gen = (LIRGenerator) __gen;
        this.___nodeOperands = __graph.createNodeMap();
        this.___lockStackHolder = createLockStackHolder();
    }

    protected abstract LockStackHolder createLockStackHolder();

    ///
    // Returns the operand that has been previously initialized by {@link #setResult(ValueNode, Value)} with the result
    // of an instruction. It's a code generation error to ask for the operand of ValueNode that doesn't have one yet.
    //
    // @param node A node that produces a result value.
    ///
    @Override
    public Value operand(Node __node)
    {
        return getOperand(__node);
    }

    @Override
    public boolean hasOperand(Node __node)
    {
        return getOperand(__node) != null;
    }

    private Value getOperand(Node __node)
    {
        if (this.___nodeOperands == null)
        {
            return null;
        }
        return this.___nodeOperands.get(__node);
    }

    @Override
    public ValueNode valueForOperand(Value __value)
    {
        UnmodifiableMapCursor<Node, Value> __cursor = this.___nodeOperands.getEntries();
        while (__cursor.advance())
        {
            if (__cursor.getValue().equals(__value))
            {
                return (ValueNode) __cursor.getKey();
            }
        }
        return null;
    }

    @Override
    public Value setResult(ValueNode __x, Value __operand)
    {
        this.___nodeOperands.set(__x, __operand);
        return __operand;
    }

    public LabelRef getLIRBlock(FixedNode __b)
    {
        Block __result = ((ControlFlowGraph) this.___gen.getResult().getLIR().getControlFlowGraph()).blockFor(__b);
        int __suxIndex = 0;
        for (AbstractBlockBase<?> __succ : this.___gen.getCurrentBlock().getSuccessors())
        {
            if (__succ == __result)
            {
                return LabelRef.forSuccessor(this.___gen.getResult().getLIR(), this.___gen.getCurrentBlock(), __suxIndex);
            }
            __suxIndex++;
        }
        throw GraalError.shouldNotReachHere("Block not in successor list of current block");
    }

    public final void append(LIRInstruction __op)
    {
        this.___gen.append(__op);
    }

    protected LIRKind getExactPhiKind(PhiNode __phi)
    {
        LIRKind __derivedKind = this.___gen.toRegisterKind(this.___gen.getLIRKind(__phi.stamp(NodeView.DEFAULT)));
        // Collect reference information.
        for (int __i = 0; __i < __phi.valueCount() && !__derivedKind.isUnknownReference(); __i++)
        {
            ValueNode __node = __phi.valueAt(__i);
            Value __value = getOperand(__node);

            // get ValueKind for input
            final LIRKind __valueKind;
            if (__value != null)
            {
                __valueKind = __value.getValueKind(LIRKind.class);
            }
            else
            {
                LIRKind __kind = this.___gen.getLIRKind(__node.stamp(NodeView.DEFAULT));
                __valueKind = this.___gen.toRegisterKind(__kind);
            }
            // Merge the reference information of the derived kind and the input.
            __derivedKind = LIRKind.mergeReferenceInformation(__derivedKind, __valueKind);
        }
        return __derivedKind;
    }

    private static boolean isPhiInputFromBackedge(PhiNode __phi, int __index)
    {
        AbstractMergeNode __merge = __phi.merge();
        AbstractEndNode __end = __merge.phiPredecessorAt(__index);
        return __end instanceof LoopEndNode && ((LoopEndNode) __end).loopBegin().equals(__merge);
    }

    private Value[] createPhiIn(AbstractMergeNode __merge)
    {
        List<Value> __values = new ArrayList<>();
        for (ValuePhiNode __phi : __merge.valuePhis())
        {
            Variable __value = this.___gen.newVariable(getExactPhiKind(__phi));
            __values.add(__value);
            setResult(__phi, __value);
        }
        return __values.toArray(new Value[__values.size()]);
    }

    ///
    // @return {@code true} if object constant to stack moves are supported.
    ///
    protected boolean allowObjectConstantToStackMove()
    {
        return true;
    }

    private Value[] createPhiOut(AbstractMergeNode __merge, AbstractEndNode __pred)
    {
        List<Value> __values = new ArrayList<>();
        for (PhiNode __phi : __merge.valuePhis())
        {
            ValueNode __node = __phi.valueAt(__pred);
            Value __value = operand(__node);
            if (ValueUtil.isRegister(__value))
            {
                // Fixed register intervals are not allowed at block boundaries so we introduce a new Variable.
                __value = this.___gen.emitMove(__value);
            }
            else if (!allowObjectConstantToStackMove() && __node instanceof ConstantNode && !LIRKind.isValue(__value))
            {
                // Some constants are not allowed as inputs for PHIs in certain backends. Explicitly create
                // a copy of this value to force it into a register. The new variable is only used in the PHI.
                Variable __result = this.___gen.newVariable(__value.getValueKind());
                this.___gen.emitMove(__result, __value);
                __value = __result;
            }
            __values.add(__value);
        }
        return __values.toArray(new Value[__values.size()]);
    }

    public void doBlockPrologue(@SuppressWarnings("unused") Block __block)
    {
    }

    @Override
    @SuppressWarnings("try")
    public void doBlock(Block __block, StructuredGraph __graph, BlockMap<List<Node>> __blockMap)
    {
        try (BlockScope __blockScope = this.___gen.getBlockScope(__block))
        {
            if (__block == this.___gen.getResult().getLIR().getControlFlowGraph().getStartBlock())
            {
                emitPrologue(__graph);
            }
            else
            {
                // create phi-in value array
                AbstractBeginNode __begin = __block.getBeginNode();
                if (__begin instanceof AbstractMergeNode)
                {
                    AbstractMergeNode __merge = (AbstractMergeNode) __begin;
                    LabelOp __label = (LabelOp) this.___gen.getResult().getLIR().getLIRforBlock(__block).get(0);
                    __label.setPhiValues(createPhiIn(__merge));
                }
            }
            doBlockPrologue(__block);

            List<Node> __nodes = __blockMap.get(__block);

            for (int __i = 0; __i < __nodes.size(); __i++)
            {
                Node __node = __nodes.get(__i);
                if (__node instanceof ValueNode)
                {
                    ValueNode __valueNode = (ValueNode) __node;
                    Value __operand = getOperand(__valueNode);
                    if (__operand == null)
                    {
                        if (!peephole(__valueNode))
                        {
                            try
                            {
                                doRoot(__valueNode);
                            }
                            catch (GraalError __e)
                            {
                                throw __e;
                            }
                            catch (Throwable __t)
                            {
                                throw new GraalError(__t);
                            }
                        }
                    }
                    else
                    {
                        // There can be cases in which the result of an instruction is already set before by other instructions.
                    }
                }
            }

            if (!this.___gen.hasBlockEnd(__block))
            {
                NodeIterable<Node> __successors = __block.getEndNode().successors();
                if (__block.getSuccessorCount() != 1)
                {
                    // If we have more than one successor, we cannot just use the first one. Since
                    // successors are unordered, this would be a random choice.
                    throw new GraalError("Block without BlockEndOp: " + __block.getEndNode());
                }
                this.___gen.emitJump(getLIRBlock((FixedNode) __successors.first()));
            }
        }
    }

    protected abstract boolean peephole(ValueNode __valueNode);

    private void doRoot(ValueNode __instr)
    {
        this.___currentInstruction = __instr;
        emitNode(__instr);
    }

    protected void emitNode(ValueNode __node)
    {
        if (__node instanceof LIRLowerable)
        {
            ((LIRLowerable) __node).generate(this);
        }
        else
        {
            throw GraalError.shouldNotReachHere("node is not LIRLowerable: " + __node);
        }
    }

    protected void emitPrologue(StructuredGraph __graph)
    {
        CallingConvention __incomingArguments = this.___gen.getResult().getCallingConvention();

        Value[] __params = new Value[__incomingArguments.getArgumentCount()];
        for (int __i = 0; __i < __params.length; __i++)
        {
            __params[__i] = __incomingArguments.getArgument(__i);
            if (ValueUtil.isStackSlot(__params[__i]))
            {
                StackSlot __slot = ValueUtil.asStackSlot(__params[__i]);
                if (__slot.isInCallerFrame() && !this.___gen.getResult().getLIR().hasArgInCallerFrame())
                {
                    this.___gen.getResult().getLIR().setHasArgInCallerFrame();
                }
            }
        }

        this.___gen.emitIncomingValues(__params);

        for (ParameterNode __param : __graph.getNodes(ParameterNode.TYPE))
        {
            Value __paramValue = __params[__param.index()];
            setResult(__param, this.___gen.emitMove(__paramValue));
        }
    }

    @Override
    public void visitMerge(AbstractMergeNode __x)
    {
    }

    @Override
    public void visitEndNode(AbstractEndNode __end)
    {
        AbstractMergeNode __merge = __end.merge();
        JumpOp __jump = newJumpOp(getLIRBlock(__merge));
        __jump.setPhiValues(createPhiOut(__merge, __end));
        append(__jump);
    }

    ///
    // Runtime specific classes can override this to insert a safepoint at the end of a loop.
    ///
    @Override
    public void visitLoopEnd(LoopEndNode __x)
    {
    }

    protected JumpOp newJumpOp(LabelRef __ref)
    {
        return new JumpOp(__ref);
    }

    protected LIRKind getPhiKind(PhiNode __phi)
    {
        return this.___gen.getLIRKind(__phi.stamp(NodeView.DEFAULT));
    }

    @Override
    public void emitIf(IfNode __x)
    {
        emitBranch(__x.condition(), getLIRBlock(__x.trueSuccessor()), getLIRBlock(__x.falseSuccessor()), __x.probability(__x.trueSuccessor()));
    }

    public void emitBranch(LogicNode __node, LabelRef __trueSuccessor, LabelRef __falseSuccessor, double __trueSuccessorProbability)
    {
        if (__node instanceof IsNullNode)
        {
            emitNullCheckBranch((IsNullNode) __node, __trueSuccessor, __falseSuccessor, __trueSuccessorProbability);
        }
        else if (__node instanceof CompareNode)
        {
            emitCompareBranch((CompareNode) __node, __trueSuccessor, __falseSuccessor, __trueSuccessorProbability);
        }
        else if (__node instanceof LogicConstantNode)
        {
            emitConstantBranch(((LogicConstantNode) __node).getValue(), __trueSuccessor, __falseSuccessor);
        }
        else if (__node instanceof IntegerTestNode)
        {
            emitIntegerTestBranch((IntegerTestNode) __node, __trueSuccessor, __falseSuccessor, __trueSuccessorProbability);
        }
        else
        {
            throw GraalError.unimplemented();
        }
    }

    private void emitNullCheckBranch(IsNullNode __node, LabelRef __trueSuccessor, LabelRef __falseSuccessor, double __trueSuccessorProbability)
    {
        LIRKind __kind = this.___gen.getLIRKind(__node.getValue().stamp(NodeView.DEFAULT));
        Value __nullValue = this.___gen.emitConstant(__kind, JavaConstant.NULL_POINTER);
        this.___gen.emitCompareBranch(__kind.getPlatformKind(), operand(__node.getValue()), __nullValue, Condition.EQ, __trueSuccessor, __falseSuccessor, __trueSuccessorProbability);
    }

    public void emitCompareBranch(CompareNode __compare, LabelRef __trueSuccessor, LabelRef __falseSuccessor, double __trueSuccessorProbability)
    {
        PlatformKind __kind = this.___gen.getLIRKind(__compare.getX().stamp(NodeView.DEFAULT)).getPlatformKind();
        this.___gen.emitCompareBranch(__kind, operand(__compare.getX()), operand(__compare.getY()), __compare.condition().asCondition(), __trueSuccessor, __falseSuccessor, __trueSuccessorProbability);
    }

    public void emitIntegerTestBranch(IntegerTestNode __test, LabelRef __trueSuccessor, LabelRef __falseSuccessor, double __trueSuccessorProbability)
    {
        this.___gen.emitIntegerTestBranch(operand(__test.getX()), operand(__test.getY()), __trueSuccessor, __falseSuccessor, __trueSuccessorProbability);
    }

    public void emitConstantBranch(boolean __value, LabelRef __trueSuccessorBlock, LabelRef __falseSuccessorBlock)
    {
        LabelRef __block = __value ? __trueSuccessorBlock : __falseSuccessorBlock;
        this.___gen.emitJump(__block);
    }

    @Override
    public void emitConditional(ConditionalNode __conditional)
    {
        Value __tVal = operand(__conditional.trueValue());
        Value __fVal = operand(__conditional.falseValue());
        setResult(__conditional, emitConditional(__conditional.condition(), __tVal, __fVal));
    }

    public Variable emitConditional(LogicNode __node, Value __trueValue, Value __falseValue)
    {
        if (__node instanceof IsNullNode)
        {
            IsNullNode __isNullNode = (IsNullNode) __node;
            LIRKind __kind = this.___gen.getLIRKind(__isNullNode.getValue().stamp(NodeView.DEFAULT));
            Value __nullValue = this.___gen.emitConstant(__kind, JavaConstant.NULL_POINTER);
            return this.___gen.emitConditionalMove(__kind.getPlatformKind(), operand(__isNullNode.getValue()), __nullValue, Condition.EQ, __trueValue, __falseValue);
        }
        else if (__node instanceof CompareNode)
        {
            CompareNode __compare = (CompareNode) __node;
            PlatformKind __kind = this.___gen.getLIRKind(__compare.getX().stamp(NodeView.DEFAULT)).getPlatformKind();
            return this.___gen.emitConditionalMove(__kind, operand(__compare.getX()), operand(__compare.getY()), __compare.condition().asCondition(), __trueValue, __falseValue);
        }
        else if (__node instanceof LogicConstantNode)
        {
            return this.___gen.emitMove(((LogicConstantNode) __node).getValue() ? __trueValue : __falseValue);
        }
        else if (__node instanceof IntegerTestNode)
        {
            IntegerTestNode __test = (IntegerTestNode) __node;
            return this.___gen.emitIntegerTestMove(operand(__test.getX()), operand(__test.getY()), __trueValue, __falseValue);
        }
        else
        {
            throw GraalError.unimplemented();
        }
    }

    @Override
    public void emitInvoke(Invoke __x)
    {
        LoweredCallTargetNode __callTarget = (LoweredCallTargetNode) __x.callTarget();
        FrameMapBuilder __frameMapBuilder = this.___gen.getResult().getFrameMapBuilder();
        CallingConvention __invokeCc = __frameMapBuilder.getRegisterConfig().getCallingConvention(__callTarget.callType(), __x.asNode().stamp(NodeView.DEFAULT).javaType(this.___gen.getMetaAccess()), __callTarget.signature(), this.___gen);
        __frameMapBuilder.callsMethod(__invokeCc);

        Value[] __parameters = visitInvokeArguments(__invokeCc, __callTarget.arguments());

        LabelRef __exceptionEdge = null;
        if (__x instanceof InvokeWithExceptionNode)
        {
            __exceptionEdge = getLIRBlock(((InvokeWithExceptionNode) __x).exceptionEdge());
        }
        LIRFrameState __callState = stateWithExceptionEdge(__x, __exceptionEdge);

        Value __result = __invokeCc.getReturn();
        if (__callTarget instanceof DirectCallTargetNode)
        {
            emitDirectCall((DirectCallTargetNode) __callTarget, __result, __parameters, AllocatableValue.NONE, __callState);
        }
        else if (__callTarget instanceof IndirectCallTargetNode)
        {
            emitIndirectCall((IndirectCallTargetNode) __callTarget, __result, __parameters, AllocatableValue.NONE, __callState);
        }
        else
        {
            throw GraalError.shouldNotReachHere();
        }

        if (ValueUtil.isLegal(__result))
        {
            setResult(__x.asNode(), this.___gen.emitMove(__result));
        }

        if (__x instanceof InvokeWithExceptionNode)
        {
            this.___gen.emitJump(getLIRBlock(((InvokeWithExceptionNode) __x).next()));
        }
    }

    protected abstract void emitDirectCall(DirectCallTargetNode __callTarget, Value __result, Value[] __parameters, Value[] __temps, LIRFrameState __callState);

    protected abstract void emitIndirectCall(IndirectCallTargetNode __callTarget, Value __result, Value[] __parameters, Value[] __temps, LIRFrameState __callState);

    @Override
    public Value[] visitInvokeArguments(CallingConvention __invokeCc, Collection<ValueNode> __arguments)
    {
        // for each argument, load it into the correct location
        Value[] __result = new Value[__arguments.size()];
        int __j = 0;
        for (ValueNode __arg : __arguments)
        {
            if (__arg != null)
            {
                AllocatableValue __operand = __invokeCc.getArgument(__j);
                this.___gen.emitMove(__operand, operand(__arg));
                __result[__j] = __operand;
                __j++;
            }
            else
            {
                throw GraalError.shouldNotReachHere("I thought we no longer have null entries for two-slot types...");
            }
        }
        return __result;
    }

    ///
    // This method tries to create a switch implementation that is optimal for the given switch. It
    // will either generate a sequential if/then/else cascade, a set of range tests or a table switch.
    //
    // If the given switch does not contain int keys, it will always create a sequential implementation.
    ///
    @Override
    public void emitSwitch(SwitchNode __x)
    {
        LabelRef __defaultTarget = getLIRBlock(__x.defaultSuccessor());
        int __keyCount = __x.keyCount();
        if (__keyCount == 0)
        {
            this.___gen.emitJump(__defaultTarget);
        }
        else
        {
            Variable __value = this.___gen.load(operand(__x.value()));
            if (__keyCount == 1)
            {
                double __probability = __x.probability(__x.keySuccessor(0));
                LIRKind __kind = this.___gen.getLIRKind(__x.value().stamp(NodeView.DEFAULT));
                Value __key = this.___gen.emitConstant(__kind, __x.keyAt(0));
                this.___gen.emitCompareBranch(__kind.getPlatformKind(), this.___gen.load(operand(__x.value())), __key, Condition.EQ, getLIRBlock(__x.keySuccessor(0)), __defaultTarget, __probability);
            }
            else if (__x instanceof IntegerSwitchNode && __x.isSorted())
            {
                IntegerSwitchNode __intSwitch = (IntegerSwitchNode) __x;
                LabelRef[] __keyTargets = new LabelRef[__keyCount];
                JavaConstant[] __keyConstants = new JavaConstant[__keyCount];
                double[] __keyProbabilities = new double[__keyCount];
                JavaKind __keyKind = __intSwitch.keyAt(0).getJavaKind();
                for (int __i = 0; __i < __keyCount; __i++)
                {
                    __keyTargets[__i] = getLIRBlock(__intSwitch.keySuccessor(__i));
                    __keyConstants[__i] = __intSwitch.keyAt(__i);
                    __keyProbabilities[__i] = __intSwitch.keyProbability(__i);
                }
                this.___gen.emitStrategySwitch(__keyConstants, __keyProbabilities, __keyTargets, __defaultTarget, __value);
            }
            else
            {
                // keyKind != JavaKind.Int || !x.isSorted()
                LabelRef[] __keyTargets = new LabelRef[__keyCount];
                Constant[] __keyConstants = new Constant[__keyCount];
                double[] __keyProbabilities = new double[__keyCount];
                for (int __i = 0; __i < __keyCount; __i++)
                {
                    __keyTargets[__i] = getLIRBlock(__x.keySuccessor(__i));
                    __keyConstants[__i] = __x.keyAt(__i);
                    __keyProbabilities[__i] = __x.keyProbability(__i);
                }

                // hopefully only a few entries
                this.___gen.emitStrategySwitch(new SwitchStrategy.SequentialStrategy(__keyProbabilities, __keyConstants), __value, __keyTargets, __defaultTarget);
            }
        }
    }

    public LockStackHolder getLockStackHolder()
    {
        return this.___lockStackHolder;
    }

    @Override
    public LIRFrameState state(DeoptimizingNode __deopt)
    {
        if (!__deopt.canDeoptimize())
        {
            return null;
        }
        return LIRFrameState.NO_STATE;
    }

    public LIRFrameState stateWithExceptionEdge(DeoptimizingNode __deopt, LabelRef __exceptionEdge)
    {
        if (!__deopt.canDeoptimize())
        {
            return null;
        }
        if (this.___gen.needOnlyOopMaps())
        {
            return LIRFrameState.NO_STATE;
        }
        return new LIRFrameState(__exceptionEdge);
    }

    @Override
    public void emitOverflowCheckBranch(AbstractBeginNode __overflowSuccessor, AbstractBeginNode __next, Stamp __stamp, double __probability)
    {
        this.___gen.emitOverflowCheckBranch(getLIRBlock(__overflowSuccessor), getLIRBlock(__next), getLIRGeneratorTool().getLIRKind(__stamp), __probability);
    }

    @Override
    public LIRGeneratorTool getLIRGeneratorTool()
    {
        return this.___gen;
    }
}
