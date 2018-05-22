package giraaff.nodes.calc;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;

/**
 * The {@code UnaryNode} class is the base of arithmetic and bit logic operations with exactly one
 * input.
 */
public abstract class UnaryNode extends FloatingNode implements Canonicalizable.Unary<ValueNode>
{
    public static final NodeClass<UnaryNode> TYPE = NodeClass.create(UnaryNode.class);
    @Input protected ValueNode value;

    @Override
    public ValueNode getValue()
    {
        return value;
    }

    public void setValue(ValueNode value)
    {
        updateUsages(this.value, value);
        this.value = value;
    }

    /**
     * Creates a new UnaryNode instance.
     *
     * @param stamp the result type of this instruction
     * @param value the input instruction
     */
    protected UnaryNode(NodeClass<? extends UnaryNode> c, Stamp stamp, ValueNode value)
    {
        super(c, stamp);
        this.value = value;
    }

    @Override
    public boolean inferStamp()
    {
        return updateStamp(foldStamp(value.stamp(NodeView.DEFAULT)));
    }

    /**
     * Compute an improved for this node using the passed in stamp. The stamp must be compatible
     * with the current value of {@link #value}. This code is used to provide the default
     * implementation of {@link #inferStamp()} and may be used by external optimizations.
     */
    public Stamp foldStamp(Stamp newStamp)
    {
        return stamp(NodeView.DEFAULT);
    }
}
