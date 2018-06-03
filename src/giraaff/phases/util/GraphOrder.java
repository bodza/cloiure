package giraaff.phases.util;

import java.util.ArrayList;
import java.util.List;

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

    private static List<Node> createOrder(StructuredGraph __graph)
    {
        final ArrayList<Node> __nodes = new ArrayList<>();
        final NodeBitMap __visited = __graph.createNodeBitMap();

        // @closure
        new StatelessPostOrderNodeIterator(__graph.start())
        {
            @Override
            protected void node(FixedNode __node)
            {
                visitForward(__nodes, __visited, __node, false);
            }
        }.apply();
        return __nodes;
    }

    private static void visitForward(ArrayList<Node> __nodes, NodeBitMap __visited, Node __node, boolean __floatingOnly)
    {
        if (__node != null && !__visited.isMarked(__node))
        {
            if (__floatingOnly && __node instanceof FixedNode)
            {
                throw new GraalError("unexpected reference to fixed node: %s (this indicates an unexpected cycle)", __node);
            }
            __visited.mark(__node);
            FrameState __stateAfter = null;
            if (__node instanceof StateSplit)
            {
                __stateAfter = ((StateSplit) __node).stateAfter();
            }
            for (Node __input : __node.inputs())
            {
                if (__input != __stateAfter)
                {
                    visitForward(__nodes, __visited, __input, true);
                }
            }
            if (__node instanceof EndNode)
            {
                EndNode __end = (EndNode) __node;
                for (PhiNode __phi : __end.merge().phis())
                {
                    visitForward(__nodes, __visited, __phi.valueAt(__end), true);
                }
            }
            __nodes.add(__node);
            if (__node instanceof AbstractMergeNode)
            {
                for (PhiNode __phi : ((AbstractMergeNode) __node).phis())
                {
                    __visited.mark(__phi);
                    __nodes.add(__phi);
                }
            }
            if (__stateAfter != null)
            {
                visitForward(__nodes, __visited, __stateAfter, true);
            }
        }
    }
}
