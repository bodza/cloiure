package giraaff.nodes;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;

// @class ControlSinkNode
public abstract class ControlSinkNode extends FixedNode
{
    // @def
    public static final NodeClass<ControlSinkNode> TYPE = NodeClass.create(ControlSinkNode.class);

    // @cons ControlSinkNode
    protected ControlSinkNode(NodeClass<? extends ControlSinkNode> __c, Stamp __stamp)
    {
        super(__c, __stamp);
    }
}
