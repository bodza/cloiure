package graalvm.compiler.nodes.java;

import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.FrameState;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.spi.ArrayLengthProvider;

/**
 * The {@code AbstractNewArrayNode} is used for all 1-dimensional array allocations.
 */
@NodeInfo
public abstract class AbstractNewArrayNode extends AbstractNewObjectNode implements ArrayLengthProvider {

    public static final NodeClass<AbstractNewArrayNode> TYPE = NodeClass.create(AbstractNewArrayNode.class);
    @Input protected ValueNode length;

    @Override
    public ValueNode length() {
        return length;
    }

    protected AbstractNewArrayNode(NodeClass<? extends AbstractNewArrayNode> c, Stamp stamp, ValueNode length, boolean fillContents, FrameState stateBefore) {
        super(c, stamp, fillContents, stateBefore);
        this.length = length;
    }

    /**
     * The list of node which produce input for this instruction.
     */
    public ValueNode dimension(int index) {
        assert index == 0;
        return length();
    }

    /**
     * The rank of the array allocated by this node, i.e. how many array dimensions.
     */
    public int dimensionCount() {
        return 1;
    }
}
