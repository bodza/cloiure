package giraaff.lir;

import java.util.ArrayList;
import java.util.List;

import jdk.vm.ci.code.TargetDescription;

import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.lir.StandardOp;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.phases.PostAllocationOptimizationPhase;

///
// This class optimizes moves, particularly those that result from eliminating SSA form.
//
// When a block has more than one predecessor, and all predecessors end with the
// {@linkplain EdgeMoveOptimizer.EMOptimizer#same(LIRInstruction, LIRInstruction) same} sequence of
// {@linkplain StandardOp.MoveOp move} instructions, then these sequences can be replaced with a
// single copy of the sequence at the beginning of the block.
//
// Similarly, when a block has more than one successor, then same sequences of moves at the
// beginning of the successors can be placed once at the end of the block. But because the moves
// must be inserted before all branch instructions, this works only when there is exactly one
// conditional branch at the end of the block (because the moves must be inserted before all
// branches, but after all compares).
//
// This optimization affects all kind of moves (reg->reg, reg->stack and stack->reg).
// Because this optimization works best when a block contains only a few moves, it has a huge impact
// on the number of blocks that are totally empty.
///
// @class EdgeMoveOptimizer
public final class EdgeMoveOptimizer extends PostAllocationOptimizationPhase
{
    @Override
    protected void run(TargetDescription __target, LIRGenerationResult __lirGenRes, PostAllocationOptimizationPhase.PostAllocationOptimizationContext __context)
    {
        LIR __ir = __lirGenRes.getLIR();
        EdgeMoveOptimizer.EMOptimizer __optimizer = new EdgeMoveOptimizer.EMOptimizer(__ir);

        AbstractBlockBase<?>[] __blockList = __ir.linearScanOrder();
        // ignore the first block in the list (index 0 is not processed)
        for (int __i = __blockList.length - 1; __i >= 1; __i--)
        {
            AbstractBlockBase<?> __block = __blockList[__i];

            if (__block.getPredecessorCount() > 1)
            {
                __optimizer.optimizeMovesAtBlockEnd(__block);
            }
            if (__block.getSuccessorCount() == 2)
            {
                __optimizer.optimizeMovesAtBlockBegin(__block);
            }
        }
    }

    // @class EdgeMoveOptimizer.EMOptimizer
    private static final class EMOptimizer
    {
        // @field
        private final List<List<LIRInstruction>> ___edgeInstructionSeqences;
        // @field
        private LIR ___ir;

        // @cons EdgeMoveOptimizer.EMOptimizer
        EMOptimizer(LIR __ir)
        {
            super();
            this.___ir = __ir;
            this.___edgeInstructionSeqences = new ArrayList<>(4);
        }

        ///
        // Determines if two operations are both {@linkplain StandardOp.MoveOp moves} that have the
        // same source and {@linkplain StandardOp.MoveOp#getResult() destination} operands.
        //
        // @param op1 the first instruction to compare
        // @param op2 the second instruction to compare
        // @return {@code true} if {@code op1} and {@code op2} are the same by the above algorithm
        ///
        private static boolean same(LIRInstruction __op1, LIRInstruction __op2)
        {
            if (StandardOp.ValueMoveOp.isValueMoveOp(__op1) && StandardOp.ValueMoveOp.isValueMoveOp(__op2))
            {
                StandardOp.ValueMoveOp __move1 = StandardOp.ValueMoveOp.asValueMoveOp(__op1);
                StandardOp.ValueMoveOp __move2 = StandardOp.ValueMoveOp.asValueMoveOp(__op2);
                if (__move1.getInput().equals(__move2.getInput()) && __move1.getResult().equals(__move2.getResult()))
                {
                    // these moves are exactly equal and can be optimized
                    return true;
                }
            }
            else if (StandardOp.LoadConstantOp.isLoadConstantOp(__op1) && StandardOp.LoadConstantOp.isLoadConstantOp(__op2))
            {
                StandardOp.LoadConstantOp __move1 = StandardOp.LoadConstantOp.asLoadConstantOp(__op1);
                StandardOp.LoadConstantOp __move2 = StandardOp.LoadConstantOp.asLoadConstantOp(__op2);
                if (__move1.getConstant().equals(__move2.getConstant()) && __move1.getResult().equals(__move2.getResult()))
                {
                    // these moves are exactly equal and can be optimized
                    return true;
                }
            }
            return false;
        }

