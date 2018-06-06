package giraaff.nodes.calc;

import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.nodes.ValueNode;

// @class FloatingNode
public abstract class FloatingNode extends ValueNode implements Node.ValueNumberable
{
    // @def
    public static final NodeClass<FloatingNode> TYPE = NodeClass.create(FloatingNode.class);

    // @cons FloatingNode
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
