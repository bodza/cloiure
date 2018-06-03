package giraaff.nodes.calc;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;

///
// The {@code BinaryNode} class is the base of arithmetic and logic operations with two inputs.
///
// @class BinaryNode
public abstract class BinaryNode extends FloatingNode implements Canonicalizable.Binary<ValueNode>
{
    // @def
    public static final NodeClass<BinaryNode> TYPE = NodeClass.create(BinaryNode.class);

    @Input
    // @field
    protected ValueNode ___x;
    @Input
    // @field
    protected ValueNode ___y;

    @Override
    public ValueNode getX()
    {
        return this.___x;
    }

    @Override
    public ValueNode getY()
    {
        return this.___y;
    }

    public void setX(ValueNode __x)
    {
        updateUsages(this.___x, __x);
        this.___x = __x;
    }

    public void setY(ValueNode __y)
    {
        updateUsages(this.___y, __y);
        this.___y = __y;
    }

    ///
    // Creates a new BinaryNode instance.
    //
    // @param stamp the result type of this instruction
    // @param x the first input instruction
    // @param y the second input instruction
    ///
    // @cons
    protected BinaryNode(NodeClass<? extends BinaryNode> __c, Stamp __stamp, ValueNode __x, ValueNode __y)
    {
        super(__c, __stamp);
        this.___x = __x;
        this.___y = __y;
    }

    @Override
    public boolean inferStamp()
    {
        return updateStamp(foldStamp(getX().stamp(NodeView.DEFAULT), getY().stamp(NodeView.DEFAULT)));
    }

    ///
    // Compute an improved for this node using the passed in stamps. The stamps must be compatible
    // with the current values of {@link #x} and {@link #y}. This code is used to provide the
    // default implementation of {@link #inferStamp()} and may be used by external optimizations.
    ///
    public abstract Stamp foldStamp(Stamp __stampX, Stamp __stampY);
}
