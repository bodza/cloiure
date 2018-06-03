package giraaff.graph;

import java.util.ArrayList;

import giraaff.graph.Edges.Type;
import giraaff.graph.NodeClass.EdgeInfo;

// @class SuccessorEdges
public final class SuccessorEdges extends Edges
{
    // @cons
    public SuccessorEdges(int __directCount, ArrayList<EdgeInfo> __edges)
    {
        super(Type.Successors, __directCount, __edges);
    }

    @Override
    public void update(Node __node, Node __oldValue, Node __newValue)
    {
        __node.updatePredecessor(__oldValue, __newValue);
    }
}
