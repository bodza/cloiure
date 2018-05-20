package graalvm.compiler.lir.alloc.trace.lsra;

import static jdk.vm.ci.code.ValueUtil.isRegister;
import static graalvm.compiler.lir.LIRValueUtil.asVariable;
import static graalvm.compiler.lir.LIRValueUtil.isStackSlotValue;
import static graalvm.compiler.lir.LIRValueUtil.isVariable;

import java.util.ArrayList;

import graalvm.compiler.core.common.alloc.RegisterAllocationConfig;
import graalvm.compiler.core.common.alloc.Trace;
import graalvm.compiler.core.common.alloc.TraceBuilderResult;
import graalvm.compiler.core.common.cfg.AbstractBlockBase;
import graalvm.compiler.lir.LIRInsertionBuffer;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.LIRInstruction.OperandMode;
import graalvm.compiler.lir.StandardOp.LoadConstantOp;
import graalvm.compiler.lir.StandardOp.MoveOp;
import graalvm.compiler.lir.StandardOp.ValueMoveOp;
import graalvm.compiler.lir.alloc.trace.lsra.TraceInterval.SpillState;
import graalvm.compiler.lir.alloc.trace.lsra.TraceLinearScanPhase.IntervalPredicate;
import graalvm.compiler.lir.alloc.trace.lsra.TraceLinearScanPhase.TraceLinearScan;
import graalvm.compiler.lir.gen.LIRGenerationResult;
import graalvm.compiler.lir.gen.LIRGeneratorTool.MoveFactory;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.AllocatableValue;

final class TraceLinearScanEliminateSpillMovePhase extends TraceLinearScanAllocationPhase
{
    private static final IntervalPredicate spilledIntervals = new TraceLinearScanPhase.IntervalPredicate()
    {
        @Override
        public boolean apply(TraceInterval i)
        {
            return i.isSplitParent() && SpillState.IN_MEMORY.contains(i.spillState());
        }
    };

    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, Trace trace, MoveFactory spillMoveFactory, RegisterAllocationConfig registerAllocationConfig, TraceBuilderResult traceBuilderResult, TraceLinearScan allocator)
    {
        boolean shouldEliminateSpillMoves = shouldEliminateSpillMoves(traceBuilderResult, allocator);
        eliminateSpillMoves(allocator, shouldEliminateSpillMoves, traceBuilderResult, lirGenRes);
    }

    private static boolean shouldEliminateSpillMoves(TraceBuilderResult traceBuilderResult, TraceLinearScan allocator)
    {
        return !traceBuilderResult.incomingSideEdges(traceBuilderResult.getTraceForBlock(allocator.blockAt(0)));
    }

    // called once before assignment of register numbers
    private static void eliminateSpillMoves(TraceLinearScan allocator, boolean shouldEliminateSpillMoves, TraceBuilderResult traceBuilderResult, LIRGenerationResult res)
    {
        allocator.sortIntervalsBySpillPos();

        /*
         * collect all intervals that must be stored after their definition. The list is sorted
         * by Interval.spillDefinitionPos.
         */
        TraceInterval interval = allocator.createUnhandledListBySpillPos(spilledIntervals);

        LIRInsertionBuffer insertionBuffer = new LIRInsertionBuffer();
        for (AbstractBlockBase<?> block : allocator.sortedBlocks())
        {
            ArrayList<LIRInstruction> instructions = allocator.getLIR().getLIRforBlock(block);
            int numInst = instructions.size();

            int lastOpId = -1;
            // iterate all instructions of the block.
            for (int j = 0; j < numInst; j++)
            {
                LIRInstruction op = instructions.get(j);
                int opId = op.id();
                if (opId == -1)
                {
                    MoveOp move = MoveOp.asMoveOp(op);
                    /*
                     * Remove move from register to stack if the stack slot is
                     * guaranteed to be correct. Only moves that have been inserted by
                     * LinearScan can be removed.
                     */
                    if (shouldEliminateSpillMoves && canEliminateSpillMove(allocator, block, move, lastOpId))
                    {
                        /*
                         * Move target is a stack slot that is always correct, so
                         * eliminate instruction.
                         */

                        // null-instructions are deleted by assignRegNum
                        instructions.set(j, null);
                    }
                }
                else
                {
                    lastOpId = opId;
                    /*
                     * Insert move from register to stack just after the beginning of
                     * the interval.
                     */

                    while (interval != TraceInterval.EndMarker && interval.spillDefinitionPos() == opId)
                    {
                        if (!interval.canMaterialize() && interval.spillState() != SpillState.StartInMemory)
                        {
                            AllocatableValue fromLocation = interval.getSplitChildAtOpId(opId, OperandMode.DEF).location();
                            AllocatableValue toLocation = allocator.canonicalSpillOpr(interval);
                            if (!fromLocation.equals(toLocation))
                            {
                                if (!insertionBuffer.initialized())
                                {
                                    /*
                                     * prepare insertion buffer (appended when all
                                     * instructions in the block are processed)
                                     */
                                    insertionBuffer.init(instructions);
                                }

                                LIRInstruction move = allocator.getSpillMoveFactory().createMove(toLocation, fromLocation);
                                insertionBuffer.append(j + 1, move);
                                move.setComment(res, "TraceLSRAEliminateSpillMove: spill def pos");
                            }
                        }
                        interval = interval.next;
                    }
                }
            }   // end of instruction iteration

            if (insertionBuffer.initialized())
            {
                insertionBuffer.finish();
            }
        }   // end of block iteration
    }

    /**
     * @param block The block {@code move} is located in.
     * @param move Spill move.
     * @param lastOpId The id of last "normal" instruction before the spill move. (Spill moves have no valid opId but -1.)
     */
    private static boolean canEliminateSpillMove(TraceLinearScan allocator, AbstractBlockBase<?> block, MoveOp move, int lastOpId)
    {
        TraceInterval curInterval = allocator.intervalFor(asVariable(move.getResult()));

        if (!isRegister(curInterval.location()) && curInterval.inMemoryAt(lastOpId) && !isPhiResolutionMove(allocator, move))
        {
            /* Phi resolution moves cannot be removed because they define the value. */
            // TODO (je) check if the comment is still valid!
            return true;
        }
        return false;
    }

    /**
     * Checks if a (spill or split) move is a Phi resolution move.
     *
     * A spill or split move connects a split parent or a split child with another split child.
     * Therefore the destination of the move is always a split child. Phi resolution moves look like
     * spill moves (i.e. {@link LIRInstruction#id() id} is {@code 0}, but they define a new
     * variable. As a result the destination interval is a split parent.
     */
    private static boolean isPhiResolutionMove(TraceLinearScan allocator, MoveOp move)
    {
        TraceInterval curInterval = allocator.intervalFor(asVariable(move.getResult()));
        return curInterval.isSplitParent();
    }
}
