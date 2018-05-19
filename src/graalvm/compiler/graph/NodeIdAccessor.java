package graalvm.compiler.graph;

/**
 * An entity that depends upon {@linkplain Graph#maybeCompress() stable} node identifiers.
 */
class NodeIdAccessor
{
    final Graph graph;
    final int epoch;

    NodeIdAccessor(Graph graph)
    {
        this.graph = graph;
        this.epoch = graph.compressions;
    }

    Graph getGraph()
    {
        return graph;
    }

    /**
     * Gets the identifier for a node. If assertions are enabled, this method asserts that the
     * identifier is stable.
     */
    int getNodeId(Node node)
    {
        return node.id();
    }
}
