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

// @NodeInfo.allowedUsageTypes "InputType.Memory"
// @class InvokeWithExceptionNode
public final class InvokeWithExceptionNode extends ControlSplitNode implements Invoke, MemoryCheckpoint.Single, LIRLowerable, UncheckedInterfaceProvider
{
    // @def
    public static final NodeClass<InvokeWithExceptionNode> TYPE = NodeClass.create(InvokeWithExceptionNode.class);

    // @def
    private static final double EXCEPTION_PROBA = 1e-5;

    @Node.Successor
    // @field
    AbstractBeginNode ___next;
    @Node.Successor
    // @field
    AbstractBeginNode ___exceptionEdge;
    @Node.OptionalInput
    // @field
    ValueNode ___classInit;
    @Node.Input(InputType.Extension)
    // @field
    CallTargetNode ___callTarget;
    @Node.OptionalInput(InputType.StateI)
    // @field
    FrameState ___stateDuring;
    @Node.OptionalInput(InputType.StateI)
    // @field
    FrameState ___stateAfter;
    // @field
    protected final int ___bci;
    // @field
    protected boolean ___polymorphic;
    // @field
    protected boolean ___useForInlining;
    // @field
    protected double ___exceptionProbability;

    // @cons InvokeWithExceptionNode
    public InvokeWithExceptionNode(CallTargetNode __callTarget, AbstractBeginNode __exceptionEdge, int __bci)
    {
        super(TYPE, __callTarget.returnStamp().getTrustedStamp());
        this.___exceptionEdge = __exceptionEdge;
        this.___bci = __bci;
        this.___callTarget = __callTarget;
        this.___polymorphic = false;
        this.___useForInlining = true;
        this.___exceptionProbability = EXCEPTION_PROBA;
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
        return this.___exceptionEdge;
    }

    public void setExceptionEdge(AbstractBeginNode __x)
    {
        updatePredecessor(this.___exceptionEdge, __x);
        this.___exceptionEdge = __x;
    }

    @Override
    public AbstractBeginNode next()
    {
        return this.___next;
    }

    public void setNext(AbstractBeginNode __x)
    {
        updatePredecessor(this.___next, __x);
        this.___next = __x;
    }

    @Override
    public CallTargetNode callTarget()
    {
        return this.___callTarget;
    }

    void setCallTarget(CallTargetNode __callTarget)
    {
        updateUsages(this.___callTarget, __callTarget);
        this.___callTarget = __callTarget;
    }

    public MethodCallTargetNode methodCallTarget()
    {
        return (MethodCallTargetNode) this.___callTarget;
    }

    @Override
    public boolean isPolymorphic()
    {
        return this.___polymorphic;
    }

    @Override
    public void setPolymorphic(boolean __value)
    {
        this.___polymorphic = __value;
    }

    @Override
    public boolean useForInlining()
    {
        return this.___useForInlining;
    }

    @Override
    public void setUseForInlining(boolean __value)
    {
        this.___useForInlining = __value;
    }

    @Override
    public int bci()
    {
        return this.___bci;
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
        return this.___stateAfter;
    }

    @Override
    public void setStateAfter(FrameState __stateAfter)
    {
        updateUsages(this.___stateAfter, __stateAfter);
        this.___stateAfter = __stateAfter;
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
        AbstractBeginNode __exceptionObject = this.___exceptionEdge;
        setExceptionEdge(null);
        setNext(null);
        InvokeWithExceptionNode __repl = graph().add(new InvokeWithExceptionNode(callTarget(), __exceptionObject, __newBci));
        __repl.setStateAfter(this.___stateAfter);
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
        CallTargetNode __call = this.___callTarget;
        FrameState __state = stateAfter();
        if (this.___exceptionEdge != null)
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
        return __successor == this.___next ? 1 - this.___exceptionProbability : this.___exceptionProbability;
    }

    @Override
    public boolean canDeoptimize()
    {
        return true;
    }

    @Override
    public FrameState stateDuring()
    {
        return this.___stateDuring;
    }

    @Override
    public void setStateDuring(FrameState __stateDuring)
    {
        updateUsages(this.___stateDuring, __stateDuring);
        this.___stateDuring = __stateDuring;
    }

    @Override
    public AbstractBeginNode getPrimarySuccessor()
    {
        return this.next();
    }

    @Override
    public Stamp uncheckedStamp()
    {
        return this.___callTarget.returnStamp().getUncheckedStamp();
    }

    @Override
    public void setClassInit(ValueNode __classInit)
    {
        this.___classInit = __classInit;
        updateUsages(null, __classInit);
    }

    @Override
    public ValueNode classInit()
    {
        return this.___classInit;
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

    ///
    // Replaces this InvokeWithExceptionNode with a normal InvokeNode. Kills the exception dispatch code.
    ///
    public InvokeNode replaceWithInvoke()
    {
        InvokeNode __invokeNode = graph().add(new InvokeNode(this.___callTarget, this.___bci));
        AbstractBeginNode __oldException = this.___exceptionEdge;
        graph().replaceSplitWithFixed(this, __invokeNode, this.next());
        GraphUtil.killCFG(__oldException);
        return __invokeNode;
    }
}
