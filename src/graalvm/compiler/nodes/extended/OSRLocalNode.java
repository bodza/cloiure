package graalvm.compiler.nodes.extended;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.IterableNodeType;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.AbstractLocalNode;

@NodeInfo(nameTemplate = "OSRLocal({p#index})")
public final class OSRLocalNode extends AbstractLocalNode implements IterableNodeType {

    public static final NodeClass<OSRLocalNode> TYPE = NodeClass.create(OSRLocalNode.class);

    public OSRLocalNode(int index, Stamp stamp) {
        super(TYPE, index, stamp);
    }

}
