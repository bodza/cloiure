package graalvm.compiler.nodes;

import jdk.vm.ci.meta.JavaKind;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.InputType;
import graalvm.compiler.nodeinfo.NodeCycles;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodeinfo.NodeSize;
import graalvm.compiler.nodeinfo.Verbosity;
import graalvm.compiler.nodes.extended.ForeignCallNode;
import graalvm.compiler.nodes.java.MethodCallTargetNode;
import graalvm.compiler.nodes.memory.AbstractMemoryCheckpoint;
import graalvm.compiler.nodes.memory.MemoryCheckpoint;
import graalvm.compiler.nodes.spi.LIRLowerable;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import graalvm.compiler.nodes.spi.UncheckedInterfaceProvider;
import graalvm.compiler.nodes.util.GraphUtil;
import org.graalvm.word.LocationIdentity;

import java.util.Map;

import static graalvm.compiler.nodeinfo.InputType.Extension;
import static graalvm.compiler.nodeinfo.InputType.Memory;
import static graalvm.compiler.nodeinfo.InputType.State;
import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_2;
import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_64;
import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_8;
import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_UNKNOWN;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_2;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_64;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_8;
import static graalvm.compiler.nodeinfo.NodeSize.SIZE_UNKNOWN;

/**
 * The {@code InvokeNode} represents all kinds of method calls.
 */
@NodeInfo(nameTemplate = "Invoke#{p#targetMethod/s}",
          allowedUsageTypes = {Memory},
          cycles = CYCLES_UNKNOWN,
          cyclesRationale = "We cannot estimate the runtime cost of a call, it is a blackhole." +
                            "However, we can estimate, dyanmically, the cost of the call operation itself based on the type of the call.",
          size = SIZE_UNKNOWN,
          sizeRationale = "We can only dyanmically, based on the type of the call (special, static, virtual, interface) decide" +
                          "how much code is generated for the call.")
public final class InvokeNode extends AbstractMemoryCheckpoint implements Invoke, LIRLowerable, MemoryCheckpoint.Single, UncheckedInterfaceProvider
{
    public static final NodeClass<InvokeNode> TYPE = NodeClass.create(InvokeNode.class);

    @OptionalInput ValueNode classInit;
    @Input(Extension) CallTargetNode callTarget;
    @OptionalInput(State) FrameState stateDuring;
    protected final int bci;
    protected boolean polymorphic;
    protected boolean useForInlining;

    public InvokeNode(CallTargetNode callTarget, int bci)
    {
        this(callTarget, bci, callTarget.returnStamp().getTrustedStamp());
    }

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

    @Override
    public NodeCycles estimatedNodeCycles()
    {
        switch (callTarget().invokeKind())
        {
            case Interface:
                return CYCLES_64;
            case Special:
            case Static:
                return CYCLES_2;
            case Virtual:
                return CYCLES_8;
            default:
                return CYCLES_UNKNOWN;
        }
    }

    @Override
    public NodeSize estimatedNodeSize()
    {
        switch (callTarget().invokeKind())
        {
            case Interface:
                return SIZE_64;
            case Special:
            case Static:
                return SIZE_2;
            case Virtual:
                return SIZE_8;
            default:
                return SIZE_UNKNOWN;
        }
    }
}
