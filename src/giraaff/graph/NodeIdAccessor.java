package giraaff.graph;

///
// An entity that depends upon {@linkplain Graph#maybeCompress() stable} node identifiers.
///
class NodeIdAccessor
{
    // @field
    final Graph ___graph;
    // @field
    final int ___epoch;

    NodeIdAccessor(Graph __graph)
    {
        this.___graph = __graph;
        this.___epoch = __graph.___compressions;
    }

    Graph getGraph()
    {
        return this.___graph;
    }

    ///
    // Gets the identifier for a node. If assertions are enabled, this method asserts that the
    // identifier is stable.
    ///
    int getNodeId(Node __node)
    {
        return __node.id();
    }
}
