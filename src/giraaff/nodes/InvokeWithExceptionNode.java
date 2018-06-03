package giraaff.nodes;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.nodes.extended.ForeignCallNode;
import giraaff.nodes.java.MethodCallTargetNode;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.spi.UncheckedInterfaceProvider;
import giraaff.nodes.util.GraphUtil;

import giraaff.nodeinfo.InputType;

// @NodeInfo.allowedUsageTypes "Memory"
// @class InvokeWithExceptionNode
public final class InvokeWithExceptionNode extends ControlSplitNode implements Invoke, MemoryCheckpoint.Single, LIRLowerable, UncheckedInterfaceProvider
{
    // @def
    public static final NodeClass<InvokeWithExceptionNode> TYPE = NodeClass.create(InvokeWithExceptionNode.class);

    // @def
    private static final double EXCEPTION_PROBA = 1e-5;

    @Successor
    // @field
    AbstractBeginNode next;
    @Successor
    // @field
    AbstractBeginNode exceptionEdge;
    @OptionalInput
    // @field
    ValueNode classInit;
    @Input(InputType.Extension)
    // @field
    CallTargetNode callTarget;
    @OptionalInput(InputType.State)
    // @field
    FrameState stateDuring;
    @OptionalInput(InputType.State)
    // @field
    FrameState stateAfter;
    // @field
    protected final int bci;
    // @field
    protected boolean polymorphic;
    // @field
    protected boolean useForInlining;
    // @field
    protected double exceptionProbability;

    // @cons
    public InvokeWithExceptionNode(CallTargetNode __callTarget, AbstractBeginNode __exceptionEdge, int __bci)
    {
        super(TYPE, __callTarget.returnStamp().getTrustedStamp());
        this.exceptionEdge = __exceptionEdge;
        this.bci = __bci;
        this.callTarget = __callTarget;
        this.polymorphic = false;
        this.useForInlining = true;
        this.exceptionProbability = EXCEPTION_PROBA;
    }

    @Override
    protected void afterClone(Node __other)
    {
    }

    @Override
    public FixedNode asFixedNode()
    {
        return this;
    }

    public AbstractBeginNode exceptionEdge()
    {
        return exceptionEdge;
    }

    public void setExceptionEdge(AbstractBeginNode __x)
    {
        updatePredecessor(exceptionEdge, __x);
        exceptionEdge = __x;
    }

    @Override
    public AbstractBeginNode next()
    {
        return next;
    }

    public void setNext(AbstractBeginNode __x)
    {
        updatePredecessor(next, __x);
        next = __x;
    }

    @Override
    public CallTargetNode callTarget()
    {
        return callTarget;
    }

    void setCallTarget(CallTargetNode __callTarget)
    {
        updateUsages(this.callTarget, __callTarget);
        this.callTarget = __callTarget;
    }

    public MethodCallTargetNode methodCallTarget()
    {
        return (MethodCallTargetNode) callTarget;
    }

    @Override
    public boolean isPolymorphic()
    {
        return polymorphic;
    }

    @Override
    public void setPolymorphic(boolean __value)
    {
        this.polymorphic = __value;
    }

    @Override
    public boolean useForInlining()
    {
        return useForInlining;
    }

    @Override
    public void setUseForInlining(boolean __value)
    {
        this.useForInlining = __value;
    }

    @Override
    public int bci()
    {
        return bci;
    }

    @Override
    public void setNext(FixedNode __x)
    {
        if (__x != null)
        {
            this.setNext(KillingBeginNode.begin(__x, getLocationIdentity()));
        }
        else
        {
            this.setNext(null);
        }
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        __gen.emitInvoke(this);
    }

    @Override
    public FrameState stateAfter()
    {
        return stateAfter;
    }

    @Override
    public void setStateAfter(FrameState __stateAfter)
    {
        updateUsages(this.stateAfter, __stateAfter);
        this.stateAfter = __stateAfter;
    }

    @Override
    public boolean hasSideEffect()
    {
        return true;
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return LocationIdentity.any();
    }

    public void killExceptionEdge()
    {
        AbstractBeginNode __edge = exceptionEdge();
        setExceptionEdge(null);
        GraphUtil.killCFG(__edge);
    }

    public void replaceWithNewBci(int __newBci)
    {
        AbstractBeginNode __nextNode = next();
        AbstractBeginNode __exceptionObject = exceptionEdge;
        setExceptionEdge(null);
        setNext(null);
        InvokeWithExceptionNode __repl = graph().add(new InvokeWithExceptionNode(callTarget(), __exceptionObject, __newBci));
        __repl.setStateAfter(stateAfter);
        this.setStateAfter(null);
        this.replaceAtPredecessor(__repl);
        __repl.setNext(__nextNode);
        boolean __removed = this.callTarget().removeUsage(this);
        this.replaceAtUsages(__repl);
        this.markDeleted();
    }

    @Override
    public void intrinsify(Node __node)
    {
        CallTargetNode __call = callTarget;
        FrameState __state = stateAfter();
        if (exceptionEdge != null)
        {
            killExceptionEdge();
        }
        if (__node instanceof StateSplit)
        {
            StateSplit __stateSplit = (StateSplit) __node;
            __stateSplit.setStateAfter(__state);
        }
        if (__node instanceof ForeignCallNode)
        {
            ForeignCallNode __foreign = (ForeignCallNode) __node;
            __foreign.setBci(bci());
        }
        if (__node == null)
        {
            graph().removeSplit(this, next());
        }
        else if (__node instanceof ControlSinkNode)
        {
            this.replaceAtPredecessor(__node);
            this.replaceAtUsages(null);
            GraphUtil.killCFG(this);
            return;
        }
        else
        {
            graph().replaceSplit(this, __node, next());
        }
        GraphUtil.killWithUnusedFloatingInputs(__call);
        if (__state.hasNoUsages())
        {
            GraphUtil.killWithUnusedFloatingInputs(__state);
        }
    }

    @Override
    public double probability(AbstractBeginNode __successor)
    {
        return __successor == next ? 1 - exceptionProbability : exceptionProbability;
    }

    @Override
    public boolean canDeoptimize()
    {
        return true;
    }

    @Override
    public FrameState stateDuring()
    {
        return stateDuring;
    }

    @Override
    public void setStateDuring(FrameState __stateDuring)
    {
        updateUsages(this.stateDuring, __stateDuring);
        this.stateDuring = __stateDuring;
    }

    @Override
    public AbstractBeginNode getPrimarySuccessor()
    {
        return this.next();
    }

    @Override
    public Stamp uncheckedStamp()
    {
        return this.callTarget.returnStamp().getUncheckedStamp();
    }

    @Override
    public void setClassInit(ValueNode __classInit)
    {
        this.classInit = __classInit;
        updateUsages(null, __classInit);
    }

    @Override
    public ValueNode classInit()
    {
        return classInit;
    }

    @Override
    public boolean setProbability(AbstractBeginNode __successor, double __value)
    {
        // Cannot set probability for exception invokes.
        return false;
    }

    @Override
    public int getSuccessorCount()
    {
        return 2;
    }

    /**
     * Replaces this InvokeWithExceptionNode with a normal InvokeNode. Kills the exception dispatch code.
     */
    public InvokeNode replaceWithInvoke()
    {
        InvokeNode __invokeNode = graph().add(new InvokeNode(callTarget, bci));
        AbstractBeginNode __oldException = this.exceptionEdge;
        graph().replaceSplitWithFixed(this, __invokeNode, this.next());
        GraphUtil.killCFG(__oldException);
        return __invokeNode;
    }
}
