package graalvm.compiler.phases.common;

import static graalvm.compiler.core.common.GraalOptions.GenLoopSafepoints;

import graalvm.compiler.nodes.LoopBeginNode;
import graalvm.compiler.nodes.LoopEndNode;
import graalvm.compiler.nodes.SafepointNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.phases.Phase;

/**
 * Adds safepoints to loops.
 */
public class LoopSafepointInsertionPhase extends Phase
{
    @Override
    protected void run(StructuredGraph graph)
    {
        if (GenLoopSafepoints.getValue(graph.getOptions()))
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
