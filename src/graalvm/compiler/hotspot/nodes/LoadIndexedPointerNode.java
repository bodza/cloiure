package graalvm.compiler.hotspot.nodes;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.java.LoadIndexedNode;

import jdk.vm.ci.meta.JavaKind;

@NodeInfo
public final class LoadIndexedPointerNode extends LoadIndexedNode {

    public static final NodeClass<LoadIndexedPointerNode> TYPE = NodeClass.create(LoadIndexedPointerNode.class);

    public LoadIndexedPointerNode(Stamp stamp, ValueNode array, ValueNode index) {
        super(TYPE, stamp, array, index, JavaKind.Illegal);
    }

    @Override
    public boolean inferStamp() {
        return false;
    }
}
