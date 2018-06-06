package giraaff.nodes.java;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
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

///
// The {@code StoreIndexedNode} represents a write to an array element.
///
// @class StoreIndexedNode
public final class StoreIndexedNode extends AccessIndexedNode implements StateSplit, Lowerable, Virtualizable
{
    // @def
    public static final NodeClass<StoreIndexedNode> TYPE = NodeClass.create(StoreIndexedNode.class);

    @Node.Input
    // @field
    ValueNode ___value;
    @Node.OptionalInput(InputType.StateI)
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

    // @cons StoreIndexedNode
    public StoreIndexedNode(ValueNode __array, ValueNode __index, JavaKind __elementKind, ValueNode __value)
    {
        super(TYPE, StampFactory.forVoid(), __array, __index, __elementKind);
        this.___value = __value;
    }

    @Override
    public void virtualize(VirtualizerTool __tool)
    {
        ValueNode __alias = __tool.getAlias(array());
        if (__alias instanceof VirtualObjectNode)
        {
            ValueNode __indexValue = __tool.getAlias(index());
            int __idx = __indexValue.isConstant() ? __indexValue.asJavaConstant().asInt() : -1;
            VirtualArrayNode __virtual = (VirtualArrayNode) __alias;
            if (__idx >= 0 && __idx < __virtual.entryCount())
            {
                ResolvedJavaType __componentType = __virtual.type().getComponentType();
                if (__componentType.isPrimitive() || StampTool.isPointerAlwaysNull(this.___value) || __componentType.getSuperclass() == null || (StampTool.typeReferenceOrNull(this.___value) != null && __componentType.isAssignableFrom(StampTool.typeOrNull(this.___value))))
                {
                    __tool.setVirtualEntry(__virtual, __idx, value());
                    __tool.delete();
                }
            }
        }
    }

    public FrameState getState()
    {
        return this.___stateAfter;
    }
}
