package giraaff.phases.graph;

import java.util.function.ToDoubleFunction;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.Equivalence;

import giraaff.graph.Node;
import giraaff.graph.NodeInputList;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.AbstractEndNode;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.ControlSplitNode;
import giraaff.nodes.EndNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.LoopBeginNode;
import giraaff.nodes.cfg.ControlFlowGraph;

/**
 * Compute probabilities for fixed nodes on the fly and cache them at {@link AbstractBeginNode}s.
 */
// @class FixedNodeProbabilityCache
public final class FixedNodeProbabilityCache implements ToDoubleFunction<FixedNode>
{
    private final EconomicMap<FixedNode, Double> cache = EconomicMap.create(Equivalence.IDENTITY);

    /**
     * Given a {@link FixedNode} this method finds the most immediate {@link AbstractBeginNode}
     * preceding it that either:
     *
     * - has no predecessor (ie, the begin-node is a merge, in particular a loop-begin, or the start-node)
     * - has a control-split predecessor
     *
     * The thus found {@link AbstractBeginNode} is equi-probable with the {@link FixedNode} it was
     * obtained from. When computed for the first time (afterwards a cache lookup returns it) that
     * probability is computed as follows, again depending on the begin-node's predecessor:
     *
     * - No predecessor. In this case the begin-node is either:
     * -- a merge-node, whose probability adds up those of its forward-ends
     * -- a loop-begin, with probability as above multiplied by the loop-frequency
     * - Control-split predecessor: probability of the branch times that of the control-split
     *
     * As an exception to all the above, a probability of 1 is assumed for a {@link FixedNode} that
     * appears to be dead-code (ie, lacks a predecessor).
     */
    @Override
    public double applyAsDouble(FixedNode node)
    {
        FixedNode current = findBegin(node);
        if (current == null)
        {
            // this should only appear for dead code
            return 1D;
        }

        Double cachedValue = cache.get(current);
        if (cachedValue != null)
        {
            return cachedValue;
        }

        double probability = 0.0;
        if (current.predecessor() == null)
        {
            if (current instanceof AbstractMergeNode)
            {
                probability = handleMerge(current, probability);
            }
            else
            {
                probability = 1D;
            }
        }
        else
        {
            ControlSplitNode split = (ControlSplitNode) current.predecessor();
            probability = ControlFlowGraph.multiplyProbabilities(split.probability((AbstractBeginNode) current), applyAsDouble(split));
        }
        cache.put(current, probability);
        return probability;
    }

    private double handleMerge(FixedNode current, double probability)
    {
        double result = probability;
        AbstractMergeNode currentMerge = (AbstractMergeNode) current;
        NodeInputList<EndNode> currentForwardEnds = currentMerge.forwardEnds();
        /*
         * Use simple iteration instead of streams, since the stream infrastructure adds many frames
         * which causes the recursion to overflow the stack earlier than it would otherwise.
         */
        for (AbstractEndNode endNode : currentForwardEnds)
        {
            result += applyAsDouble(endNode);
        }
        if (current instanceof LoopBeginNode)
        {
            result = ControlFlowGraph.multiplyProbabilities(result, ((LoopBeginNode) current).loopFrequency());
        }
        return result;
    }

    private static FixedNode findBegin(FixedNode node)
    {
        FixedNode current = node;
        while (true)
        {
            Node predecessor = current.predecessor();
            if (current instanceof AbstractBeginNode)
            {
                if (predecessor == null)
                {
                    break;
                }
                else if (predecessor.successors().count() != 1)
                {
                    break;
                }
            }
            else if (predecessor == null)
            {
                current = null;
                break;
            }
            current = (FixedNode) predecessor;
        }
        return current;
    }
}
