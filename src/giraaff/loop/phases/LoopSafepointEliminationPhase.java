package giraaff.loop.phases;

import giraaff.loop.LoopEx;
import giraaff.loop.LoopsData;
import giraaff.nodes.FixedNode;
import giraaff.nodes.Invoke;
import giraaff.nodes.LoopEndNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.cfg.Block;
import giraaff.nodes.extended.ForeignCallNode;
import giraaff.phases.BasePhase;
import giraaff.phases.tiers.MidTierContext;

// @class LoopSafepointEliminationPhase
public final class LoopSafepointEliminationPhase extends BasePhase<MidTierContext>
{
    @Override
    protected void run(StructuredGraph __graph, MidTierContext __context)
    {
        LoopsData __loops = new LoopsData(__graph);
        if (__context.getOptimisticOptimizations().useLoopLimitChecks() && __graph.getGuardsStage().allowsFloatingGuards())
        {
            __loops.detectedCountedLoops();
            for (LoopEx __loop : __loops.countedLoops())
            {
                if (__loop.loop().getChildren().isEmpty() && __loop.counted().getStamp().getBits() <= 32)
                {
                    boolean __hasSafepoint = false;
                    for (LoopEndNode __loopEnd : __loop.loopBegin().loopEnds())
                    {
                        __hasSafepoint |= __loopEnd.canSafepoint();
                    }
                    if (__hasSafepoint)
                    {
                        __loop.counted().createOverFlowGuard();
                        __loop.loopBegin().disableSafepoint();
                    }
                }
            }
        }
        for (LoopEx __loop : __loops.loops())
        {
            for (LoopEndNode __loopEnd : __loop.loopBegin().loopEnds())
            {
                Block __b = __loops.getCFG().blockFor(__loopEnd);
                blocks: while (__b != __loop.loop().getHeader())
                {
                    for (FixedNode __node : __b.getNodes())
                    {
                        if (__node instanceof Invoke || (__node instanceof ForeignCallNode && ((ForeignCallNode) __node).isGuaranteedSafepoint()))
                        {
                            __loopEnd.disableSafepoint();
                            break blocks;
                        }
                    }
                    __b = __b.getDominator();
                }
            }
        }
        __loops.deleteUnusedNodes();
    }
}
