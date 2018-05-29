package giraaff.hotspot.nodes;

import jdk.vm.ci.meta.JavaKind;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.nodes.ValueNode;
import giraaff.nodes.java.LoadIndexedNode;

// @class LoadIndexedPointerNode
public final class LoadIndexedPointerNode extends LoadIndexedNode
{
    public static final NodeClass<LoadIndexedPointerNode> TYPE = NodeClass.create(LoadIndexedPointerNode.class);

    // @cons
    public LoadIndexedPointerNode(Stamp stamp, ValueNode array, ValueNode index)
    {
        super(TYPE, stamp, array, index, JavaKind.Illegal);
    }

    @Override
    public boolean inferStamp()
    {
        return false;
    }
}
