package giraaff.nodes.extended;

import java.util.Collections;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.TypeReference;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.java.MonitorIdNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.spi.VirtualizableAllocation;
import giraaff.nodes.spi.VirtualizerTool;
import giraaff.nodes.type.StampTool;
import giraaff.nodes.virtual.VirtualBoxingNode;

///
// This node represents the boxing of a primitive value. This corresponds to a call to the valueOf
// methods in Integer, Long, etc.
///
// @class BoxNode
public final class BoxNode extends FixedWithNextNode implements VirtualizableAllocation, Lowerable, Canonicalizable.Unary<ValueNode>
{
    // @def
    public static final NodeClass<BoxNode> TYPE = NodeClass.create(BoxNode.class);

    @Node.Input
    // @field
    private ValueNode ___value;
    // @field
    protected final JavaKind ___boxingKind;

    // @cons BoxNode
    public BoxNode(ValueNode __value, ResolvedJavaType __resultType, JavaKind __boxingKind)
    {
        this(TYPE, __value, __resultType, __boxingKind);
    }

    // @cons BoxNode
    public BoxNode(NodeClass<? extends BoxNode> __c, ValueNode __value, ResolvedJavaType __resultType, JavaKind __boxingKind)
    {
        super(__c, StampFactory.objectNonNull(TypeReference.createExactTrusted(__resultType)));
        this.___value = __value;
        this.___boxingKind = __boxingKind;
    }

    public JavaKind getBoxingKind()
    {
        return this.___boxingKind;
    }

    @Override
    public ValueNode getValue()
    {
        return this.___value;
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forValue)
    {
        if (__tool.allUsagesAvailable() && hasNoUsages())
        {
            return null;
        }
        return this;
    }

    protected VirtualBoxingNode createVirtualBoxingNode()
    {
        return new VirtualBoxingNode(StampTool.typeOrNull(stamp(NodeView.DEFAULT)), this.___boxingKind);
    }

    @Override
    public void virtualize(VirtualizerTool __tool)
    {
        ValueNode __alias = __tool.getAlias(getValue());

        VirtualBoxingNode __newVirtual = createVirtualBoxingNode();

        __tool.createVirtualObject(__newVirtual, new ValueNode[] { __alias }, Collections.<MonitorIdNode> emptyList(), false);
        __tool.replaceWithVirtual(__newVirtual);
    }
}
