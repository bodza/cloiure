package graalvm.compiler.loop.phases;

import graalvm.compiler.debug.CounterKey;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.loop.LoopEx;
import graalvm.compiler.loop.LoopPolicies;
import graalvm.compiler.loop.LoopsData;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.phases.common.CanonicalizerPhase;
import graalvm.compiler.phases.tiers.PhaseContext;

public class LoopFullUnrollPhase extends LoopPhase<LoopPolicies> {

    private static final CounterKey FULLY_UNROLLED_LOOPS = DebugContext.counter("FullUnrolls");
    private final CanonicalizerPhase canonicalizer;

    public LoopFullUnrollPhase(CanonicalizerPhase canonicalizer, LoopPolicies policies) {
        super(policies);
        this.canonicalizer = canonicalizer;
    }

    @Override
    protected void run(StructuredGraph graph, PhaseContext context) {
        DebugContext debug = graph.getDebug();
        if (graph.hasLoops()) {
            boolean peeled;
            do {
                peeled = false;
                final LoopsData dataCounted = new LoopsData(graph);
                dataCounted.detectedCountedLoops();
                for (LoopEx loop : dataCounted.countedLoops()) {
                    if (getPolicies().shouldFullUnroll(loop)) {
                        debug.log("FullUnroll %s", loop);
                        LoopTransformations.fullUnroll(loop, context, canonicalizer);
                        FULLY_UNROLLED_LOOPS.increment(debug);
                        debug.dump(DebugContext.DETAILED_LEVEL, graph, "FullUnroll %s", loop);
                        peeled = true;
                        break;
                    }
                }
                dataCounted.deleteUnusedNodes();
            } while (peeled);
        }
    }

    @Override
    public boolean checkContract() {
        return false;
    }
}
