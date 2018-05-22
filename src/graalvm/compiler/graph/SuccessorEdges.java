package graalvm.compiler.graph;

import java.util.ArrayList;

import graalvm.compiler.graph.Edges.Type;
import graalvm.compiler.graph.NodeClass.EdgeInfo;

public final class SuccessorEdges extends Edges
{
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
