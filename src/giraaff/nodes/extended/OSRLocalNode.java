package giraaff.nodes.extended;

import giraaff.core.common.type.Stamp;
import giraaff.graph.IterableNodeType;
import giraaff.graph.NodeClass;
import giraaff.nodes.AbstractLocalNode;

// @class OSRLocalNode
public final class OSRLocalNode extends AbstractLocalNode implements IterableNodeType
{
    // @def
    public static final NodeClass<OSRLocalNode> TYPE = NodeClass.create(OSRLocalNode.class);

    // @cons OSRLocalNode
    public OSRLocalNode(int __index, Stamp __stamp)
    {
        super(TYPE, __index, __stamp);
    }
}
