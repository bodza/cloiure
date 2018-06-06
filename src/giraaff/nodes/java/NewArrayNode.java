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
import giraaff.graph.Node;
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

///
// The {@code NewArrayNode} is used for all array allocations where the element type is know at
// compile time.
///
// @class NewArrayNode
public final class NewArrayNode extends AbstractNewArrayNode implements VirtualizableAllocation, Simplifiable
{
    // @def
    public static final NodeClass<NewArrayNode> TYPE = NodeClass.create(NewArrayNode.class);

    // @field
    private final ResolvedJavaType ___elementType;

    // @cons NewArrayNode
    public NewArrayNode(ResolvedJavaType __elementType, ValueNode __length, boolean __fillContents)
    {
        this(__elementType, __length, __fillContents, null);
    }

    // @cons NewArrayNode
    public NewArrayNode(ResolvedJavaType __elementType, ValueNode __length, boolean __fillContents, FrameState __stateBefore)
    {
        this(TYPE, __elementType, __length, __fillContents, __stateBefore);
    }

    // @cons NewArrayNode
    protected NewArrayNode(NodeClass<? extends NewArrayNode> __c, ResolvedJavaType __elementType, ValueNode __length, boolean __fillContents, FrameState __stateBefore)
    {
        super(__c, StampFactory.objectNonNull(TypeReference.createExactTrusted(__elementType.getArrayClass())), __length, __fillContents, __stateBefore);
        this.___elementType = __elementType;
    }

    @Node.NodeIntrinsic
    private static native Object newArray(@Node.ConstantNodeParameter Class<?> __elementType, int __length, @Node.ConstantNodeParameter boolean __fillContents);

    public static Object newUninitializedArray(Class<?> __elementType, int __length)
    {
        return newArray(__elementType, __length, false);
    }

    ///
    // Gets the element type of the array.
    //
    // @return the element type of the array
    ///
    public ResolvedJavaType elementType()
    {
        return this.___elementType;
    }

    @Override
    public void virtualize(VirtualizerTool __tool)
    {
        ValueNode __lengthAlias = __tool.getAlias(length());
        if (__lengthAlias.asConstant() != null)
        {
            int __constantLength = __lengthAlias.asJavaConstant().asInt();
            if (__constantLength >= 0 && __constantLength < __tool.getMaximumEntryCount())
            {
                ValueNode[] __state = new ValueNode[__constantLength];
                ConstantNode __defaultForKind = __constantLength == 0 ? null : defaultElementValue();
                for (int __i = 0; __i < __constantLength; __i++)
                {
                    __state[__i] = __defaultForKind;
                }
                VirtualObjectNode __virtualObject = createVirtualArrayNode(__constantLength);
                __tool.createVirtualObject(__virtualObject, __state, Collections.<MonitorIdNode> emptyList(), false);
                __tool.replaceWithVirtual(__virtualObject);
            }
        }
    }

    protected VirtualArrayNode createVirtualArrayNode(int __constantLength)
    {
        return new VirtualArrayNode(elementType(), __constantLength);
    }

    // Factored out in a separate method so that subclasses can override it.
    protected ConstantNode defaultElementValue()
    {
        return ConstantNode.defaultForKind(elementType().getJavaKind(), graph());
    }

    @Override
    public void simplify(SimplifierTool __tool)
    {
        if (hasNoUsages())
        {
            NodeView __view = NodeView.from(__tool);
            Stamp __lengthStamp = length().stamp(__view);
            if (__lengthStamp instanceof IntegerStamp)
            {
                IntegerStamp __lengthIntegerStamp = (IntegerStamp) __lengthStamp;
                if (__lengthIntegerStamp.isPositive())
                {
                    GraphUtil.removeFixedWithUnusedInputs(this);
                    return;
                }
            }
            // should be areFrameStatesAtSideEffects, but currently SVM will complain about RuntimeConstraint
            if (graph().getGuardsStage().allowsFloatingGuards())
            {
                LogicNode __lengthNegativeCondition = CompareNode.createCompareNode(graph(), CanonicalCondition.LT, length(), ConstantNode.forInt(0, graph()), __tool.getConstantReflection(), __view);
                // we do not have a non-deopting path for that at the moment so action=None.
                FixedGuardNode __guard = graph().add(new FixedGuardNode(__lengthNegativeCondition, DeoptimizationReason.RuntimeConstraint, DeoptimizationAction.None, true));
                graph().replaceFixedWithFixed(this, __guard);
            }
        }
    }
}
