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

///
// The {@code StoreFieldNode} represents a write to a static or instance field.
///
// @class StoreFieldNode
public final class StoreFieldNode extends AccessFieldNode implements StateSplit, Virtualizable
{
    // @def
    public static final NodeClass<StoreFieldNode> TYPE = NodeClass.create(StoreFieldNode.class);

    @Input
    // @field
    ValueNode ___value;
    @OptionalInput(InputType.State)
    // @field
    FrameState ___stateAfter;

    @Override
    public FrameState stateAfter()
    {
        return this.___stateAfter;
    }

    @Override
    public void setStateAfter(FrameState __x)
    {
        updateUsages(this.___stateAfter, __x);
        this.___stateAfter = __x;
    }

    @Override
    public boolean hasSideEffect()
    {
        return true;
    }

    public ValueNode value()
    {
        return this.___value;
    }

    // @cons
    public StoreFieldNode(ValueNode __object, ResolvedJavaField __field, ValueNode __value)
    {
        super(TYPE, StampFactory.forVoid(), __object, __field);
        this.___value = __value;
    }

    // @cons
    public StoreFieldNode(ValueNode __object, ResolvedJavaField __field, ValueNode __value, FrameState __stateAfter)
    {
        super(TYPE, StampFactory.forVoid(), __object, __field);
        this.___value = __value;
        this.___stateAfter = __stateAfter;
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
        return this.___stateAfter;
    }
}
