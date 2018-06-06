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

///
// The {@code InvokeNode} represents all kinds of method calls.
///
// @NodeInfo.allowedUsageTypes "InputType.Memory"
// @class InvokeNode
public final class InvokeNode extends AbstractMemoryCheckpoint implements Invoke, LIRLowerable, MemoryCheckpoint.Single, UncheckedInterfaceProvider
{
    // @def
    public static final NodeClass<InvokeNode> TYPE = NodeClass.create(InvokeNode.class);

    @Node.OptionalInput
    // @field
    ValueNode ___classInit;
    @Node.Input(InputType.Extension)
    // @field
    CallTargetNode ___callTarget;
    @Node.OptionalInput(InputType.StateI)
    // @field
    FrameState ___stateDuring;
    // @field
    protected final int ___bci;
    // @field
    protected boolean ___polymorphic;
    // @field
    protected boolean ___useForInlining;

    // @cons InvokeNode
    public InvokeNode(CallTargetNode __callTarget, int __bci)
    {
        this(__callTarget, __bci, __callTarget.returnStamp().getTrustedStamp());
    }

    // @cons InvokeNode
    public InvokeNode(CallTargetNode __callTarget, int __bci, Stamp __stamp)
    {
        super(TYPE, __stamp);
        this.___callTarget = __callTarget;
        this.___bci = __bci;
        this.___polymorphic = false;
        this.___useForInlining = true;
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
        return this.___callTarget;
    }

    void setCallTarget(CallTargetNode __callTarget)
    {
        updateUsages(this.___callTarget, __callTarget);
        this.___callTarget = __callTarget;
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
    public boolean isAllowedUsageType(InputType __type)
    {
        if (!super.isAllowedUsageType(__type))
        {
            if (getStackKind() != JavaKind.Void)
            {
                if (this.___callTarget instanceof MethodCallTargetNode && ((MethodCallTargetNode) this.___callTarget).targetMethod().getAnnotation(Node.NodeIntrinsic.class) != null)
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
        return this.___bci;
    }

    @Override
    public void intrinsify(Node __node)
    {
        CallTargetNode __call = this.___callTarget;
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
        return this.___stateDuring;
    }

    @Override
    public void setStateDuring(FrameState __stateDuring)
    {
        updateUsages(this.___stateDuring, __stateDuring);
        this.___stateDuring = __stateDuring;
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
}
