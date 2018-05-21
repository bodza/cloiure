package graalvm.compiler.lir.alloc.lsra;

import static jdk.vm.ci.code.ValueUtil.isRegister;
import static graalvm.compiler.lir.phases.LIRPhase.Options.LIROptimization;

import java.util.ArrayList;

import graalvm.compiler.core.common.cfg.AbstractBlockBase;
import graalvm.compiler.lir.LIRInsertionBuffer;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.StandardOp.MoveOp;
import graalvm.compiler.lir.alloc.lsra.Interval.SpillState;
import graalvm.compiler.lir.alloc.lsra.LinearScan.IntervalPredicate;
import graalvm.compiler.lir.gen.LIRGenerationResult;
import graalvm.compiler.lir.phases.AllocationPhase.AllocationContext;
import graalvm.compiler.options.NestedBooleanOptionKey;
import graalvm.compiler.options.OptionKey;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.AllocatableValue;

public class LinearScanEliminateSpillMovePhase extends LinearScanAllocationPhase
{
    public static class Options
    {
        // "Enable spill move elimination."
        public static final OptionKey<Boolean> LIROptLSRAEliminateSpillMoves = new NestedBooleanOptionKey(LIROptimization, true);
    }

    private static final IntervalPredicate mustStoreAtDefinition = new LinearScan.IntervalPredicate()
    {
        @Override
        public boolean apply(Interval i)
        {
            return i.isSplitParent() && i.spillState() == SpillState.StoreAtDefinition;
        }
    };

    protected final LinearScan allocator;

    protected LinearScanEliminateSpillMovePhase(LinearScan allocator)
    {
        this.allocator = allocator;
    }

    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, AllocationContext context)
    {
        eliminateSpillMoves(lirGenRes);
    }

    /**
     * @return the index of the first instruction that is of interest for
     *         {@link #eliminateSpillMoves}
     */
    protected int firstInstructionOfInterest()
    {
        // skip the first because it is always a label
        return 1;
    }

    // called once before assignment of register numbers
    void eliminateSpillMoves(LIRGenerationResult res)
    {
        /*
         * collect all intervals that must be stored after their definition. The list is sorted
         * by Interval.spillDefinitionPos.
         */
        Interval interval = allocator.createUnhandledLists(mustStoreAtDefinition, null).getLeft();

        LIRInsertionBuffer insertionBuffer = new LIRInsertionBuffer();
        for (AbstractBlockBase<?> block : allocator.sortedBlocks())
        {
            ArrayList<LIRInstruction> instructions = allocator.getLIR().getLIRforBlock(block);
            int numInst = instructions.size();

            // iterate all instructions of the block.
            for (int j = firstInstructionOfInterest(); j < numInst; j++)
            {
                LIRInstruction op = instructions.get(j);
                int opId = op.id();

                if (opId == -1)
                {
                    MoveOp move = MoveOp.asMoveOp(op);
                    /*
                     * Remove move from register to stack if the stack slot is guaranteed to
                     * be correct. Only moves that have been inserted by LinearScan can be
                     * removed.
                     */
                    if (Options.LIROptLSRAEliminateSpillMoves.getValue(allocator.getOptions()) && canEliminateSpillMove(block, move))
                    {
                        /*
                         * Move target is a stack slot that is always correct, so eliminate
                         * instruction.
                         */

                        // null-instructions are deleted by assignRegNum
                        instructions.set(j, null);
                    }
                }
                else
                {
                    /*
                     * Insert move from register to stack just after the beginning of the
                     * interval.
                     */

                    while (!interval.isEndMarker() && interval.spillDefinitionPos() == opId)
                    {
                        if (!interval.canMaterialize())
                        {
                            if (!insertionBuffer.initialized())
                            {
                                /*
                                 * prepare insertion buffer (appended when all instructions
                                 * in the block are processed)
                                 */
                                insertionBuffer.init(instructions);
                            }

                            AllocatableValue fromLocation = interval.location();
                            AllocatableValue toLocation = LinearScan.canonicalSpillOpr(interval);
                            if (!fromLocation.equals(toLocation))
                            {
                                LIRInstruction move = allocator.getSpillMoveFactory().createMove(toLocation, fromLocation);
                                insertionBuffer.append(j + 1, move);
                                move.setComment(res, "LSRAEliminateSpillMove: store at definition");
                            }
                        }
                        interval = interval.next;
                    }
                }
            } // end of instruction iteration

            if (insertionBuffer.initialized())
            {
                insertionBuffer.finish();
            }
        } // end of block iteration
    }

    /**
     * @param block The block {@code move} is located in.
     * @param move Spill move.
     */
    protected boolean canEliminateSpillMove(AbstractBlockBase<?> block, MoveOp move)
    {
        Interval curInterval = allocator.intervalFor(move.getResult());

        if (!isRegister(curInterval.location()) && curInterval.alwaysInMemory())
        {
            return true;
        }
        return false;
    }
}
