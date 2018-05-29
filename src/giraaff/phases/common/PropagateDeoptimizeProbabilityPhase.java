package giraaff.phases.common;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.EconomicSet;
import org.graalvm.collections.MapCursor;

import giraaff.graph.NodeStack;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.AbstractDeoptimizeNode;
import giraaff.nodes.AbstractEndNode;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.ControlSplitNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.StructuredGraph;
import giraaff.phases.BasePhase;
import giraaff.phases.tiers.PhaseContext;

/**
 * This phase will make sure that the branch leading towards this deopt has 0.0 probability.
 */
// @class PropagateDeoptimizeProbabilityPhase
public final class PropagateDeoptimizeProbabilityPhase extends BasePhase<PhaseContext>
{
    @Override
    protected void run(final StructuredGraph graph, PhaseContext context)
    {
        if (graph.hasNode(AbstractDeoptimizeNode.TYPE))
        {
            NodeStack stack = new NodeStack();
            EconomicMap<ControlSplitNode, EconomicSet<AbstractBeginNode>> reachableSplits = EconomicMap.create();

            // Mark all control flow nodes that are post-dominated by a deoptimization.
            for (AbstractDeoptimizeNode d : graph.getNodes(AbstractDeoptimizeNode.TYPE))
            {
                stack.push(AbstractBeginNode.prevBegin(d));
                while (!stack.isEmpty())
                {
                    AbstractBeginNode beginNode = (AbstractBeginNode) stack.pop();
                    FixedNode fixedNode = (FixedNode) beginNode.predecessor();

                    if (fixedNode == null)
                    {
                        // Can happen for start node.
                    }
                    else if (fixedNode instanceof AbstractMergeNode)
                    {
                        AbstractMergeNode mergeNode = (AbstractMergeNode) fixedNode;
                        for (AbstractEndNode end : mergeNode.forwardEnds())
                        {
                            AbstractBeginNode newBeginNode = AbstractBeginNode.prevBegin(end);
                            stack.push(newBeginNode);
                        }
                    }
                    else if (fixedNode instanceof ControlSplitNode)
                    {
                        ControlSplitNode controlSplitNode = (ControlSplitNode) fixedNode;
                        EconomicSet<AbstractBeginNode> reachableSuccessors = reachableSplits.get(controlSplitNode);
                        if (reachableSuccessors == null)
                        {
                            reachableSuccessors = EconomicSet.create();
                            reachableSplits.put(controlSplitNode, reachableSuccessors);
                        }

                        if (controlSplitNode.getSuccessorCount() == reachableSuccessors.size() - 1)
                        {
                            // All successors of this split lead to deopt, propagate reachability further upwards.
                            reachableSplits.removeKey(controlSplitNode);
                            stack.push(AbstractBeginNode.prevBegin((FixedNode) controlSplitNode.predecessor()));
                        }
                        else
                        {
                            reachableSuccessors.add(beginNode);
                        }
                    }
                    else
                    {
                        stack.push(AbstractBeginNode.prevBegin(fixedNode));
                    }
                }
            }

            // Make sure the probability on the path towards the deoptimization is 0.0.
            MapCursor<ControlSplitNode, EconomicSet<AbstractBeginNode>> entries = reachableSplits.getEntries();
            while (entries.advance())
            {
                ControlSplitNode controlSplitNode = entries.getKey();
                EconomicSet<AbstractBeginNode> value = entries.getValue();
                for (AbstractBeginNode begin : value)
                {
                    double probability = controlSplitNode.probability(begin);
                    if (probability != 0.0)
                    {
                        controlSplitNode.setProbability(begin, 0.0);
                    }
                }
            }
        }
    }
}
