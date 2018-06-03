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
    // @def
    public static final NodeClass<StoreFieldNode> TYPE = NodeClass.create(StoreFieldNode.class);

    @Input
    // @field
    ValueNode value;
    @OptionalInput(InputType.State)
    // @field
    FrameState stateAfter;

    @Override
    public FrameState stateAfter()
    {
        return stateAfter;
    }

    @Override
    public void setStateAfter(FrameState __x)
    {
        updateUsages(stateAfter, __x);
        stateAfter = __x;
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
    public StoreFieldNode(ValueNode __object, ResolvedJavaField __field, ValueNode __value)
    {
        super(TYPE, StampFactory.forVoid(), __object, __field);
        this.value = __value;
    }

    // @cons
    public StoreFieldNode(ValueNode __object, ResolvedJavaField __field, ValueNode __value, FrameState __stateAfter)
    {
        super(TYPE, StampFactory.forVoid(), __object, __field);
        this.value = __value;
        this.stateAfter = __stateAfter;
    }

    @Override
    public void virtualize(VirtualizerTool __tool)
    {
        ValueNode __alias = __tool.getAlias(object());
        if (__alias instanceof VirtualObjectNode)
        {
            VirtualInstanceNode __virtual = (VirtualInstanceNode) __alias;
            int __fieldIndex = __virtual.fieldIndex(field());
            if (__fieldIndex != -1)
            {
                __tool.setVirtualEntry(__virtual, __fieldIndex, value());
                __tool.delete();
            }
        }
    }

    public FrameState getState()
    {
        return stateAfter;
    }
}
