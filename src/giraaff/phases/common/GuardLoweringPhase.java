package giraaff.phases.common;

import giraaff.core.common.cfg.Loop;
import giraaff.graph.Node;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.BeginNode;
import giraaff.nodes.DeoptimizeNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.GuardNode;
import giraaff.nodes.IfNode;
import giraaff.nodes.LoopBeginNode;
import giraaff.nodes.LoopExitNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.StructuredGraph.GuardsStage;
import giraaff.nodes.StructuredGraph.ScheduleResult;
import giraaff.nodes.cfg.Block;
import giraaff.phases.BasePhase;
import giraaff.phases.graph.ScheduledNodeIterator;
import giraaff.phases.schedule.SchedulePhase;
import giraaff.phases.schedule.SchedulePhase.SchedulingStrategy;
import giraaff.phases.tiers.MidTierContext;

/**
 * This phase lowers {@link GuardNode GuardNodes} into corresponding control-flow structure and
 * {@link DeoptimizeNode DeoptimizeNodes}.
 *
 * This allow to enter the {@link GuardsStage#FIXED_DEOPTS FIXED_DEOPTS} stage of the graph where
 * all node that may cause deoptimization are fixed.
 *
 * It first makes a schedule in order to know where the control flow should be placed. Then, for
 * each block, it applies two passes. The first one tries to replace null-check guards with implicit
 * null checks performed by access to the objects that need to be null checked. The second phase
 * does the actual control-flow expansion of the remaining {@link GuardNode GuardNodes}.
 */
public class GuardLoweringPhase extends BasePhase<MidTierContext>
{
    private static class LowerGuards extends ScheduledNodeIterator
    {
        private final Block block;

        LowerGuards(Block block)
        {
            this.block = block;
        }

        @Override
        protected void processNode(Node node)
        {
            if (node instanceof GuardNode)
            {
                GuardNode guard = (GuardNode) node;
                FixedWithNextNode lowered = guard.lowerGuard();
                if (lowered != null)
                {
                    replaceCurrent(lowered);
                }
                else
                {
                    lowerToIf(guard);
                }
            }
        }

        private void lowerToIf(GuardNode guard)
        {
            StructuredGraph graph = guard.graph();
            AbstractBeginNode fastPath = graph.add(new BeginNode());
            DeoptimizeNode deopt = graph.add(new DeoptimizeNode(guard.getAction(), guard.getReason(), DeoptimizeNode.DEFAULT_DEBUG_ID, guard.getSpeculation(), null));
            AbstractBeginNode deoptBranch = BeginNode.begin(deopt);
            AbstractBeginNode trueSuccessor;
            AbstractBeginNode falseSuccessor;
            insertLoopExits(deopt);
            if (guard.isNegated())
            {
                trueSuccessor = deoptBranch;
                falseSuccessor = fastPath;
            }
            else
            {
                trueSuccessor = fastPath;
                falseSuccessor = deoptBranch;
            }
            IfNode ifNode = graph.add(new IfNode(guard.getCondition(), trueSuccessor, falseSuccessor, trueSuccessor == fastPath ? 1 : 0));
            guard.replaceAndDelete(fastPath);
            insert(ifNode, fastPath);
        }

        private void insertLoopExits(DeoptimizeNode deopt)
        {
            Loop<Block> loop = block.getLoop();
            StructuredGraph graph = deopt.graph();
            while (loop != null)
            {
                LoopExitNode exit = graph.add(new LoopExitNode((LoopBeginNode) loop.getHeader().getBeginNode()));
                graph.addBeforeFixed(deopt, exit);
                loop = loop.getParent();
            }
        }
    }

    @Override
    protected void run(StructuredGraph graph, MidTierContext context)
    {
        if (graph.getGuardsStage().allowsFloatingGuards())
        {
            SchedulePhase schedulePhase = new SchedulePhase(SchedulingStrategy.EARLIEST_WITH_GUARD_ORDER);
            schedulePhase.apply(graph);
            ScheduleResult schedule = graph.getLastSchedule();

            for (Block block : schedule.getCFG().getBlocks())
            {
                processBlock(block, schedule);
            }
            graph.setGuardsStage(GuardsStage.FIXED_DEOPTS);
        }
    }

    private static boolean assertNoGuardsLeft(StructuredGraph graph)
    {
        return true;
    }

    private static void processBlock(Block block, ScheduleResult schedule)
    {
        new LowerGuards(block).processNodes(block, schedule);
    }
}
