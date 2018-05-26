package giraaff.lir.alloc.lsra;

import java.util.ArrayList;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;

import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.lir.LIRInsertionBuffer;
import giraaff.lir.LIRInstruction;
import giraaff.lir.StandardOp.MoveOp;
import giraaff.lir.alloc.lsra.Interval.SpillState;
import giraaff.lir.alloc.lsra.LinearScan.IntervalPredicate;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.phases.AllocationPhase.AllocationContext;
import giraaff.lir.phases.LIRPhase;
import giraaff.options.NestedBooleanOptionKey;
import giraaff.options.OptionKey;

public class LinearScanEliminateSpillMovePhase extends LinearScanAllocationPhase
{
    public static class Options
    {
        // @Option "Enable spill move elimination."
        public static final OptionKey<Boolean> LIROptLSRAEliminateSpillMoves = new NestedBooleanOptionKey(LIRPhase.Options.LIROptimization, true);
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
         * Collect all intervals that must be stored after their definition. The list is sorted
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
                     * be correct. Only moves that have been inserted by LinearScan can be removed.
                     */
                    if (Options.LIROptLSRAEliminateSpillMoves.getValue(allocator.getOptions()) && canEliminateSpillMove(block, move))
                    {
                        // Move target is a stack slot that is always correct, so eliminate instruction.

                        // null-instructions are deleted by assignRegNum
                        instructions.set(j, null);
                    }
                }
                else
                {
                    // Insert move from register to stack just after the beginning of the interval.
                    while (!interval.isEndMarker() && interval.spillDefinitionPos() == opId)
                    {
                        if (!interval.canMaterialize())
                        {
                            if (!insertionBuffer.initialized())
                            {
                                // prepare insertion buffer (appended when all instructions in the block are processed)
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
            }

            if (insertionBuffer.initialized())
            {
                insertionBuffer.finish();
            }
        }
    }

    /**
     * @param block The block {@code move} is located in.
     * @param move Spill move.
     */
    protected boolean canEliminateSpillMove(AbstractBlockBase<?> block, MoveOp move)
    {
        Interval curInterval = allocator.intervalFor(move.getResult());

        if (!ValueUtil.isRegister(curInterval.location()) && curInterval.alwaysInMemory())
        {
            return true;
        }
        return false;
    }
}
