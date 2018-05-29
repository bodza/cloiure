package giraaff.nodes;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;

// @class ControlSinkNode
public abstract class ControlSinkNode extends FixedNode
{
    public static final NodeClass<ControlSinkNode> TYPE = NodeClass.create(ControlSinkNode.class);

    // @cons
    protected ControlSinkNode(NodeClass<? extends ControlSinkNode> c, Stamp stamp)
    {
        super(c, stamp);
    }
}
