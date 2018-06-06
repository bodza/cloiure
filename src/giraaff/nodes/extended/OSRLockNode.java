package giraaff.nodes.extended;

import giraaff.core.common.type.Stamp;
import giraaff.graph.IterableNodeType;
import giraaff.graph.NodeClass;
import giraaff.nodes.AbstractLocalNode;

// @class OSRLockNode
public final class OSRLockNode extends AbstractLocalNode implements IterableNodeType
{
    // @def
    public static final NodeClass<OSRLockNode> TYPE = NodeClass.create(OSRLockNode.class);

    // @cons OSRLockNode
    public OSRLockNode(int __index, Stamp __stamp)
    {
        super(TYPE, __index, __stamp);
    }
}
