package graalvm.compiler.nodes.calc;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.Node.ValueNumberable;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodes.ValueNode;

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
