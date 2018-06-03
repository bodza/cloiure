package giraaff.graph.spi;

import giraaff.graph.Node;

///
// @see Simplifiable
///
// @iface SimplifierTool
public interface SimplifierTool extends CanonicalizerTool
{
    void deleteBranch(Node __branch);

    ///
    // Adds a node to the worklist independent of whether it has already been on the worklist.
    ///
    void addToWorkList(Node __node);

    void addToWorkList(Iterable<? extends Node> __nodes);

    void removeIfUnused(Node __node);
}
