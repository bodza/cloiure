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

/**
 * Node for a {@linkplain ForeignCallDescriptor foreign} call.
 */
// @NodeInfo.allowedUsageTypes "Memory"
// @class ForeignCallNode
public final class ForeignCallNode extends AbstractMemoryCheckpoint implements LIRLowerable, DeoptimizingNode.DeoptDuring, MemoryCheckpoint.Multi
{
    public static final NodeClass<ForeignCallNode> TYPE = NodeClass.create(ForeignCallNode.class);

    @Input protected NodeInputList<ValueNode> arguments;
    @OptionalInput(InputType.State) protected FrameState stateDuring;
    protected final ForeignCallsProvider foreignCalls;

    protected final ForeignCallDescriptor descriptor;
    protected int bci = BytecodeFrame.UNKNOWN_BCI;

    public static boolean intrinsify(GraphBuilderContext b, ResolvedJavaMethod targetMethod, @InjectedNodeParameter Stamp returnStamp, @InjectedNodeParameter ForeignCallsProvider foreignCalls, ForeignCallDescriptor descriptor, ValueNode... arguments)
    {
        ForeignCallNode node = new ForeignCallNode(foreignCalls, descriptor, arguments);
        node.setStamp(returnStamp);

        /*
         * Need to update the BCI of a ForeignCallNode so that it gets the stateDuring in the case that the
         * foreign call can deoptimize. As with all deoptimization, we need a state in a non-intrinsic method.
         */
        GraphBuilderContext nonIntrinsicAncestor = b.getNonIntrinsicAncestor();
        if (nonIntrinsicAncestor != null)
        {
            node.setBci(nonIntrinsicAncestor.bci());
        }

        JavaKind returnKind = targetMethod.getSignature().getReturnKind();
        if (returnKind == JavaKind.Void)
        {
            b.add(node);
        }
        else
        {
            b.addPush(returnKind, node);
        }

        return true;
    }

    static boolean verifyDescriptor(GraphBuilderContext b, ResolvedJavaMethod targetMethod, ForeignCallDescriptor descriptor)
    {
        int parameters = 1;
        for (Class<?> arg : descriptor.getArgumentTypes())
        {
            ResolvedJavaType res = b.getMetaAccess().lookupJavaType(arg);
            ResolvedJavaType parameterType = (ResolvedJavaType) targetMethod.getSignature().getParameterType(parameters, targetMethod.getDeclaringClass());
            parameters++;
        }
        return true;
    }

    // @cons
    public ForeignCallNode(ForeignCallsProvider foreignCalls, ForeignCallDescriptor descriptor, ValueNode... arguments)
    {
        this(TYPE, foreignCalls, descriptor, arguments);
    }

    // @cons
    public ForeignCallNode(ForeignCallsProvider foreignCalls, ForeignCallDescriptor descriptor, Stamp stamp, List<ValueNode> arguments)
    {
        super(TYPE, stamp);
        this.arguments = new NodeInputList<>(this, arguments);
        this.descriptor = descriptor;
        this.foreignCalls = foreignCalls;
    }

    // @cons
    public ForeignCallNode(ForeignCallsProvider foreignCalls, ForeignCallDescriptor descriptor, Stamp stamp)
    {
        super(TYPE, stamp);
        this.arguments = new NodeInputList<>(this);
        this.descriptor = descriptor;
        this.foreignCalls = foreignCalls;
    }

    // @cons
    protected ForeignCallNode(NodeClass<? extends ForeignCallNode> c, ForeignCallsProvider foreignCalls, ForeignCallDescriptor descriptor, ValueNode... arguments)
    {
        super(c, StampFactory.forKind(JavaKind.fromJavaClass(descriptor.getResultType())));
        this.arguments = new NodeInputList<>(this, arguments);
        this.descriptor = descriptor;
        this.foreignCalls = foreignCalls;
    }

    @Override
    public boolean hasSideEffect()
    {
        return !foreignCalls.isReexecutable(descriptor);
    }

    public ForeignCallDescriptor getDescriptor()
    {
        return descriptor;
    }

    @Override
    public LocationIdentity[] getLocationIdentities()
    {
        return foreignCalls.getKilledLocations(descriptor);
    }

    protected Value[] operands(NodeLIRBuilderTool gen)
    {
        Value[] operands = new Value[arguments.size()];
        for (int i = 0; i < operands.length; i++)
        {
            operands[i] = gen.operand(arguments.get(i));
        }
        return operands;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        ForeignCallLinkage linkage = gen.getLIRGeneratorTool().getForeignCalls().lookupForeignCall(descriptor);
        Value[] operands = operands(gen);
        Value result = gen.getLIRGeneratorTool().emitForeignCall(linkage, gen.state(this), operands);
        if (result != null)
        {
            gen.setResult(this, result);
        }
    }

    @Override
    public void setStateAfter(FrameState x)
    {
        super.setStateAfter(x);
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

    public int getBci()
    {
        return bci;
    }

    /**
     * Set the {@code bci} of the invoke bytecode for use when converting a stateAfter into a stateDuring.
     */
    public void setBci(int bci)
    {
        this.bci = bci;
    }

    @Override
    public void computeStateDuring(FrameState currentStateAfter)
    {
        FrameState newStateDuring;
        if ((currentStateAfter.stackSize() > 0 && currentStateAfter.stackAt(currentStateAfter.stackSize() - 1) == this) || (currentStateAfter.stackSize() > 1 && currentStateAfter.stackAt(currentStateAfter.stackSize() - 2) == this))
        {
            // The result of this call is on the top of stack, so roll back to the previous bci.
            newStateDuring = currentStateAfter.duplicateModifiedDuringCall(bci, this.getStackKind());
        }
        else
        {
            newStateDuring = currentStateAfter;
        }
        setStateDuring(newStateDuring);
    }

    @Override
    public boolean canDeoptimize()
    {
        return foreignCalls.canDeoptimize(descriptor);
    }

    public boolean isGuaranteedSafepoint()
    {
        return foreignCalls.isGuaranteedSafepoint(descriptor);
    }

    public NodeInputList<ValueNode> getArguments()
    {
        return arguments;
    }
}
