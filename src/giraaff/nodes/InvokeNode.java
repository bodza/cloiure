package giraaff.nodes;

import jdk.vm.ci.meta.JavaKind;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
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
    // @def
    public static final NodeClass<InvokeNode> TYPE = NodeClass.create(InvokeNode.class);

    @OptionalInput
    // @field
    ValueNode classInit;
    @Input(InputType.Extension)
    // @field
    CallTargetNode callTarget;
    @OptionalInput(InputType.State)
    // @field
    FrameState stateDuring;
    // @field
    protected final int bci;
    // @field
    protected boolean polymorphic;
    // @field
    protected boolean useForInlining;

    // @cons
    public InvokeNode(CallTargetNode __callTarget, int __bci)
    {
        this(__callTarget, __bci, __callTarget.returnStamp().getTrustedStamp());
    }

    // @cons
    public InvokeNode(CallTargetNode __callTarget, int __bci, Stamp __stamp)
    {
        super(TYPE, __stamp);
        this.callTarget = __callTarget;
        this.bci = __bci;
        this.polymorphic = false;
        this.useForInlining = true;
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
    public boolean isAllowedUsageType(InputType __type)
    {
        if (!super.isAllowedUsageType(__type))
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
    public int bci()
    {
        return bci;
    }

    @Override
    public void intrinsify(Node __node)
    {
        CallTargetNode __call = callTarget;
        FrameState __currentStateAfter = stateAfter();
        if (__node instanceof StateSplit)
        {
            StateSplit __stateSplit = (StateSplit) __node;
            __stateSplit.setStateAfter(__currentStateAfter);
        }
        if (__node instanceof ForeignCallNode)
        {
            ForeignCallNode __foreign = (ForeignCallNode) __node;
            __foreign.setBci(bci());
        }
        if (__node instanceof FixedWithNextNode)
        {
            graph().replaceFixedWithFixed(this, (FixedWithNextNode) __node);
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
            graph().replaceFixed(this, __node);
        }
        GraphUtil.killWithUnusedFloatingInputs(__call);
        if (__currentStateAfter.hasNoUsages())
        {
            GraphUtil.killWithUnusedFloatingInputs(__currentStateAfter);
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
    public void setStateDuring(FrameState __stateDuring)
    {
        updateUsages(this.stateDuring, __stateDuring);
        this.stateDuring = __stateDuring;
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
}
