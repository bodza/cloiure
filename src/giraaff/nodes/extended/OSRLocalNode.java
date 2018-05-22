package giraaff.nodes.extended;

import giraaff.core.common.type.Stamp;
import giraaff.graph.IterableNodeType;
import giraaff.graph.NodeClass;
import giraaff.nodes.AbstractLocalNode;

public final class OSRLocalNode extends AbstractLocalNode implements IterableNodeType
{
    public static final NodeClass<OSRLocalNode> TYPE = NodeClass.create(OSRLocalNode.class);

    public OSRLocalNode(int index, Stamp stamp)
    {
        super(TYPE, index, stamp);
    }
}
