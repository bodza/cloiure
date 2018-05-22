package giraaff.phases.common;

import giraaff.core.common.GraalOptions;
import giraaff.nodes.LoopBeginNode;
import giraaff.nodes.LoopEndNode;
import giraaff.nodes.SafepointNode;
import giraaff.nodes.StructuredGraph;
import giraaff.phases.Phase;

/**
 * Adds safepoints to loops.
 */
public class LoopSafepointInsertionPhase extends Phase
{
    @Override
    protected void run(StructuredGraph graph)
    {
        if (GraalOptions.GenLoopSafepoints.getValue(graph.getOptions()))
        {
            for (LoopBeginNode loopBeginNode : graph.getNodes(LoopBeginNode.TYPE))
            {
                for (LoopEndNode loopEndNode : loopBeginNode.loopEnds())
                {
                    if (loopEndNode.canSafepoint())
                    {
                        SafepointNode safepointNode = graph.add(new SafepointNode());
                        graph.addBeforeFixed(loopEndNode, safepointNode);
                    }
                }
            }
        }
    }
}
