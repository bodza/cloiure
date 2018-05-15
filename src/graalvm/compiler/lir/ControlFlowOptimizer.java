package graalvm.compiler.lir;

import static graalvm.compiler.lir.LIR.verifyBlocks;

import java.util.ArrayList;

import graalvm.compiler.core.common.cfg.AbstractBlockBase;
import graalvm.compiler.debug.CounterKey;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.lir.gen.LIRGenerationResult;
import graalvm.compiler.lir.phases.PostAllocationOptimizationPhase;

import jdk.vm.ci.code.TargetDescription;

/**
 * This class performs basic optimizations on the control flow graph after LIR generation.
 */
public final class ControlFlowOptimizer extends PostAllocationOptimizationPhase {

    /**
     * Performs control flow optimizations on the given LIR graph.
     */
    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, PostAllocationOptimizationContext context) {
        LIR lir = lirGenRes.getLIR();
        new Optimizer(lir).deleteEmptyBlocks(lir.codeEmittingOrder());
    }

    private static final class Optimizer {

        private final LIR lir;

        private Optimizer(LIR lir) {
            this.lir = lir;
        }

        private static final CounterKey BLOCKS_DELETED = DebugContext.counter("BlocksDeleted");

        /**
         * Checks whether a block can be deleted. Only blocks with exactly one successor and an
         * unconditional branch to this successor are eligable.
         *
         * @param block the block checked for deletion
         * @return whether the block can be deleted
         */
        private boolean canDeleteBlock(AbstractBlockBase<?> block) {
            if (block == null || block.getSuccessorCount() != 1 || block.getPredecessorCount() == 0 || block.getSuccessors()[0] == block) {
                return false;
            }

            ArrayList<LIRInstruction> instructions = lir.getLIRforBlock(block);

            assert instructions.size() >= 2 : "block must have label and branch";
            assert instructions.get(0) instanceof StandardOp.LabelOp : "first instruction must always be a label";
            assert instructions.get(instructions.size() - 1) instanceof StandardOp.JumpOp : "last instruction must always be a branch";
            assert ((StandardOp.JumpOp) instructions.get(instructions.size() - 1)).destination().label() == ((StandardOp.LabelOp) lir.getLIRforBlock(block.getSuccessors()[0]).get(
                            0)).getLabel() : "branch target must be the successor";

            // Block must have exactly one successor.
            return instructions.size() == 2 && !instructions.get(instructions.size() - 1).hasState() && !block.isExceptionEntry();
        }

        private void alignBlock(AbstractBlockBase<?> block) {
            if (!block.isAligned()) {
                block.setAlign(true);
                ArrayList<LIRInstruction> instructions = lir.getLIRforBlock(block);
                assert instructions.get(0) instanceof StandardOp.LabelOp : "first instruction must always be a label";
                StandardOp.LabelOp label = (StandardOp.LabelOp) instructions.get(0);
                instructions.set(0, new StandardOp.LabelOp(label.getLabel(), true));
            }
        }

        private void deleteEmptyBlocks(AbstractBlockBase<?>[] blocks) {
            assert verifyBlocks(lir, blocks);
            for (int i = 0; i < blocks.length; i++) {
                AbstractBlockBase<?> block = blocks[i];
                if (canDeleteBlock(block)) {

                    block.delete();
                    // adjust successor and predecessor lists
                    AbstractBlockBase<?> other = block.getSuccessors()[0];
                    if (block.isAligned()) {
                        alignBlock(other);
                    }

                    BLOCKS_DELETED.increment(lir.getDebug());
                    blocks[i] = null;
                }
            }
            assert verifyBlocks(lir, blocks);
        }
    }
}
