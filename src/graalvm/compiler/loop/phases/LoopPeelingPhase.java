package graalvm.compiler.loop.phases;

import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.loop.LoopEx;
import graalvm.compiler.loop.LoopPolicies;
import graalvm.compiler.loop.LoopsData;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.phases.tiers.PhaseContext;

public class LoopPeelingPhase extends LoopPhase<LoopPolicies> {

    public LoopPeelingPhase(LoopPolicies policies) {
        super(policies);
    }

    @Override
    @SuppressWarnings("try")
    protected void run(StructuredGraph graph, PhaseContext context) {
        DebugContext debug = graph.getDebug();
        if (graph.hasLoops()) {
            LoopsData data = new LoopsData(graph);
            try (DebugContext.Scope s = debug.scope("peeling", data.getCFG())) {
                for (LoopEx loop : data.outerFirst()) {
                    if (getPolicies().shouldPeel(loop, data.getCFG(), context.getMetaAccess())) {
                        debug.log("Peeling %s", loop);
                        LoopTransformations.peel(loop);
                        debug.dump(DebugContext.DETAILED_LEVEL, graph, "Peeling %s", loop);
                    }
                }
                data.deleteUnusedNodes();
            } catch (Throwable t) {
                throw debug.handle(t);
            }
        }
    }

    @Override
    public float codeSizeIncrease() {
        return 5.0f;
    }
}
