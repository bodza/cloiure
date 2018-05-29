package giraaff.lir;

import java.util.ArrayList;

import jdk.vm.ci.code.TargetDescription;

import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.phases.PostAllocationOptimizationPhase;

/**
 * This class performs basic optimizations on the control flow graph after LIR generation.
 */
// @class ControlFlowOptimizer
public final class ControlFlowOptimizer extends PostAllocationOptimizationPhase
{
    /**
     * Performs control flow optimizations on the given LIR graph.
     */
    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, PostAllocationOptimizationContext context)
    {
        LIR lir = lirGenRes.getLIR();
        new Optimizer(lir).deleteEmptyBlocks(lir.codeEmittingOrder());
    }

    // @class ControlFlowOptimizer.Optimizer
    private static final class Optimizer
    {
        private final LIR lir;

        // @cons
        private Optimizer(LIR lir)
        {
            super();
            this.lir = lir;
        }

        /**
         * Checks whether a block can be deleted. Only blocks with exactly one successor and an
         * unconditional branch to this successor are eligable.
         *
         * @param block the block checked for deletion
         * @return whether the block can be deleted
         */
        private boolean canDeleteBlock(AbstractBlockBase<?> block)
        {
            if (block == null || block.getSuccessorCount() != 1 || block.getPredecessorCount() == 0 || block.getSuccessors()[0] == block)
            {
                return false;
            }

            ArrayList<LIRInstruction> instructions = lir.getLIRforBlock(block);

            // Block must have exactly one successor.
            return instructions.size() == 2 && !block.isExceptionEntry();
        }

        private void alignBlock(AbstractBlockBase<?> block)
        {
            if (!block.isAligned())
            {
                block.setAlign(true);
                ArrayList<LIRInstruction> instructions = lir.getLIRforBlock(block);
                StandardOp.LabelOp label = (StandardOp.LabelOp) instructions.get(0);
                instructions.set(0, new StandardOp.LabelOp(label.getLabel(), true));
            }
        }

        private void deleteEmptyBlocks(AbstractBlockBase<?>[] blocks)
        {
            for (int i = 0; i < blocks.length; i++)
            {
                AbstractBlockBase<?> block = blocks[i];
                if (canDeleteBlock(block))
                {
                    block.delete();
                    // adjust successor and predecessor lists
                    AbstractBlockBase<?> other = block.getSuccessors()[0];
                    if (block.isAligned())
                    {
                        alignBlock(other);
                    }

                    blocks[i] = null;
                }
            }
        }
    }
}
