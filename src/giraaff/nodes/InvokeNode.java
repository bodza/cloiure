package giraaff.nodes;

import jdk.vm.ci.meta.JavaKind;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.Verbosity;
import giraaff.nodes.extended.ForeignCallNode;
import giraaff.nodes.java.MethodCallTargetNode;
import giraaff.nodes.memory.AbstractMemoryCheckpoint;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.spi.NodeLIRBuilderTool;
import giraaff.nodes.spi.UncheckedInterfaceProvider;
import giraaff.nodes.util.GraphUtil;

import giraaff.nodeinfo.InputType;

/**
 * The {@code InvokeNode} represents all kinds of method calls.
 */
// @NodeInfo.allowedUsageTypes "Memory"
// @class InvokeNode
public final class InvokeNode extends AbstractMemoryCheckpoint implements Invoke, LIRLowerable, MemoryCheckpoint.Single, UncheckedInterfaceProvider
{
    public static final NodeClass<InvokeNode> TYPE = NodeClass.create(InvokeNode.class);

    @OptionalInput ValueNode classInit;
    @Input(InputType.Extension) CallTargetNode callTarget;
    @OptionalInput(InputType.State) FrameState stateDuring;
    protected final int bci;
    protected boolean polymorphic;
    protected boolean useForInlining;

    // @cons
    public InvokeNode(CallTargetNode callTarget, int bci)
    {
        this(callTarget, bci, callTarget.returnStamp().getTrustedStamp());
    }

    // @cons
    public InvokeNode(CallTargetNode callTarget, int bci, Stamp stamp)
    {
        super(TYPE, stamp);
        this.callTarget = callTarget;
        this.bci = bci;
        this.polymorphic = false;
        this.useForInlining = true;
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
    public boolean isAllowedUsageType(InputType type)
    {
        if (!super.isAllowedUsageType(type))
        {
            if (getStackKind() != JavaKind.Void)
            {
                if (callTarget instanceof MethodCallTargetNode && ((MethodCallTargetNode) callTarget).targetMethod().getAnnotation(NodeIntrinsic.class) != null)
                {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    @Override
    public LocationIdentity getLocationIdentity()
    {
        return LocationIdentity.any();
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
    public void intrinsify(Node node)
    {
        CallTargetNode call = callTarget;
        FrameState currentStateAfter = stateAfter();
        if (node instanceof StateSplit)
        {
            StateSplit stateSplit = (StateSplit) node;
            stateSplit.setStateAfter(currentStateAfter);
        }
        if (node instanceof ForeignCallNode)
        {
            ForeignCallNode foreign = (ForeignCallNode) node;
            foreign.setBci(bci());
        }
        if (node instanceof FixedWithNextNode)
        {
            graph().replaceFixedWithFixed(this, (FixedWithNextNode) node);
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
            graph().replaceFixed(this, node);
        }
        GraphUtil.killWithUnusedFloatingInputs(call);
        if (currentStateAfter.hasNoUsages())
        {
            GraphUtil.killWithUnusedFloatingInputs(currentStateAfter);
        }
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
}
