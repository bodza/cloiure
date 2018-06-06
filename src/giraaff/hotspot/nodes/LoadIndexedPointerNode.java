package giraaff.hotspot.nodes;

import jdk.vm.ci.meta.JavaKind;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.nodes.ValueNode;
import giraaff.nodes.java.LoadIndexedNode;

// @class LoadIndexedPointerNode
public final class LoadIndexedPointerNode extends LoadIndexedNode
{
    // @def
    public static final NodeClass<LoadIndexedPointerNode> TYPE = NodeClass.create(LoadIndexedPointerNode.class);

    // @cons LoadIndexedPointerNode
    public LoadIndexedPointerNode(Stamp __stamp, ValueNode __array, ValueNode __index)
    {
        super(TYPE, __stamp, __array, __index, JavaKind.Illegal);
    }

    @Override
    public boolean inferStamp()
    {
        return false;
    }
}
