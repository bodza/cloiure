package graalvm.compiler.phases.common;

import static graalvm.compiler.core.common.GraalOptions.GenLoopSafepoints;

import graalvm.compiler.debug.DebugCloseable;
import graalvm.compiler.nodes.LoopBeginNode;
import graalvm.compiler.nodes.LoopEndNode;
import graalvm.compiler.nodes.SafepointNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.phases.Phase;

/**
 * Adds safepoints to loops.
 */
public class LoopSafepointInsertionPhase extends Phase {

    @Override
    public boolean checkContract() {
        // the size / cost after is highly dynamic and dependent on the graph, thus we do not verify
        // costs for this phase
        return false;
    }

    @Override
    @SuppressWarnings("try")
    protected void run(StructuredGraph graph) {
        if (GenLoopSafepoints.getValue(graph.getOptions())) {
            for (LoopBeginNode loopBeginNode : graph.getNodes(LoopBeginNode.TYPE)) {
                for (LoopEndNode loopEndNode : loopBeginNode.loopEnds()) {
                    if (loopEndNode.canSafepoint()) {
                        try (DebugCloseable s = loopEndNode.withNodeSourcePosition()) {
                            SafepointNode safepointNode = graph.add(new SafepointNode());
                            graph.addBeforeFixed(loopEndNode, safepointNode);
                        }
                    }
                }
            }
        }
    }
}
