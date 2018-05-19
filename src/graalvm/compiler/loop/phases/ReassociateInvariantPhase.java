package graalvm.compiler.loop.phases;

import graalvm.compiler.loop.LoopEx;
import graalvm.compiler.loop.LoopsData;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.calc.BinaryArithmeticNode;
import graalvm.compiler.phases.Phase;

/**
 * Rearrange {@link BinaryArithmeticNode#isAssociative() associative binary operations} so that
 * invariant parts of the expression can move outside of the loop.
 */
public class ReassociateInvariantPhase extends Phase
{
    @Override
    protected void run(StructuredGraph graph)
    {
        int iterations = 0;
        boolean changed = true;
        while (changed)
        {
            changed = false;
            final LoopsData dataReassociate = new LoopsData(graph);
            for (LoopEx loop : dataReassociate.loops())
            {
                changed |= loop.reassociateInvariants();
            }
            dataReassociate.deleteUnusedNodes();
            iterations++;
        }
    }
}
