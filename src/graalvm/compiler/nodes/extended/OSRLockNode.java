package graalvm.compiler.nodes.extended;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.IterableNodeType;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.AbstractLocalNode;

@NodeInfo(nameTemplate = "OSRLock({p#index})")
public class OSRLockNode extends AbstractLocalNode implements IterableNodeType {

    public static final NodeClass<OSRLockNode> TYPE = NodeClass.create(OSRLockNode.class);

    public OSRLockNode(int index, Stamp stamp) {
        super(TYPE, index, stamp);
    }
}
