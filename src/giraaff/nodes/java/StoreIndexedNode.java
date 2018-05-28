package giraaff.nodes.java;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.FrameState;
import giraaff.nodes.StateSplit;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.Virtualizable;
import giraaff.nodes.spi.VirtualizerTool;
import giraaff.nodes.type.StampTool;
import giraaff.nodes.virtual.VirtualArrayNode;
import giraaff.nodes.virtual.VirtualObjectNode;

/**
 * The {@code StoreIndexedNode} represents a write to an array element.
 */
public final class StoreIndexedNode extends AccessIndexedNode implements StateSplit, Lowerable, Virtualizable
{
    public static final NodeClass<StoreIndexedNode> TYPE = NodeClass.create(StoreIndexedNode.class);

    @Input ValueNode value;
    @OptionalInput(InputType.State) FrameState stateAfter;

    @Override
    public FrameState stateAfter()
    {
        return stateAfter;
    }

    @Override
    public void setStateAfter(FrameState x)
    {
        updateUsages(stateAfter, x);
        stateAfter = x;
    }

    @Override
    public boolean hasSideEffect()
    {
        return true;
    }

    public ValueNode value()
    {
        return value;
    }

    public StoreIndexedNode(ValueNode array, ValueNode index, JavaKind elementKind, ValueNode value)
    {
        super(TYPE, StampFactory.forVoid(), array, index, elementKind);
        this.value = value;
    }

    @Override
    public void virtualize(VirtualizerTool tool)
    {
        ValueNode alias = tool.getAlias(array());
        if (alias instanceof VirtualObjectNode)
        {
            ValueNode indexValue = tool.getAlias(index());
            int idx = indexValue.isConstant() ? indexValue.asJavaConstant().asInt() : -1;
            VirtualArrayNode virtual = (VirtualArrayNode) alias;
            if (idx >= 0 && idx < virtual.entryCount())
            {
                ResolvedJavaType componentType = virtual.type().getComponentType();
                if (componentType.isPrimitive() || StampTool.isPointerAlwaysNull(value) || componentType.getSuperclass() == null || (StampTool.typeReferenceOrNull(value) != null && componentType.isAssignableFrom(StampTool.typeOrNull(value))))
                {
                    tool.setVirtualEntry(virtual, idx, value());
                    tool.delete();
                }
            }
        }
    }

    public FrameState getState()
    {
        return stateAfter;
    }
}
