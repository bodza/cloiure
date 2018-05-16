package graalvm.compiler.nodes.calc;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.Canonicalizable;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.DeoptimizingFixedWithNextNode;
import graalvm.compiler.nodes.ValueNode;

@NodeInfo
public abstract class FixedBinaryNode extends DeoptimizingFixedWithNextNode implements Canonicalizable.Binary<ValueNode>
{
    public static final NodeClass<FixedBinaryNode> TYPE = NodeClass.create(FixedBinaryNode.class);

    @Input protected ValueNode x;
    @Input protected ValueNode y;

    public FixedBinaryNode(NodeClass<? extends FixedBinaryNode> c, Stamp stamp, ValueNode x, ValueNode y)
    {
        super(c, stamp);
        this.x = x;
        this.y = y;
    }

    @Override
    public ValueNode getX()
    {
        return x;
    }

    @Override
    public ValueNode getY()
    {
        return y;
    }
}
