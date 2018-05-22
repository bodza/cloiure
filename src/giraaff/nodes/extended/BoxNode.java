package giraaff.nodes.extended;

import java.util.Collections;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.TypeReference;
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

/**
 * This node represents the boxing of a primitive value. This corresponds to a call to the valueOf
 * methods in Integer, Long, etc.
 */
public class BoxNode extends FixedWithNextNode implements VirtualizableAllocation, Lowerable, Canonicalizable.Unary<ValueNode>
{
    public static final NodeClass<BoxNode> TYPE = NodeClass.create(BoxNode.class);
    @Input private ValueNode value;
    protected final JavaKind boxingKind;

    public BoxNode(ValueNode value, ResolvedJavaType resultType, JavaKind boxingKind)
    {
        this(TYPE, value, resultType, boxingKind);
    }

    public BoxNode(NodeClass<? extends BoxNode> c, ValueNode value, ResolvedJavaType resultType, JavaKind boxingKind)
    {
        super(c, StampFactory.objectNonNull(TypeReference.createExactTrusted(resultType)));
        this.value = value;
        this.boxingKind = boxingKind;
    }

    public JavaKind getBoxingKind()
    {
        return boxingKind;
    }

    @Override
    public ValueNode getValue()
    {
        return value;
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }

    @Override
    public ValueNode canonical(CanonicalizerTool tool, ValueNode forValue)
    {
        if (tool.allUsagesAvailable() && hasNoUsages())
        {
            return null;
        }
        return this;
    }

    protected VirtualBoxingNode createVirtualBoxingNode()
    {
        return new VirtualBoxingNode(StampTool.typeOrNull(stamp(NodeView.DEFAULT)), boxingKind);
    }

    @Override
    public void virtualize(VirtualizerTool tool)
    {
        ValueNode alias = tool.getAlias(getValue());

        VirtualBoxingNode newVirtual = createVirtualBoxingNode();

        tool.createVirtualObject(newVirtual, new ValueNode[]{alias}, Collections.<MonitorIdNode> emptyList(), false);
        tool.replaceWithVirtual(newVirtual);
    }
}
