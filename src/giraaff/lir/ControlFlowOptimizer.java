package giraaff.lir;

import java.util.ArrayList;

import jdk.vm.ci.code.TargetDescription;

import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.phases.PostAllocationOptimizationPhase;

///
// This class performs basic optimizations on the control flow graph after LIR generation.
///
// @class ControlFlowOptimizer
public final class ControlFlowOptimizer extends PostAllocationOptimizationPhase
{
    ///
    // Performs control flow optimizations on the given LIR graph.
    ///
    @Override
    protected void run(TargetDescription __target, LIRGenerationResult __lirGenRes, PostAllocationOptimizationContext __context)
    {
        LIR __lir = __lirGenRes.getLIR();
        new Optimizer(__lir).deleteEmptyBlocks(__lir.codeEmittingOrder());
    }

    // @class ControlFlowOptimizer.Optimizer
    private static final class Optimizer
    {
        // @field
        private final LIR ___lir;

        // @cons
        private Optimizer(LIR __lir)
        {
            super();
            this.___lir = __lir;
        }

        ///
        // Checks whether a block can be deleted. Only blocks with exactly one successor and an
        // unconditional branch to this successor are eligable.
        //
        // @param block the block checked for deletion
        // @return whether the block can be deleted
        ///
        private boolean canDeleteBlock(AbstractBlockBase<?> __block)
        {
            if (__block == null || __block.getSuccessorCount() != 1 || __block.getPredecessorCount() == 0 || __block.getSuccessors()[0] == __block)
            {
                return false;
            }

            ArrayList<LIRInstruction> __instructions = this.___lir.getLIRforBlock(__block);

            // Block must have exactly one successor.
            return __instructions.size() == 2 && !__block.isExceptionEntry();
        }

        private void alignBlock(AbstractBlockBase<?> __block)
        {
            if (!__block.isAligned())
            {
                __block.setAlign(true);
                ArrayList<LIRInstruction> __instructions = this.___lir.getLIRforBlock(__block);
                StandardOp.LabelOp __label = (StandardOp.LabelOp) __instructions.get(0);
                __instructions.set(0, new StandardOp.LabelOp(__label.getLabel(), true));
            }
        }

        private void deleteEmptyBlocks(AbstractBlockBase<?>[] __blocks)
        {
            for (int __i = 0; __i < __blocks.length; __i++)
            {
                AbstractBlockBase<?> __block = __blocks[__i];
                if (canDeleteBlock(__block))
                {
                    __block.delete();
                    // adjust successor and predecessor lists
                    AbstractBlockBase<?> __other = __block.getSuccessors()[0];
                    if (__block.isAligned())
                    {
                        alignBlock(__other);
                    }

                    __blocks[__i] = null;
                }
            }
        }
    }
}
