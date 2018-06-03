package giraaff.nodes.calc;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;

///
// The {@code UnaryNode} class is the base of arithmetic and bit logic operations with exactly one input.
///
// @class UnaryNode
public abstract class UnaryNode extends FloatingNode implements Canonicalizable.Unary<ValueNode>
{
    // @def
    public static final NodeClass<UnaryNode> TYPE = NodeClass.create(UnaryNode.class);

    @Input
    // @field
    protected ValueNode ___value;

    @Override
    public ValueNode getValue()
    {
        return this.___value;
    }

    public void setValue(ValueNode __value)
    {
        updateUsages(this.___value, __value);
        this.___value = __value;
    }

    ///
    // Creates a new UnaryNode instance.
    //
    // @param stamp the result type of this instruction
    // @param value the input instruction
    ///
    // @cons
    protected UnaryNode(NodeClass<? extends UnaryNode> __c, Stamp __stamp, ValueNode __value)
    {
        super(__c, __stamp);
        this.___value = __value;
    }

    @Override
    public boolean inferStamp()
    {
        return updateStamp(foldStamp(this.___value.stamp(NodeView.DEFAULT)));
    }

    ///
    // Compute an improved for this node using the passed in stamp. The stamp must be compatible
    // with the current value of {@link #value}. This code is used to provide the default
    // implementation of {@link #inferStamp()} and may be used by external optimizations.
    ///
    public Stamp foldStamp(Stamp __newStamp)
    {
        return stamp(NodeView.DEFAULT);
    }
}
