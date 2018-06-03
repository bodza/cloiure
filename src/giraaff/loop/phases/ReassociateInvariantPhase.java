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
// @class ReassociateInvariantPhase
public final class ReassociateInvariantPhase extends Phase
{
    @Override
    protected void run(StructuredGraph __graph)
    {
        int __iterations = 0;
        boolean __changed = true;
        while (__changed)
        {
            __changed = false;
            final LoopsData __dataReassociate = new LoopsData(__graph);
            for (LoopEx __loop : __dataReassociate.loops())
            {
                __changed |= __loop.reassociateInvariants();
            }
            __dataReassociate.deleteUnusedNodes();
            __iterations++;
        }
    }
}
