package giraaff.nodes.java;

import java.util.Collections;

import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.calc.CanonicalCondition;
import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.TypeReference;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Simplifiable;
import giraaff.graph.spi.SimplifierTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.FixedGuardNode;
import giraaff.nodes.FrameState;
import giraaff.nodes.LogicNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.CompareNode;
import giraaff.nodes.spi.VirtualizableAllocation;
import giraaff.nodes.spi.VirtualizerTool;
import giraaff.nodes.util.GraphUtil;
import giraaff.nodes.virtual.VirtualArrayNode;
import giraaff.nodes.virtual.VirtualObjectNode;

/**
 * The {@code NewArrayNode} is used for all array allocations where the element type is know at
 * compile time.
 */
public class NewArrayNode extends AbstractNewArrayNode implements VirtualizableAllocation, Simplifiable
{
    public static final NodeClass<NewArrayNode> TYPE = NodeClass.create(NewArrayNode.class);
    private final ResolvedJavaType elementType;

    public NewArrayNode(ResolvedJavaType elementType, ValueNode length, boolean fillContents)
    {
        this(elementType, length, fillContents, null);
    }

    public NewArrayNode(ResolvedJavaType elementType, ValueNode length, boolean fillContents, FrameState stateBefore)
    {
        this(TYPE, elementType, length, fillContents, stateBefore);
    }

    protected NewArrayNode(NodeClass<? extends NewArrayNode> c, ResolvedJavaType elementType, ValueNode length, boolean fillContents, FrameState stateBefore)
    {
        super(c, StampFactory.objectNonNull(TypeReference.createExactTrusted(elementType.getArrayClass())), length, fillContents, stateBefore);
        this.elementType = elementType;
    }

    @NodeIntrinsic
    private static native Object newArray(@ConstantNodeParameter Class<?> elementType, int length, @ConstantNodeParameter boolean fillContents);

    public static Object newUninitializedArray(Class<?> elementType, int length)
    {
        return newArray(elementType, length, false);
    }

    /**
     * Gets the element type of the array.
     *
     * @return the element type of the array
     */
    public ResolvedJavaType elementType()
    {
        return elementType;
    }

    @Override
    public void virtualize(VirtualizerTool tool)
    {
        ValueNode lengthAlias = tool.getAlias(length());
        if (lengthAlias.asConstant() != null)
        {
            int constantLength = lengthAlias.asJavaConstant().asInt();
            if (constantLength >= 0 && constantLength < tool.getMaximumEntryCount())
            {
                ValueNode[] state = new ValueNode[constantLength];
                ConstantNode defaultForKind = constantLength == 0 ? null : defaultElementValue();
                for (int i = 0; i < constantLength; i++)
                {
                    state[i] = defaultForKind;
                }
                VirtualObjectNode virtualObject = createVirtualArrayNode(constantLength);
                tool.createVirtualObject(virtualObject, state, Collections.<MonitorIdNode> emptyList(), false);
                tool.replaceWithVirtual(virtualObject);
            }
        }
    }

    protected VirtualArrayNode createVirtualArrayNode(int constantLength)
    {
        return new VirtualArrayNode(elementType(), constantLength);
    }

    // Factored out in a separate method so that subclasses can override it.
    protected ConstantNode defaultElementValue()
    {
        return ConstantNode.defaultForKind(elementType().getJavaKind(), graph());
    }

    @Override
    public void simplify(SimplifierTool tool)
    {
        if (hasNoUsages())
        {
            NodeView view = NodeView.from(tool);
            Stamp lengthStamp = length().stamp(view);
            if (lengthStamp instanceof IntegerStamp)
            {
                IntegerStamp lengthIntegerStamp = (IntegerStamp) lengthStamp;
                if (lengthIntegerStamp.isPositive())
                {
                    GraphUtil.removeFixedWithUnusedInputs(this);
                    return;
                }
            }
            // should be areFrameStatesAtSideEffects, but currently SVM will complain about RuntimeConstraint
            if (graph().getGuardsStage().allowsFloatingGuards())
            {
                LogicNode lengthNegativeCondition = CompareNode.createCompareNode(graph(), CanonicalCondition.LT, length(), ConstantNode.forInt(0, graph()), tool.getConstantReflection(), view);
                // we do not have a non-deopting path for that at the moment so action=None.
                FixedGuardNode guard = graph().add(new FixedGuardNode(lengthNegativeCondition, DeoptimizationReason.RuntimeConstraint, DeoptimizationAction.None, true));
                graph().replaceFixedWithFixed(this, guard);
            }
        }
    }
}
