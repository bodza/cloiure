package giraaff.graph;

import java.util.ArrayList;

import giraaff.graph.Edges;
import giraaff.graph.NodeClass;

// @class SuccessorEdges
public final class SuccessorEdges extends Edges
{
    // @cons SuccessorEdges
    public SuccessorEdges(int __directCount, ArrayList<NodeClass.EdgeInfo> __edges)
    {
        super(Edges.EdgesType.Successors, __directCount, __edges);
    }

    @Override
    public void update(Node __node, Node __oldValue, Node __newValue)
    {
        __node.updatePredecessor(__oldValue, __newValue);
    }
}
