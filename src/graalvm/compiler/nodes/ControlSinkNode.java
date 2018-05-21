package graalvm.compiler.nodes;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;

public abstract class ControlSinkNode extends FixedNode
{
    public static final NodeClass<ControlSinkNode> TYPE = NodeClass.create(ControlSinkNode.class);

    protected ControlSinkNode(NodeClass<? extends ControlSinkNode> c, Stamp stamp)
    {
        super(c, stamp);
    }
}
