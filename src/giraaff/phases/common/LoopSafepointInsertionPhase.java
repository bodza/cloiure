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
// @class LoopSafepointInsertionPhase
public final class LoopSafepointInsertionPhase extends Phase
{
    @Override
    protected void run(StructuredGraph __graph)
    {
        if (GraalOptions.genLoopSafepoints)
        {
            for (LoopBeginNode __loopBeginNode : __graph.getNodes(LoopBeginNode.TYPE))
            {
                for (LoopEndNode __loopEndNode : __loopBeginNode.loopEnds())
                {
                    if (__loopEndNode.canSafepoint())
                    {
                        SafepointNode __safepointNode = __graph.add(new SafepointNode());
                        __graph.addBeforeFixed(__loopEndNode, __safepointNode);
                    }
                }
            }
        }
    }
}
