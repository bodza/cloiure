package giraaff.nodes;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.Verbosity;
import giraaff.nodes.extended.ForeignCallNode;
import giraaff.nodes.java.MethodCallTargetNode;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.spi.UncheckedInterfaceProvider;
import giraaff.nodes.util.GraphUtil;

import giraaff.nodeinfo.InputType;

// NodeInfo.allowedUsageTypes = Memory
public final class InvokeWithExceptionNode extends ControlSplitNode implements Invoke, MemoryCheckpoint.Single, LIRLowerable, UncheckedInterfaceProvider
{
    public static final NodeClass<InvokeWithExceptionNode> TYPE = NodeClass.create(InvokeWithExceptionNode.class);

    private static final double EXCEPTION_PROBA = 1e-5;

    @Successor AbstractBeginNode next;
    @Successor AbstractBeginNode exceptionEdge;
    @OptionalInput ValueNode classInit;
    @Input(InputType.Extension) CallTargetNode callTarget;
    @OptionalInput(InputType.State) FrameState stateDuring;
    @OptionalInput(InputType.State) FrameState stateAfter;
    protected final int bci;
    protected boolean polymorphic;
    protected boolean useForInlining;
    protected double exceptionProbability;

    public InvokeWithExceptionNode(CallTargetNode callTarget, AbstractBeginNode exceptionEdge, int bci)
    {
        super(TYPE, callTarget.returnStamp().getTrustedStamp());
        this.exceptionEdge = exceptionEdge;
        this.bci = bci;
        this.callTarget = callTarget;
        this.polymorphic = false;
        this.useForInlining = true;
        this.exceptionProbability = EXCEPTION_PROBA;
    }

    @Override
    protected void afterClone(Node other)
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

    public void setExceptionEdge(AbstractBeginNode x)
    {
        updatePredecessor(exceptionEdge, x);
        exceptionEdge = x;
    }

    @Override
    public AbstractBeginNode next()
    {
        return next;
    }

    public void setNext(AbstractBeginNode x)
    {
        updatePredecessor(next, x);
        next = x;
    }

    @Override
    public CallTargetNode callTarget()
    {
        return callTarget;
    }

    void setCallTarget(CallTargetNode callTarget)
    {
        updateUsages(this.callTarget, callTarget);
        this.callTarget = callTarget;
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
    public void setPolymorphic(boolean value)
    {
        this.polymorphic = value;
    }

    @Override
    public boolean useForInlining()
    {
        return useForInlining;
    }

    @Override
    public void setUseForInlining(boolean value)
    {
        this.useForInlining = value;
    }

    @Override
    public String toString(Verbosity verbosity)
    {
        if (verbosity == Verbosity.Long)
        {
            return super.toString(Verbosity.Short) + "(bci=" + bci() + ")";
        }
        else if (verbosity == Verbosity.Name)
        {
            return "Invoke#" + (callTarget == null ? "null" : callTarget().targetName());
        }
        else
        {
            return super.toString(verbosity);
        }
    }

    @Override
    public int bci()
    {
        return bci;
    }

    @Override
    public void setNext(FixedNode x)
    {
        if (x != null)
        {
            this.setNext(KillingBeginNode.begin(x, getLocationIdentity()));
        }
        else
        {
            this.setNext(null);
        }
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        gen.emitInvoke(this);
    }

    @Override
    public FrameState stateAfter()
    {
        return stateAfter;
    }

    @Override
    public void setStateAfter(FrameState stateAfter)
    {
        updateUsages(this.stateAfter, stateAfter);
        this.stateAfter = stateAfter;
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
        AbstractBeginNode edge = exceptionEdge();
        setExceptionEdge(null);
        GraphUtil.killCFG(edge);
    }

    public void replaceWithNewBci(int newBci)
    {
        AbstractBeginNode nextNode = next();
        AbstractBeginNode exceptionObject = exceptionEdge;
        setExceptionEdge(null);
        setNext(null);
        InvokeWithExceptionNode repl = graph().add(new InvokeWithExceptionNode(callTarget(), exceptionObject, newBci));
        repl.setStateAfter(stateAfter);
        this.setStateAfter(null);
        this.replaceAtPredecessor(repl);
        repl.setNext(nextNode);
        boolean removed = this.callTarget().removeUsage(this);
        this.replaceAtUsages(repl);
        this.markDeleted();
    }

    @Override
    public void intrinsify(Node node)
    {
        CallTargetNode call = callTarget;
        FrameState state = stateAfter();
        if (exceptionEdge != null)
        {
            killExceptionEdge();
        }
        if (node instanceof StateSplit)
        {
            StateSplit stateSplit = (StateSplit) node;
            stateSplit.setStateAfter(state);
        }
        if (node instanceof ForeignCallNode)
        {
            ForeignCallNode foreign = (ForeignCallNode) node;
            foreign.setBci(bci());
        }
        if (node == null)
        {
            graph().removeSplit(this, next());
        }
        else if (node instanceof ControlSinkNode)
        {
            this.replaceAtPredecessor(node);
            this.replaceAtUsages(null);
            GraphUtil.killCFG(this);
            return;
        }
        else
        {
            graph().replaceSplit(this, node, next());
        }
        GraphUtil.killWithUnusedFloatingInputs(call);
        if (state.hasNoUsages())
        {
            GraphUtil.killWithUnusedFloatingInputs(state);
        }
    }

    @Override
    public double probability(AbstractBeginNode successor)
    {
        return successor == next ? 1 - exceptionProbability : exceptionProbability;
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
    public void setStateDuring(FrameState stateDuring)
    {
        updateUsages(this.stateDuring, stateDuring);
        this.stateDuring = stateDuring;
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
    public void setClassInit(ValueNode classInit)
    {
        this.classInit = classInit;
        updateUsages(null, classInit);
    }

    @Override
    public ValueNode classInit()
    {
        return classInit;
    }

    @Override
    public boolean setProbability(AbstractBeginNode successor, double value)
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
     * Replaces this InvokeWithExceptionNode with a normal InvokeNode. Kills the exception dispatch
     * code.
     */
    public InvokeNode replaceWithInvoke()
    {
        InvokeNode invokeNode = graph().add(new InvokeNode(callTarget, bci));
        AbstractBeginNode oldException = this.exceptionEdge;
        graph().replaceSplitWithFixed(this, invokeNode, this.next());
        GraphUtil.killCFG(oldException);
        return invokeNode;
    }
}
