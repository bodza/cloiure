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

public class LoopSafepointEliminationPhase extends BasePhase<MidTierContext>
{
    @Override
    protected void run(StructuredGraph graph, MidTierContext context)
    {
        LoopsData loops = new LoopsData(graph);
        if (context.getOptimisticOptimizations().useLoopLimitChecks(graph.getOptions()) && graph.getGuardsStage().allowsFloatingGuards())
        {
            loops.detectedCountedLoops();
            for (LoopEx loop : loops.countedLoops())
            {
                if (loop.loop().getChildren().isEmpty() && loop.counted().getStamp().getBits() <= 32)
                {
                    boolean hasSafepoint = false;
                    for (LoopEndNode loopEnd : loop.loopBegin().loopEnds())
                    {
                        hasSafepoint |= loopEnd.canSafepoint();
                    }
                    if (hasSafepoint)
                    {
                        loop.counted().createOverFlowGuard();
                        loop.loopBegin().disableSafepoint();
                    }
                }
            }
        }
        for (LoopEx loop : loops.loops())
        {
            for (LoopEndNode loopEnd : loop.loopBegin().loopEnds())
            {
                Block b = loops.getCFG().blockFor(loopEnd);
                blocks: while (b != loop.loop().getHeader())
                {
                    for (FixedNode node : b.getNodes())
                    {
                        if (node instanceof Invoke || (node instanceof ForeignCallNode && ((ForeignCallNode) node).isGuaranteedSafepoint()))
                        {
                            loopEnd.disableSafepoint();
                            break blocks;
                        }
                    }
                    b = b.getDominator();
                }
            }
        }
        loops.deleteUnusedNodes();
    }
}
