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
import giraaff.nodes.cfg.Block;
import giraaff.phases.BasePhase;
import giraaff.phases.graph.ScheduledNodeIterator;
import giraaff.phases.schedule.SchedulePhase;
import giraaff.phases.tiers.MidTierContext;

///
// This phase lowers {@link GuardNode GuardNodes} into corresponding control-flow structure and
// {@link DeoptimizeNode DeoptimizeNodes}.
//
// This allow to enter the {@link StructuredGraph.GuardsStage#FIXED_DEOPTS FIXED_DEOPTS} stage of
// the graph where all node that may cause deoptimization are fixed.
//
// It first makes a schedule in order to know where the control flow should be placed. Then, for
// each block, it applies two passes. The first one tries to replace null-check guards with implicit
// null checks performed by access to the objects that need to be null checked. The second phase
// does the actual control-flow expansion of the remaining {@link GuardNode GuardNodes}.
///
// @class GuardLoweringPhase
public final class GuardLoweringPhase extends BasePhase<MidTierContext>
{
    // @class GuardLoweringPhase.LowerGuards
    private static final class LowerGuards extends ScheduledNodeIterator
    {
        // @field
        private final Block ___block;

        // @cons GuardLoweringPhase.LowerGuards
        LowerGuards(Block __block)
        {
            super();
            this.___block = __block;
        }

        @Override
        protected void processNode(Node __node)
        {
            if (__node instanceof GuardNode)
            {
                GuardNode __guard = (GuardNode) __node;
                FixedWithNextNode __lowered = __guard.lowerGuard();
                if (__lowered != null)
                {
                    replaceCurrent(__lowered);
                }
                else
                {
                    lowerToIf(__guard);
                }
            }
        }

        private void lowerToIf(GuardNode __guard)
        {
            StructuredGraph __graph = __guard.graph();
            AbstractBeginNode __fastPath = __graph.add(new BeginNode());
            DeoptimizeNode __deopt = __graph.add(new DeoptimizeNode(__guard.getAction(), __guard.getReason(), DeoptimizeNode.DEFAULT_DEBUG_ID, __guard.getSpeculation(), null));
            AbstractBeginNode __deoptBranch = BeginNode.begin(__deopt);
            AbstractBeginNode __trueSuccessor;
            AbstractBeginNode __falseSuccessor;
            insertLoopExits(__deopt);
            if (__guard.isNegated())
            {
                __trueSuccessor = __deoptBranch;
                __falseSuccessor = __fastPath;
            }
            else
            {
                __trueSuccessor = __fastPath;
                __falseSuccessor = __deoptBranch;
            }
            IfNode __ifNode = __graph.add(new IfNode(__guard.getCondition(), __trueSuccessor, __falseSuccessor, __trueSuccessor == __fastPath ? 1 : 0));
            __guard.replaceAndDelete(__fastPath);
            insert(__ifNode, __fastPath);
        }

        private void insertLoopExits(DeoptimizeNode __deopt)
        {
            Loop<Block> __loop = this.___block.getLoop();
            StructuredGraph __graph = __deopt.graph();
            while (__loop != null)
            {
                LoopExitNode __exit = __graph.add(new LoopExitNode((LoopBeginNode) __loop.getHeader().getBeginNode()));
                __graph.addBeforeFixed(__deopt, __exit);
                __loop = __loop.getParent();
            }
        }
    }

    @Override
    protected void run(StructuredGraph __graph, MidTierContext __context)
    {
        if (__graph.getGuardsStage().allowsFloatingGuards())
        {
            SchedulePhase __schedulePhase = new SchedulePhase(SchedulePhase.SchedulingStrategy.EARLIEST_WITH_GUARD_ORDER);
            __schedulePhase.apply(__graph);
            StructuredGraph.ScheduleResult __schedule = __graph.getLastSchedule();

            for (Block __block : __schedule.getCFG().getBlocks())
            {
                processBlock(__block, __schedule);
            }
            __graph.setGuardsStage(StructuredGraph.GuardsStage.FIXED_DEOPTS);
        }
    }

    private static boolean assertNoGuardsLeft(StructuredGraph __graph)
    {
        return true;
    }

    private static void processBlock(Block __block, StructuredGraph.ScheduleResult __schedule)
    {
        new GuardLoweringPhase.LowerGuards(__block).processNodes(__block, __schedule);
    }
}
