package graalvm.compiler.phases.util;

import java.util.ArrayList;
import java.util.List;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;
import graalvm.compiler.core.common.cfg.Loop;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.graph.GraalGraphError;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeBitMap;
import graalvm.compiler.nodes.AbstractEndNode;
import graalvm.compiler.nodes.AbstractMergeNode;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.EndNode;
import graalvm.compiler.nodes.FixedNode;
import graalvm.compiler.nodes.FrameState;
import graalvm.compiler.nodes.FullInfopointNode;
import graalvm.compiler.nodes.LoopBeginNode;
import graalvm.compiler.nodes.LoopExitNode;
import graalvm.compiler.nodes.PhiNode;
import graalvm.compiler.nodes.ProxyNode;
import graalvm.compiler.nodes.StateSplit;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.StructuredGraph.GuardsStage;
import graalvm.compiler.nodes.StructuredGraph.ScheduleResult;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.VirtualState;
import graalvm.compiler.nodes.VirtualState.NodeClosure;
import graalvm.compiler.nodes.cfg.Block;
import graalvm.compiler.nodes.virtual.VirtualObjectNode;
import graalvm.compiler.phases.graph.ReentrantBlockIterator;
import graalvm.compiler.phases.graph.ReentrantBlockIterator.BlockIteratorClosure;
import graalvm.compiler.phases.graph.StatelessPostOrderNodeIterator;
import graalvm.compiler.phases.schedule.SchedulePhase;
import graalvm.compiler.phases.schedule.SchedulePhase.SchedulingStrategy;

public final class GraphOrder
{
    private GraphOrder()
    {
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
