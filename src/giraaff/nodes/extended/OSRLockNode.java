package giraaff.nodes.extended;

import giraaff.core.common.type.Stamp;
import giraaff.graph.IterableNodeType;
import giraaff.graph.NodeClass;
import giraaff.nodes.AbstractLocalNode;

public class OSRLockNode extends AbstractLocalNode implements IterableNodeType
{
    public static final NodeClass<OSRLockNode> TYPE = NodeClass.create(OSRLockNode.class);

    public OSRLockNode(int index, Stamp stamp)
    {
        super(TYPE, index, stamp);
    }
}
