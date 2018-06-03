package giraaff.phases.graph;

import giraaff.core.common.type.ObjectStamp;
import giraaff.graph.Node;
import giraaff.nodes.NodeView;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.ValuePhiNode;

// @class InferStamps
public final class InferStamps
{
    ///
    // Infer the stamps for all Object nodes in the graph, to make the stamps as precise as
    // possible. For example, this propagates the word-type through phi functions. To handle phi
    // functions at loop headers, the stamp inference is called until a fix point is reached.
    //
    // This method can be used when it is needed that stamps are inferred before the first run of
    // the canonicalizer. For example, word type rewriting must run before the first run of the
    // canonicalizer because many nodes are not prepared to see the word type during canonicalization.
    ///
    public static void inferStamps(StructuredGraph __graph)
    {
        // We want to make the stamps more precise. For cyclic phi functions, this means we have to
        // ignore the initial stamp because the imprecise stamp would always propagate around the
        // cycle. We therefore set the stamp to an illegal stamp, which is automatically ignored
        // when the phi function performs the "meet" operator on its input stamps.
        for (Node __n : __graph.getNodes())
        {
            if (__n instanceof ValuePhiNode)
            {
                ValueNode __node = (ValueNode) __n;
                if (__node.stamp(NodeView.DEFAULT) instanceof ObjectStamp)
                {
                    __node.setStamp(__node.stamp(NodeView.DEFAULT).empty());
                }
            }
        }

        boolean __stampChanged;
        // The algorithm is not guaranteed to reach a stable state.
        int __z = 0;
        do
        {
            __stampChanged = false;
            // We could use GraphOrder.forwardGraph() to process the nodes in a defined order and
            // propagate long def-use chains in fewer iterations. However, measurements showed that
            // we have few iterations anyway, and the overhead of computing the order is much higher
            // than the benefit.
            for (Node __n : __graph.getNodes())
            {
                if (__n instanceof ValueNode)
                {
                    ValueNode __node = (ValueNode) __n;
                    if (__node.stamp(NodeView.DEFAULT) instanceof ObjectStamp)
                    {
                        __stampChanged |= __node.inferStamp();
                    }
                }
            }
            ++__z;
        } while (__stampChanged && __z < 10000);
    }
}
