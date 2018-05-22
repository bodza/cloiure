package giraaff.nodes.calc;

import giraaff.core.common.type.Stamp;
import giraaff.graph.Node.ValueNumberable;
import giraaff.graph.NodeClass;
import giraaff.nodes.ValueNode;

public abstract class FloatingNode extends ValueNode implements ValueNumberable
{
    public static final NodeClass<FloatingNode> TYPE = NodeClass.create(FloatingNode.class);

    public FloatingNode(NodeClass<? extends FloatingNode> c, Stamp stamp)
    {
        super(c, stamp);
    }

    @Override
    public FloatingNode asNode()
    {
        return this;
    }
}
