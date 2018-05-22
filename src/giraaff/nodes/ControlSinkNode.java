package giraaff.nodes;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;

public abstract class ControlSinkNode extends FixedNode
{
    public static final NodeClass<ControlSinkNode> TYPE = NodeClass.create(ControlSinkNode.class);

    protected ControlSinkNode(NodeClass<? extends ControlSinkNode> c, Stamp stamp)
    {
        super(c, stamp);
    }
}
