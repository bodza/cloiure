package giraaff.nodes.calc;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;

/**
 * The {@code BinaryNode} class is the base of arithmetic and logic operations with two inputs.
 */
// @class BinaryNode
public abstract class BinaryNode extends FloatingNode implements Canonicalizable.Binary<ValueNode>
{
    public static final NodeClass<BinaryNode> TYPE = NodeClass.create(BinaryNode.class);

    @Input protected ValueNode x;
    @Input protected ValueNode y;

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

    public void setX(ValueNode x)
    {
        updateUsages(this.x, x);
        this.x = x;
    }

    public void setY(ValueNode y)
    {
        updateUsages(this.y, y);
        this.y = y;
    }

    /**
     * Creates a new BinaryNode instance.
     *
     * @param stamp the result type of this instruction
     * @param x the first input instruction
     * @param y the second input instruction
     */
    // @cons
    protected BinaryNode(NodeClass<? extends BinaryNode> c, Stamp stamp, ValueNode x, ValueNode y)
    {
        super(c, stamp);
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean inferStamp()
    {
        return updateStamp(foldStamp(getX().stamp(NodeView.DEFAULT), getY().stamp(NodeView.DEFAULT)));
    }

    /**
     * Compute an improved for this node using the passed in stamps. The stamps must be compatible
     * with the current values of {@link #x} and {@link #y}. This code is used to provide the
     * default implementation of {@link #inferStamp()} and may be used by external optimizations.
     */
    public abstract Stamp foldStamp(Stamp stampX, Stamp stampY);
}
