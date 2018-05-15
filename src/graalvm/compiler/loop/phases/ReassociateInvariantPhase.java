package graalvm.compiler.loop.phases;

import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.loop.LoopEx;
import graalvm.compiler.loop.LoopsData;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.calc.BinaryArithmeticNode;
import graalvm.compiler.phases.Phase;

/**
 * Rearrange {@link BinaryArithmeticNode#isAssociative() associative binary operations} so that
 * invariant parts of the expression can move outside of the loop.
 */
public class ReassociateInvariantPhase extends Phase {

    @SuppressWarnings("try")
    @Override
    protected void run(StructuredGraph graph) {
        int iterations = 0;
        DebugContext debug = graph.getDebug();
        try (DebugContext.Scope s = debug.scope("ReassociateInvariants")) {
            boolean changed = true;
            while (changed) {
                changed = false;
                final LoopsData dataReassociate = new LoopsData(graph);
                for (LoopEx loop : dataReassociate.loops()) {
                    changed |= loop.reassociateInvariants();
                }
                dataReassociate.deleteUnusedNodes();
                iterations++;
                debug.dump(DebugContext.VERBOSE_LEVEL, graph, "after iteration %d", iterations);
            }
        } catch (Throwable e) {
            throw debug.handle(e);
        }
    }
}
