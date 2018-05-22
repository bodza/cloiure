package giraaff.nodes.debug;

import giraaff.graph.NodeClass;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

public final class OpaqueNode extends FloatingNode implements LIRLowerable
{
    public static final NodeClass<OpaqueNode> TYPE = NodeClass.create(OpaqueNode.class);
    @Input protected ValueNode value;

    public OpaqueNode(ValueNode value)
    {
        super(TYPE, value.stamp(NodeView.DEFAULT).unrestricted());
        this.value = value;
    }

    public ValueNode getValue()
    {
        return value;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        gen.setResult(this, gen.operand(value));
    }
}