        ///
        // Moves the longest {@linkplain #same common} subsequence at the end all predecessors of
        // {@code block} to the start of {@code block}.
        ///
        private void optimizeMovesAtBlockEnd(AbstractBlockBase<?> __block)
        {
            for (AbstractBlockBase<?> __pred : __block.getPredecessors())
            {
                if (__pred == __block)
                {
                    // currently we can't handle this correctly.
                    return;
                }
            }

            // clear all internal data structures
            this.___edgeInstructionSeqences.clear();

            int __numPreds = __block.getPredecessorCount();

            // setup a list with the LIR instructions of all predecessors
            for (AbstractBlockBase<?> __pred : __block.getPredecessors())
            {
                ArrayList<LIRInstruction> __predInstructions = this.___ir.getLIRforBlock(__pred);

                if (__pred.getSuccessorCount() != 1)
                {
                    // this can happen with switch-statements where multiple edges are between
                    // the same blocks.
                    return;
                }

                // ignore the unconditional branch at the end of the block
                List<LIRInstruction> __seq = __predInstructions.subList(0, __predInstructions.size() - 1);
                this.___edgeInstructionSeqences.add(__seq);
            }

            // process lir-instructions while all predecessors end with the same instruction
            while (true)
            {
                List<LIRInstruction> __seq = this.___edgeInstructionSeqences.get(0);
                if (__seq.isEmpty())
                {
                    return;
                }

                LIRInstruction __op = last(__seq);
                for (int __i = 1; __i < __numPreds; ++__i)
                {
                    List<LIRInstruction> __otherSeq = this.___edgeInstructionSeqences.get(__i);
                    if (__otherSeq.isEmpty() || !same(__op, last(__otherSeq)))
                    {
                        return;
                    }
                }

                // insert the instruction at the beginning of the current block
                this.___ir.getLIRforBlock(__block).add(1, __op);

                // delete the instruction at the end of all predecessors
                for (int __i = 0; __i < __numPreds; __i++)
                {
                    __seq = this.___edgeInstructionSeqences.get(__i);
                    removeLast(__seq);
                }
            }
        }

        ///
        // Moves the longest {@linkplain #same common} subsequence at the start of all successors of
        // {@code block} to the end of {@code block} just prior to the branch instruction ending {@code block}.
        ///
        private void optimizeMovesAtBlockBegin(AbstractBlockBase<?> __block)
        {
            this.___edgeInstructionSeqences.clear();
            int __numSux = __block.getSuccessorCount();

            ArrayList<LIRInstruction> __instructions = this.___ir.getLIRforBlock(__block);

            LIRInstruction __branch = __instructions.get(__instructions.size() - 1);
            if (!(__branch instanceof StandardOp.StandardBranchOp) || __branch.hasOperands())
            {
                // Only blocks that end with a conditional branch are optimized. In addition,
                // a conditional branch with operands (including state) cannot be optimized.
                // Moving a successor instruction before such a branch may interfere with
                // the operands of the branch. For example, a successive move instruction
                // may redefine an input operand of the branch.
                return;
            }

            // Now it is guaranteed that the block ends with a conditional branch.
            // The instructions are inserted at the end of the block before the branch.
            int __insertIdx = __instructions.size() - 1;

            // setup a list with the lir-instructions of all successors
            for (AbstractBlockBase<?> __sux : __block.getSuccessors())
            {
                ArrayList<LIRInstruction> __suxInstructions = this.___ir.getLIRforBlock(__sux);

                if (__sux.getPredecessorCount() != 1)
                {
                    // this can happen with switch-statements where multiple edges are between the same blocks
                    return;
                }

                // ignore the label at the beginning of the block
                List<LIRInstruction> __seq = __suxInstructions.subList(1, __suxInstructions.size());
                this.___edgeInstructionSeqences.add(__seq);
            }

            // process LIR instructions while all successors begin with the same instruction
            while (true)
            {
                List<LIRInstruction> __seq = this.___edgeInstructionSeqences.get(0);
                if (__seq.isEmpty())
                {
                    return;
                }

                LIRInstruction __op = first(__seq);
                for (int __i = 1; __i < __numSux; __i++)
                {
                    List<LIRInstruction> __otherSeq = this.___edgeInstructionSeqences.get(__i);
                    if (__otherSeq.isEmpty() || !same(__op, first(__otherSeq)))
                    {
                        // these instructions are different and cannot be optimized .
                        // no further optimization possible
                        return;
                    }
                }

                // insert instruction at end of current block
                this.___ir.getLIRforBlock(__block).add(__insertIdx, __op);
                __insertIdx++;

                // delete the instructions at the beginning of all successors
                for (int __i = 0; __i < __numSux; __i++)
                {
                    __seq = this.___edgeInstructionSeqences.get(__i);
                    removeFirst(__seq);
                }
            }
        }

        ///
        // Gets the first element from a LIR instruction sequence.
        ///
        private static LIRInstruction first(List<LIRInstruction> __seq)
        {
            return __seq.get(0);
        }

        ///
        // Gets the last element from a LIR instruction sequence.
        ///
        private static LIRInstruction last(List<LIRInstruction> __seq)
        {
            return __seq.get(__seq.size() - 1);
        }

        ///
        // Removes the first element from a LIR instruction sequence.
        ///
        private static void removeFirst(List<LIRInstruction> __seq)
        {
            __seq.remove(0);
        }

        ///
        // Removes the last element from a LIR instruction sequence.
        ///
        private static void removeLast(List<LIRInstruction> __seq)
        {
            __seq.remove(__seq.size() - 1);
        }
    }
}
