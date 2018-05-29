package giraaff.graph;

import java.util.ArrayList;

import giraaff.graph.Edges.Type;
import giraaff.graph.NodeClass.EdgeInfo;

// @class SuccessorEdges
public final class SuccessorEdges extends Edges
{
    // @cons
    public SuccessorEdges(int directCount, ArrayList<EdgeInfo> edges)
    {
        super(Type.Successors, directCount, edges);
    }

    @Override
    public void update(Node node, Node oldValue, Node newValue)
    {
        node.updatePredecessor(oldValue, newValue);
    }
}
