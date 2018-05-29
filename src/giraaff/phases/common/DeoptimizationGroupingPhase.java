package giraaff.phases.common;

import java.util.LinkedList;
import java.util.List;

import giraaff.core.common.cfg.Loop;
import giraaff.core.common.type.StampFactory;
import giraaff.nodes.AbstractDeoptimizeNode;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.DynamicDeoptimizeNode;
import giraaff.nodes.EndNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FrameState;
import giraaff.nodes.LoopBeginNode;
import giraaff.nodes.LoopExitNode;
import giraaff.nodes.MergeNode;
import giraaff.nodes.PhiNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.ValuePhiNode;
import giraaff.nodes.cfg.Block;
import giraaff.nodes.cfg.ControlFlowGraph;
import giraaff.phases.BasePhase;
import giraaff.phases.tiers.MidTierContext;

/**
 * This phase tries to find {@link AbstractDeoptimizeNode DeoptimizeNodes} which use the same
 * {@link FrameState} and merges them together.
 */
// @class DeoptimizationGroupingPhase
public final class DeoptimizationGroupingPhase extends BasePhase<MidTierContext>
{
    @Override
    protected void run(StructuredGraph graph, MidTierContext context)
    {
        ControlFlowGraph cfg = null;
        for (FrameState fs : graph.getNodes(FrameState.TYPE))
        {
            FixedNode target = null;
            PhiNode reasonActionPhi = null;
            PhiNode speculationPhi = null;
            List<AbstractDeoptimizeNode> obsoletes = null;
            for (AbstractDeoptimizeNode deopt : fs.usages().filter(AbstractDeoptimizeNode.class))
            {
                if (target == null)
                {
                    target = deopt;
                }
                else
                {
                    if (cfg == null)
                    {
                        cfg = ControlFlowGraph.compute(graph, true, true, false, false);
                    }
                    AbstractMergeNode merge;
                    if (target instanceof AbstractDeoptimizeNode)
                    {
                        merge = graph.add(new MergeNode());
                        EndNode firstEnd = graph.add(new EndNode());
                        ValueNode actionAndReason = ((AbstractDeoptimizeNode) target).getActionAndReason(context.getMetaAccess());
                        ValueNode speculation = ((AbstractDeoptimizeNode) target).getSpeculation(context.getMetaAccess());
                        reasonActionPhi = graph.addWithoutUnique(new ValuePhiNode(StampFactory.forKind(actionAndReason.getStackKind()), merge));
                        speculationPhi = graph.addWithoutUnique(new ValuePhiNode(StampFactory.forKind(speculation.getStackKind()), merge));
                        merge.addForwardEnd(firstEnd);
                        reasonActionPhi.addInput(actionAndReason);
                        speculationPhi.addInput(speculation);
                        target.replaceAtPredecessor(firstEnd);

                        exitLoops((AbstractDeoptimizeNode) target, firstEnd, cfg);
                        merge.setNext(graph.add(new DynamicDeoptimizeNode(reasonActionPhi, speculationPhi)));
                        obsoletes = new LinkedList<>();
                        obsoletes.add((AbstractDeoptimizeNode) target);
                        target = merge;
                    }
                    else
                    {
                        merge = (AbstractMergeNode) target;
                    }
                    EndNode newEnd = graph.add(new EndNode());
                    merge.addForwardEnd(newEnd);
                    reasonActionPhi.addInput(deopt.getActionAndReason(context.getMetaAccess()));
                    speculationPhi.addInput(deopt.getSpeculation(context.getMetaAccess()));
                    deopt.replaceAtPredecessor(newEnd);
                    exitLoops(deopt, newEnd, cfg);
                    obsoletes.add(deopt);
                }
            }
            if (obsoletes != null)
            {
                ((DynamicDeoptimizeNode) ((AbstractMergeNode) target).next()).setStateBefore(fs);
                for (AbstractDeoptimizeNode obsolete : obsoletes)
                {
                    obsolete.safeDelete();
                }
            }
        }
    }

    private static void exitLoops(AbstractDeoptimizeNode deopt, EndNode end, ControlFlowGraph cfg)
    {
        Block block = cfg.blockFor(deopt);
        Loop<Block> loop = block.getLoop();
        while (loop != null)
        {
            end.graph().addBeforeFixed(end, end.graph().add(new LoopExitNode((LoopBeginNode) loop.getHeader().getBeginNode())));
            loop = loop.getParent();
        }
    }
}
