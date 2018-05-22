package graalvm.compiler.hotspot.nodes;

import jdk.vm.ci.meta.JavaKind;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.java.LoadIndexedNode;

public final class LoadIndexedPointerNode extends LoadIndexedNode
{
    public static final NodeClass<LoadIndexedPointerNode> TYPE = NodeClass.create(LoadIndexedPointerNode.class);

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
