package giraaff.nodes.java;

import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.nodes.FrameState;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.ArrayLengthProvider;

/**
 * The {@code AbstractNewArrayNode} is used for all 1-dimensional array allocations.
 */
// @class AbstractNewArrayNode
public abstract class AbstractNewArrayNode extends AbstractNewObjectNode implements ArrayLengthProvider
{
    public static final NodeClass<AbstractNewArrayNode> TYPE = NodeClass.create(AbstractNewArrayNode.class);

    @Input protected ValueNode length;

    @Override
    public ValueNode length()
    {
        return length;
    }

    // @cons
    protected AbstractNewArrayNode(NodeClass<? extends AbstractNewArrayNode> c, Stamp stamp, ValueNode length, boolean fillContents, FrameState stateBefore)
    {
        super(c, stamp, fillContents, stateBefore);
        this.length = length;
    }

    /**
     * The list of node which produce input for this instruction.
     */
    public ValueNode dimension(int index)
    {
        return length();
    }

    /**
     * The rank of the array allocated by this node, i.e. how many array dimensions.
     */
    public int dimensionCount()
    {
        return 1;
    }
}
