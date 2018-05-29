package giraaff.phases.util;

import java.util.ArrayList;
import java.util.List;

import giraaff.graph.GraalGraphError;
import giraaff.graph.Node;
import giraaff.graph.NodeBitMap;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.EndNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FrameState;
import giraaff.nodes.PhiNode;
import giraaff.nodes.StateSplit;
import giraaff.nodes.StructuredGraph;
import giraaff.phases.graph.StatelessPostOrderNodeIterator;
import giraaff.util.GraalError;

// @class GraphOrder
public final class GraphOrder
{
    // @cons
    private GraphOrder()
    {
        super();
    }

    private static List<Node> createOrder(StructuredGraph graph)
    {
        final ArrayList<Node> nodes = new ArrayList<>();
        final NodeBitMap visited = graph.createNodeBitMap();

        new StatelessPostOrderNodeIterator(graph.start())
        {
            @Override
            protected void node(FixedNode node)
            {
                visitForward(nodes, visited, node, false);
            }
        }.apply();
        return nodes;
    }

    private static void visitForward(ArrayList<Node> nodes, NodeBitMap visited, Node node, boolean floatingOnly)
    {
        try
        {
            if (node != null && !visited.isMarked(node))
            {
                if (floatingOnly && node instanceof FixedNode)
                {
                    throw new GraalError("unexpected reference to fixed node: %s (this indicates an unexpected cycle)", node);
                }
                visited.mark(node);
                FrameState stateAfter = null;
                if (node instanceof StateSplit)
                {
                    stateAfter = ((StateSplit) node).stateAfter();
                }
                for (Node input : node.inputs())
                {
                    if (input != stateAfter)
                    {
                        visitForward(nodes, visited, input, true);
                    }
                }
                if (node instanceof EndNode)
                {
                    EndNode end = (EndNode) node;
                    for (PhiNode phi : end.merge().phis())
                    {
                        visitForward(nodes, visited, phi.valueAt(end), true);
                    }
                }
                nodes.add(node);
                if (node instanceof AbstractMergeNode)
                {
                    for (PhiNode phi : ((AbstractMergeNode) node).phis())
                    {
                        visited.mark(phi);
                        nodes.add(phi);
                    }
                }
                if (stateAfter != null)
                {
                    visitForward(nodes, visited, stateAfter, true);
                }
            }
        }
        catch (GraalError e)
        {
            throw GraalGraphError.transformAndAddContext(e, node);
        }
    }
}
