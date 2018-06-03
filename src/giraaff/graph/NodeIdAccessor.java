package giraaff.graph;

/**
 * An entity that depends upon {@linkplain Graph#maybeCompress() stable} node identifiers.
 */
class NodeIdAccessor
{
    // @field
    final Graph graph;
    // @field
    final int epoch;

    NodeIdAccessor(Graph __graph)
    {
        this.graph = __graph;
        this.epoch = __graph.compressions;
    }

    Graph getGraph()
    {
        return graph;
    }

    /**
     * Gets the identifier for a node. If assertions are enabled, this method asserts that the
     * identifier is stable.
     */
    int getNodeId(Node __node)
    {
        return __node.id();
    }
}
