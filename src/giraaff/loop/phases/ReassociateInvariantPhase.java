package giraaff.loop.phases;

import giraaff.loop.LoopEx;
import giraaff.loop.LoopsData;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.calc.BinaryArithmeticNode;
import giraaff.phases.Phase;

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
