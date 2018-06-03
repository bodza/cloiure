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

///
// Compute probabilities for fixed nodes on the fly and cache them at {@link AbstractBeginNode}s.
///
// @class FixedNodeProbabilityCache
public final class FixedNodeProbabilityCache implements ToDoubleFunction<FixedNode>
{
    // @field
    private final EconomicMap<FixedNode, Double> ___cache = EconomicMap.create(Equivalence.IDENTITY);

    ///
    // Given a {@link FixedNode} this method finds the most immediate {@link AbstractBeginNode}
    // preceding it that either:
    //
    // - has no predecessor (ie, the begin-node is a merge, in particular a loop-begin, or the start-node)
    // - has a control-split predecessor
    //
    // The thus found {@link AbstractBeginNode} is equi-probable with the {@link FixedNode} it was
    // obtained from. When computed for the first time (afterwards a cache lookup returns it) that
    // probability is computed as follows, again depending on the begin-node's predecessor:
    //
    // - No predecessor. In this case the begin-node is either:
    // -- a merge-node, whose probability adds up those of its forward-ends
    // -- a loop-begin, with probability as above multiplied by the loop-frequency
    // - Control-split predecessor: probability of the branch times that of the control-split
    //
    // As an exception to all the above, a probability of 1 is assumed for a {@link FixedNode} that
    // appears to be dead-code (ie, lacks a predecessor).
    ///
    @Override
    public double applyAsDouble(FixedNode __node)
    {
        FixedNode __current = findBegin(__node);
        if (__current == null)
        {
            // this should only appear for dead code
            return 1D;
        }

        Double __cachedValue = this.___cache.get(__current);
        if (__cachedValue != null)
        {
            return __cachedValue;
        }

        double __probability = 0.0;
        if (__current.predecessor() == null)
        {
            if (__current instanceof AbstractMergeNode)
            {
                __probability = handleMerge(__current, __probability);
            }
            else
            {
                __probability = 1D;
            }
        }
        else
        {
            ControlSplitNode __split = (ControlSplitNode) __current.predecessor();
            __probability = ControlFlowGraph.multiplyProbabilities(__split.probability((AbstractBeginNode) __current), applyAsDouble(__split));
        }
        this.___cache.put(__current, __probability);
        return __probability;
    }

    private double handleMerge(FixedNode __current, double __probability)
    {
        double __result = __probability;
        AbstractMergeNode __currentMerge = (AbstractMergeNode) __current;
        NodeInputList<EndNode> __currentForwardEnds = __currentMerge.forwardEnds();
        // Use simple iteration instead of streams, since the stream infrastructure adds many frames
        // which causes the recursion to overflow the stack earlier than it would otherwise.
        for (AbstractEndNode __endNode : __currentForwardEnds)
        {
            __result += applyAsDouble(__endNode);
        }
        if (__current instanceof LoopBeginNode)
        {
            __result = ControlFlowGraph.multiplyProbabilities(__result, ((LoopBeginNode) __current).loopFrequency());
        }
        return __result;
    }

    private static FixedNode findBegin(FixedNode __node)
    {
        FixedNode __current = __node;
        while (true)
        {
            Node __predecessor = __current.predecessor();
            if (__current instanceof AbstractBeginNode)
            {
                if (__predecessor == null)
                {
                    break;
                }
                else if (__predecessor.successors().count() != 1)
                {
                    break;
                }
            }
            else if (__predecessor == null)
            {
                __current = null;
                break;
            }
            __current = (FixedNode) __predecessor;
        }
        return __current;
    }
}
