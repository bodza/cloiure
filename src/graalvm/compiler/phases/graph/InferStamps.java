package graalvm.compiler.phases.graph;

import graalvm.compiler.core.common.type.ObjectStamp;
import graalvm.compiler.graph.Node;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.ValuePhiNode;

public class InferStamps
{
    /**
     * Infer the stamps for all Object nodes in the graph, to make the stamps as precise as
     * possible. For example, this propagates the word-type through phi functions. To handle phi
     * functions at loop headers, the stamp inference is called until a fix point is reached.
     * <p>
     * This method can be used when it is needed that stamps are inferred before the first run of
     * the canonicalizer. For example, word type rewriting must run before the first run of the
     * canonicalizer because many nodes are not prepared to see the word type during
     * canonicalization.
     */
    public static void inferStamps(StructuredGraph graph)
    {
        /*
         * We want to make the stamps more precise. For cyclic phi functions, this means we have to
         * ignore the initial stamp because the imprecise stamp would always propagate around the
         * cycle. We therefore set the stamp to an illegal stamp, which is automatically ignored
         * when the phi function performs the "meet" operator on its input stamps.
         */
        for (Node n : graph.getNodes())
        {
            if (n instanceof ValuePhiNode)
            {
                ValueNode node = (ValueNode) n;
                if (node.stamp(NodeView.DEFAULT) instanceof ObjectStamp)
                {
                    node.setStamp(node.stamp(NodeView.DEFAULT).empty());
                }
            }
        }

        boolean stampChanged;
        // The algorithm is not guaranteed to reach a stable state.
        int z = 0;
        do
        {
            stampChanged = false;
            /*
             * We could use GraphOrder.forwardGraph() to process the nodes in a defined order and
             * propagate long def-use chains in fewer iterations. However, measurements showed that
             * we have few iterations anyway, and the overhead of computing the order is much higher
             * than the benefit.
             */
            for (Node n : graph.getNodes())
            {
                if (n instanceof ValueNode)
                {
                    ValueNode node = (ValueNode) n;
                    if (node.stamp(NodeView.DEFAULT) instanceof ObjectStamp)
                    {
                        stampChanged |= node.inferStamp();
                    }
                }
            }
            ++z;
        } while (stampChanged && z < 10000);
    }
}
