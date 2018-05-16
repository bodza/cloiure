package graalvm.compiler.nodes.spi;

import graalvm.compiler.graph.iterators.NodeIterable;
import graalvm.compiler.nodes.FixedNodeInterface;
import graalvm.compiler.nodes.FrameState;

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
