package giraaff.nodes.spi;

import giraaff.nodes.ValueNodeInterface;

/**
 * Interface implemented by nodes that can replace themselves with lower level nodes during a phase
 * that transforms a graph to replace higher level nodes with lower level nodes.
 */
// @iface Lowerable
public interface Lowerable extends ValueNodeInterface
{
    /**
     * Expand this node into lower level nodes expressing the same semantics. If the introduced
     * nodes are themselves lowerable, they should be recursively lowered as part of this call.
     */
    void lower(LoweringTool tool);
}
