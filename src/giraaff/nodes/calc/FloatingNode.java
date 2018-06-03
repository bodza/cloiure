package giraaff.nodes.calc;

import giraaff.core.common.type.Stamp;
import giraaff.graph.Node.ValueNumberable;
import giraaff.graph.NodeClass;
import giraaff.nodes.ValueNode;

// @class FloatingNode
public abstract class FloatingNode extends ValueNode implements ValueNumberable
{
    // @def
    public static final NodeClass<FloatingNode> TYPE = NodeClass.create(FloatingNode.class);

    // @cons
    public FloatingNode(NodeClass<? extends FloatingNode> __c, Stamp __stamp)
    {
        super(__c, __stamp);
    }

    @Override
    public FloatingNode asNode()
    {
        return this;
    }
}
