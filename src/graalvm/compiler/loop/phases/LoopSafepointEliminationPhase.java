package graalvm.compiler.loop.phases;

import graalvm.compiler.loop.LoopEx;
import graalvm.compiler.loop.LoopsData;
import graalvm.compiler.nodes.FixedNode;
import graalvm.compiler.nodes.Invoke;
import graalvm.compiler.nodes.LoopEndNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.cfg.Block;
import graalvm.compiler.nodes.extended.ForeignCallNode;
import graalvm.compiler.phases.BasePhase;
import graalvm.compiler.phases.tiers.MidTierContext;

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
