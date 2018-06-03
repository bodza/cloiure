package giraaff.nodes.debug;

import giraaff.graph.NodeClass;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @class OpaqueNode
public final class OpaqueNode extends FloatingNode implements LIRLowerable
{
    // @def
    public static final NodeClass<OpaqueNode> TYPE = NodeClass.create(OpaqueNode.class);

    @Input
    // @field
    protected ValueNode value;

    // @cons
    public OpaqueNode(ValueNode __value)
    {
        super(TYPE, __value.stamp(NodeView.DEFAULT).unrestricted());
        this.value = __value;
    }

    public ValueNode getValue()
    {
        return value;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        __gen.setResult(this, __gen.operand(value));
    }
}
