package giraaff.nodes.java;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.nodes.FrameState;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.ArrayLengthProvider;

///
// The {@code AbstractNewArrayNode} is used for all 1-dimensional array allocations.
///
// @class AbstractNewArrayNode
public abstract class AbstractNewArrayNode extends AbstractNewObjectNode implements ArrayLengthProvider
{
    // @def
    public static final NodeClass<AbstractNewArrayNode> TYPE = NodeClass.create(AbstractNewArrayNode.class);

    @Input
    // @field
    protected ValueNode ___length;

    @Override
    public ValueNode length()
    {
        return this.___length;
    }

    // @cons
    protected AbstractNewArrayNode(NodeClass<? extends AbstractNewArrayNode> __c, Stamp __stamp, ValueNode __length, boolean __fillContents, FrameState __stateBefore)
    {
        super(__c, __stamp, __fillContents, __stateBefore);
        this.___length = __length;
    }

    ///
    // The list of node which produce input for this instruction.
    ///
    public ValueNode dimension(int __index)
    {
        return length();
    }

    ///
    // The rank of the array allocated by this node, i.e. how many array dimensions.
    ///
    public int dimensionCount()
    {
        return 1;
    }
}
