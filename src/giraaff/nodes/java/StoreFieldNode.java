package giraaff.nodes.java;

import jdk.vm.ci.meta.ResolvedJavaField;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.InputType;
import giraaff.nodes.FrameState;
import giraaff.nodes.StateSplit;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.Virtualizable;
import giraaff.nodes.spi.VirtualizerTool;
import giraaff.nodes.virtual.VirtualInstanceNode;
import giraaff.nodes.virtual.VirtualObjectNode;

/**
 * The {@code StoreFieldNode} represents a write to a static or instance field.
 */
// @class StoreFieldNode
public final class StoreFieldNode extends AccessFieldNode implements StateSplit, Virtualizable
{
    public static final NodeClass<StoreFieldNode> TYPE = NodeClass.create(StoreFieldNode.class);

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

    // @cons
    public StoreFieldNode(ValueNode object, ResolvedJavaField field, ValueNode value)
    {
        super(TYPE, StampFactory.forVoid(), object, field);
        this.value = value;
    }

    // @cons
    public StoreFieldNode(ValueNode object, ResolvedJavaField field, ValueNode value, FrameState stateAfter)
    {
        super(TYPE, StampFactory.forVoid(), object, field);
        this.value = value;
        this.stateAfter = stateAfter;
    }

    @Override
    public void virtualize(VirtualizerTool tool)
    {
        ValueNode alias = tool.getAlias(object());
        if (alias instanceof VirtualObjectNode)
        {
            VirtualInstanceNode virtual = (VirtualInstanceNode) alias;
            int fieldIndex = virtual.fieldIndex(field());
            if (fieldIndex != -1)
            {
                tool.setVirtualEntry(virtual, fieldIndex, value());
                tool.delete();
            }
        }
    }

    public FrameState getState()
    {
        return stateAfter;
    }
}
