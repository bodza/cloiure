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

///
// This phase tries to find {@link AbstractDeoptimizeNode DeoptimizeNodes} which use the same
// {@link FrameState} and merges them together.
///
// @class DeoptimizationGroupingPhase
public final class DeoptimizationGroupingPhase extends BasePhase<MidTierContext>
{
    @Override
    protected void run(StructuredGraph __graph, MidTierContext __context)
    {
        ControlFlowGraph __cfg = null;
        for (FrameState __fs : __graph.getNodes(FrameState.TYPE))
        {
            FixedNode __target = null;
            PhiNode __reasonActionPhi = null;
            PhiNode __speculationPhi = null;
            List<AbstractDeoptimizeNode> __obsoletes = null;
            for (AbstractDeoptimizeNode __deopt : __fs.usages().filter(AbstractDeoptimizeNode.class))
            {
                if (__target == null)
                {
                    __target = __deopt;
                }
                else
                {
                    if (__cfg == null)
                    {
                        __cfg = ControlFlowGraph.compute(__graph, true, true, false, false);
                    }
                    AbstractMergeNode __merge;
                    if (__target instanceof AbstractDeoptimizeNode)
                    {
                        __merge = __graph.add(new MergeNode());
                        EndNode __firstEnd = __graph.add(new EndNode());
                        ValueNode __actionAndReason = ((AbstractDeoptimizeNode) __target).getActionAndReason(__context.getMetaAccess());
                        ValueNode __speculation = ((AbstractDeoptimizeNode) __target).getSpeculation(__context.getMetaAccess());
                        __reasonActionPhi = __graph.addWithoutUnique(new ValuePhiNode(StampFactory.forKind(__actionAndReason.getStackKind()), __merge));
                        __speculationPhi = __graph.addWithoutUnique(new ValuePhiNode(StampFactory.forKind(__speculation.getStackKind()), __merge));
                        __merge.addForwardEnd(__firstEnd);
                        __reasonActionPhi.addInput(__actionAndReason);
                        __speculationPhi.addInput(__speculation);
                        __target.replaceAtPredecessor(__firstEnd);

                        exitLoops((AbstractDeoptimizeNode) __target, __firstEnd, __cfg);
                        __merge.setNext(__graph.add(new DynamicDeoptimizeNode(__reasonActionPhi, __speculationPhi)));
                        __obsoletes = new LinkedList<>();
                        __obsoletes.add((AbstractDeoptimizeNode) __target);
                        __target = __merge;
                    }
                    else
                    {
                        __merge = (AbstractMergeNode) __target;
                    }
                    EndNode __newEnd = __graph.add(new EndNode());
                    __merge.addForwardEnd(__newEnd);
                    __reasonActionPhi.addInput(__deopt.getActionAndReason(__context.getMetaAccess()));
                    __speculationPhi.addInput(__deopt.getSpeculation(__context.getMetaAccess()));
                    __deopt.replaceAtPredecessor(__newEnd);
                    exitLoops(__deopt, __newEnd, __cfg);
                    __obsoletes.add(__deopt);
                }
            }
            if (__obsoletes != null)
            {
                ((DynamicDeoptimizeNode) ((AbstractMergeNode) __target).next()).setStateBefore(__fs);
                for (AbstractDeoptimizeNode __obsolete : __obsoletes)
                {
                    __obsolete.safeDelete();
                }
            }
        }
    }

    private static void exitLoops(AbstractDeoptimizeNode __deopt, EndNode __end, ControlFlowGraph __cfg)
    {
        Block __block = __cfg.blockFor(__deopt);
        Loop<Block> __loop = __block.getLoop();
        while (__loop != null)
        {
            __end.graph().addBeforeFixed(__end, __end.graph().add(new LoopExitNode((LoopBeginNode) __loop.getHeader().getBeginNode())));
            __loop = __loop.getParent();
        }
    }
}
