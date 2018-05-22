package giraaff.nodes.spi;

import giraaff.graph.iterators.NodeIterable;
import giraaff.nodes.FixedNodeInterface;
import giraaff.nodes.FrameState;

/**
 * Interface for nodes which have {@link FrameState} nodes as input.
 */
public interface NodeWithState extends FixedNodeInterface
{
    default NodeIterable<FrameState> states()
    {
        return asNode().inputs().filter(FrameState.class);
    }
}
