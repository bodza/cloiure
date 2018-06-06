package giraaff.nodes.extended;

import java.util.List;

import jdk.vm.ci.code.BytecodeFrame;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.meta.Value;

import org.graalvm.word.LocationIdentity;

import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.core.common.spi.ForeignCallLinkage;
import giraaff.core.common.spi.ForeignCallsProvider;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.NodeInputList;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.DeoptimizingNode;
import giraaff.nodes.FrameState;
import giraaff.nodes.ValueNode;
import giraaff.nodes.graphbuilderconf.GraphBuilderContext;
import giraaff.nodes.memory.AbstractMemoryCheckpoint;
import giraaff.nodes.memory.MemoryCheckpoint;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

///
// Node for a {@linkplain ForeignCallDescriptor foreign} call.
///
// @NodeInfo.allowedUsageTypes "InputType.Memory"
// @class ForeignCallNode
public final class ForeignCallNode extends AbstractMemoryCheckpoint implements LIRLowerable, DeoptimizingNode.DeoptDuring, MemoryCheckpoint.Multi
{
    // @def
    public static final NodeClass<ForeignCallNode> TYPE = NodeClass.create(ForeignCallNode.class);

    @Node.Input
    // @field
    protected NodeInputList<ValueNode> ___arguments;
    @Node.OptionalInput(InputType.StateI)
    // @field
    protected FrameState ___stateDuring;
    // @field
    protected final ForeignCallsProvider ___foreignCalls;

    // @field
    protected final ForeignCallDescriptor ___descriptor;
    // @field
    protected int ___bci = BytecodeFrame.UNKNOWN_BCI;

    public static boolean intrinsify(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, @Node.InjectedNodeParameter Stamp __returnStamp, @Node.InjectedNodeParameter ForeignCallsProvider __foreignCalls, ForeignCallDescriptor __descriptor, ValueNode... __arguments)
    {
        ForeignCallNode __node = new ForeignCallNode(__foreignCalls, __descriptor, __arguments);
        __node.setStamp(__returnStamp);

        // Need to update the BCI of a ForeignCallNode so that it gets the stateDuring in the case that the
        // foreign call can deoptimize. As with all deoptimization, we need a state in a non-intrinsic method.
        GraphBuilderContext __nonIntrinsicAncestor = __b.getNonIntrinsicAncestor();
        if (__nonIntrinsicAncestor != null)
        {
            __node.setBci(__nonIntrinsicAncestor.bci());
        }

        JavaKind __returnKind = __targetMethod.getSignature().getReturnKind();
        if (__returnKind == JavaKind.Void)
        {
            __b.add(__node);
        }
        else
        {
            __b.addPush(__returnKind, __node);
        }

        return true;
    }

    static boolean verifyDescriptor(GraphBuilderContext __b, ResolvedJavaMethod __targetMethod, ForeignCallDescriptor __descriptor)
    {
        int __parameters = 1;
        for (Class<?> __arg : __descriptor.getArgumentTypes())
        {
            ResolvedJavaType __res = __b.getMetaAccess().lookupJavaType(__arg);
            ResolvedJavaType __parameterType = (ResolvedJavaType) __targetMethod.getSignature().getParameterType(__parameters, __targetMethod.getDeclaringClass());
            __parameters++;
        }
        return true;
    }

    // @cons ForeignCallNode
    public ForeignCallNode(ForeignCallsProvider __foreignCalls, ForeignCallDescriptor __descriptor, ValueNode... __arguments)
    {
        this(TYPE, __foreignCalls, __descriptor, __arguments);
    }

    // @cons ForeignCallNode
    public ForeignCallNode(ForeignCallsProvider __foreignCalls, ForeignCallDescriptor __descriptor, Stamp __stamp, List<ValueNode> __arguments)
    {
        super(TYPE, __stamp);
        this.___arguments = new NodeInputList<>(this, __arguments);
        this.___descriptor = __descriptor;
        this.___foreignCalls = __foreignCalls;
    }

    // @cons ForeignCallNode
    public ForeignCallNode(ForeignCallsProvider __foreignCalls, ForeignCallDescriptor __descriptor, Stamp __stamp)
    {
        super(TYPE, __stamp);
        this.___arguments = new NodeInputList<>(this);
        this.___descriptor = __descriptor;
        this.___foreignCalls = __foreignCalls;
    }

    // @cons ForeignCallNode
    protected ForeignCallNode(NodeClass<? extends ForeignCallNode> __c, ForeignCallsProvider __foreignCalls, ForeignCallDescriptor __descriptor, ValueNode... __arguments)
    {
        super(__c, StampFactory.forKind(JavaKind.fromJavaClass(__descriptor.getResultType())));
        this.___arguments = new NodeInputList<>(this, __arguments);
        this.___descriptor = __descriptor;
        this.___foreignCalls = __foreignCalls;
    }

    @Override
    public boolean hasSideEffect()
    {
        return !this.___foreignCalls.isReexecutable(this.___descriptor);
    }

    public ForeignCallDescriptor getDescriptor()
    {
        return this.___descriptor;
    }

    @Override
    public LocationIdentity[] getLocationIdentities()
    {
        return this.___foreignCalls.getKilledLocations(this.___descriptor);
    }

    protected Value[] operands(NodeLIRBuilderTool __gen)
    {
        Value[] __operands = new Value[this.___arguments.size()];
        for (int __i = 0; __i < __operands.length; __i++)
        {
            __operands[__i] = __gen.operand(this.___arguments.get(__i));
        }
        return __operands;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        ForeignCallLinkage __linkage = __gen.getLIRGeneratorTool().getForeignCalls().lookupForeignCall(this.___descriptor);
        Value[] __operands = operands(__gen);
        Value __result = __gen.getLIRGeneratorTool().emitForeignCall(__linkage, __gen.state(this), __operands);
        if (__result != null)
        {
            __gen.setResult(this, __result);
        }
    }

    @Override
    public void setStateAfter(FrameState __x)
    {
        super.setStateAfter(__x);
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

    public int getBci()
    {
        return this.___bci;
    }

    ///
    // Set the {@code bci} of the invoke bytecode for use when converting a stateAfter into a stateDuring.
    ///
    public void setBci(int __bci)
    {
        this.___bci = __bci;
    }

    @Override
    public void computeStateDuring(FrameState __currentStateAfter)
    {
        FrameState __newStateDuring;
        if ((__currentStateAfter.stackSize() > 0 && __currentStateAfter.stackAt(__currentStateAfter.stackSize() - 1) == this) || (__currentStateAfter.stackSize() > 1 && __currentStateAfter.stackAt(__currentStateAfter.stackSize() - 2) == this))
        {
            // The result of this call is on the top of stack, so roll back to the previous bci.
            __newStateDuring = __currentStateAfter.duplicateModifiedDuringCall(this.___bci, this.getStackKind());
        }
        else
        {
            __newStateDuring = __currentStateAfter;
        }
        setStateDuring(__newStateDuring);
    }

    @Override
    public boolean canDeoptimize()
    {
        return this.___foreignCalls.canDeoptimize(this.___descriptor);
    }

    public boolean isGuaranteedSafepoint()
    {
        return this.___foreignCalls.isGuaranteedSafepoint(this.___descriptor);
    }

    public NodeInputList<ValueNode> getArguments()
    {
        return this.___arguments;
    }
}
