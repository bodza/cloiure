package giraaff.lir.alloc.lsra;

import java.util.ArrayList;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.code.ValueUtil;
import jdk.vm.ci.meta.AllocatableValue;

import giraaff.core.common.GraalOptions;
import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.lir.LIRInsertionBuffer;
import giraaff.lir.LIRInstruction;
import giraaff.lir.StandardOp.MoveOp;
import giraaff.lir.alloc.lsra.Interval.SpillState;
import giraaff.lir.alloc.lsra.LinearScan.IntervalPredicate;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.phases.AllocationPhase.AllocationContext;
import giraaff.lir.phases.LIRPhase;

// @class LinearScanEliminateSpillMovePhase
public class LinearScanEliminateSpillMovePhase extends LinearScanAllocationPhase
{
    // @closure
    private static final IntervalPredicate mustStoreAtDefinition = new LinearScan.IntervalPredicate()
    {
        @Override
        public boolean apply(Interval __i)
        {
            return __i.isSplitParent() && __i.spillState() == SpillState.StoreAtDefinition;
        }
    };

    // @field
    protected final LinearScan allocator;

    // @cons
    protected LinearScanEliminateSpillMovePhase(LinearScan __allocator)
    {
        super();
        this.allocator = __allocator;
    }

    @Override
    protected void run(TargetDescription __target, LIRGenerationResult __lirGenRes, AllocationContext __context)
    {
        eliminateSpillMoves(__lirGenRes);
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
    void eliminateSpillMoves(LIRGenerationResult __res)
    {
        /*
         * Collect all intervals that must be stored after their definition. The list is sorted
         * by Interval.spillDefinitionPos.
         */
        Interval __interval = allocator.createUnhandledLists(mustStoreAtDefinition, null).getLeft();

        LIRInsertionBuffer __insertionBuffer = new LIRInsertionBuffer();
        for (AbstractBlockBase<?> __block : allocator.sortedBlocks())
        {
            ArrayList<LIRInstruction> __instructions = allocator.getLIR().getLIRforBlock(__block);
            int __numInst = __instructions.size();

            // iterate all instructions of the block.
            for (int __j = firstInstructionOfInterest(); __j < __numInst; __j++)
            {
                LIRInstruction __op = __instructions.get(__j);
                int __opId = __op.id();

                if (__opId == -1)
                {
                    MoveOp __move = MoveOp.asMoveOp(__op);
                    /*
                     * Remove move from register to stack if the stack slot is guaranteed to
                     * be correct. Only moves that have been inserted by LinearScan can be removed.
                     */
                    if (GraalOptions.lirOptLSRAEliminateSpillMoves && canEliminateSpillMove(__block, __move))
                    {
                        // Move target is a stack slot that is always correct, so eliminate instruction.

                        // null-instructions are deleted by assignRegNum
                        __instructions.set(__j, null);
                    }
                }
                else
                {
                    // Insert move from register to stack just after the beginning of the interval.
                    while (!__interval.isEndMarker() && __interval.spillDefinitionPos() == __opId)
                    {
                        if (!__interval.canMaterialize())
                        {
                            if (!__insertionBuffer.initialized())
                            {
                                // prepare insertion buffer (appended when all instructions in the block are processed)
                                __insertionBuffer.init(__instructions);
                            }

                            AllocatableValue __fromLocation = __interval.location();
                            AllocatableValue __toLocation = LinearScan.canonicalSpillOpr(__interval);
                            if (!__fromLocation.equals(__toLocation))
                            {
                                LIRInstruction __move = allocator.getSpillMoveFactory().createMove(__toLocation, __fromLocation);
                                __insertionBuffer.append(__j + 1, __move);
                            }
                        }
                        __interval = __interval.next;
                    }
                }
            }

            if (__insertionBuffer.initialized())
            {
                __insertionBuffer.finish();
            }
        }
    }

    /**
     * @param block The block {@code move} is located in.
     * @param move Spill move.
     */
    protected boolean canEliminateSpillMove(AbstractBlockBase<?> __block, MoveOp __move)
    {
        Interval __curInterval = allocator.intervalFor(__move.getResult());

        if (!ValueUtil.isRegister(__curInterval.location()) && __curInterval.alwaysInMemory())
        {
            return true;
        }
        return false;
    }
}
