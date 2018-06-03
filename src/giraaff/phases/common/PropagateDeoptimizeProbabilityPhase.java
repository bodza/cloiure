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

///
// This phase will make sure that the branch leading towards this deopt has 0.0 probability.
///
// @class PropagateDeoptimizeProbabilityPhase
public final class PropagateDeoptimizeProbabilityPhase extends BasePhase<PhaseContext>
{
    @Override
    protected void run(final StructuredGraph __graph, PhaseContext __context)
    {
        if (__graph.hasNode(AbstractDeoptimizeNode.TYPE))
        {
            NodeStack __stack = new NodeStack();
            EconomicMap<ControlSplitNode, EconomicSet<AbstractBeginNode>> __reachableSplits = EconomicMap.create();

            // Mark all control flow nodes that are post-dominated by a deoptimization.
            for (AbstractDeoptimizeNode __d : __graph.getNodes(AbstractDeoptimizeNode.TYPE))
            {
                __stack.push(AbstractBeginNode.prevBegin(__d));
                while (!__stack.isEmpty())
                {
                    AbstractBeginNode __beginNode = (AbstractBeginNode) __stack.pop();
                    FixedNode __fixedNode = (FixedNode) __beginNode.predecessor();

                    if (__fixedNode == null)
                    {
                        // Can happen for start node.
                    }
                    else if (__fixedNode instanceof AbstractMergeNode)
                    {
                        AbstractMergeNode __mergeNode = (AbstractMergeNode) __fixedNode;
                        for (AbstractEndNode __end : __mergeNode.forwardEnds())
                        {
                            AbstractBeginNode __newBeginNode = AbstractBeginNode.prevBegin(__end);
                            __stack.push(__newBeginNode);
                        }
                    }
                    else if (__fixedNode instanceof ControlSplitNode)
                    {
                        ControlSplitNode __controlSplitNode = (ControlSplitNode) __fixedNode;
                        EconomicSet<AbstractBeginNode> __reachableSuccessors = __reachableSplits.get(__controlSplitNode);
                        if (__reachableSuccessors == null)
                        {
                            __reachableSuccessors = EconomicSet.create();
                            __reachableSplits.put(__controlSplitNode, __reachableSuccessors);
                        }

                        if (__controlSplitNode.getSuccessorCount() == __reachableSuccessors.size() - 1)
                        {
                            // All successors of this split lead to deopt, propagate reachability further upwards.
                            __reachableSplits.removeKey(__controlSplitNode);
                            __stack.push(AbstractBeginNode.prevBegin((FixedNode) __controlSplitNode.predecessor()));
                        }
                        else
                        {
                            __reachableSuccessors.add(__beginNode);
                        }
                    }
                    else
                    {
                        __stack.push(AbstractBeginNode.prevBegin(__fixedNode));
                    }
                }
            }

            // Make sure the probability on the path towards the deoptimization is 0.0.
            MapCursor<ControlSplitNode, EconomicSet<AbstractBeginNode>> __entries = __reachableSplits.getEntries();
            while (__entries.advance())
            {
                ControlSplitNode __controlSplitNode = __entries.getKey();
                EconomicSet<AbstractBeginNode> __value = __entries.getValue();
                for (AbstractBeginNode __begin : __value)
                {
                    double __probability = __controlSplitNode.probability(__begin);
                    if (__probability != 0.0)
                    {
                        __controlSplitNode.setProbability(__begin, 0.0);
                    }
                }
            }
        }
    }
}
