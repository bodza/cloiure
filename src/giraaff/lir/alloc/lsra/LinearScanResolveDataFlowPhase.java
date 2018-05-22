package giraaff.lir.alloc.lsra;

import java.util.ArrayList;
import java.util.BitSet;

import jdk.vm.ci.code.TargetDescription;

import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.lir.LIRInstruction;
import giraaff.lir.StandardOp;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.phases.AllocationPhase.AllocationContext;

/**
 * Phase 6: resolve data flow
 *
 * Insert moves at edges between blocks if intervals have been split.
 */
public class LinearScanResolveDataFlowPhase extends LinearScanAllocationPhase
{
    protected final LinearScan allocator;

    protected LinearScanResolveDataFlowPhase(LinearScan allocator)
    {
        this.allocator = allocator;
    }

    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, AllocationContext context)
    {
        resolveDataFlow();
    }

    protected void resolveCollectMappings(AbstractBlockBase<?> fromBlock, AbstractBlockBase<?> toBlock, AbstractBlockBase<?> midBlock, MoveResolver moveResolver)
    {
        int toBlockFirstInstructionId = allocator.getFirstLirInstructionId(toBlock);
        int fromBlockLastInstructionId = allocator.getLastLirInstructionId(fromBlock) + 1;
        int numOperands = allocator.operandSize();
        BitSet liveAtEdge = allocator.getBlockData(toBlock).liveIn;

        // visit all variables for which the liveAtEdge bit is set
        for (int operandNum = liveAtEdge.nextSetBit(0); operandNum >= 0; operandNum = liveAtEdge.nextSetBit(operandNum + 1))
        {
            Interval fromInterval = allocator.splitChildAtOpId(allocator.intervalFor(operandNum), fromBlockLastInstructionId, LIRInstruction.OperandMode.DEF);
            Interval toInterval = allocator.splitChildAtOpId(allocator.intervalFor(operandNum), toBlockFirstInstructionId, LIRInstruction.OperandMode.DEF);

            if (fromInterval != toInterval && !fromInterval.location().equals(toInterval.location()))
            {
                // need to insert move instruction
                moveResolver.addMapping(fromInterval, toInterval);
            }
        }
    }

    void resolveFindInsertPos(AbstractBlockBase<?> fromBlock, AbstractBlockBase<?> toBlock, MoveResolver moveResolver)
    {
        if (fromBlock.getSuccessorCount() <= 1)
        {
            ArrayList<LIRInstruction> instructions = allocator.getLIR().getLIRforBlock(fromBlock);
            LIRInstruction instr = instructions.get(instructions.size() - 1);
            if (instr instanceof StandardOp.JumpOp)
            {
                // insert moves before branch
                moveResolver.setInsertPosition(instructions, instructions.size() - 1);
            }
            else
            {
                moveResolver.setInsertPosition(instructions, instructions.size());
            }
        }
        else
        {
            moveResolver.setInsertPosition(allocator.getLIR().getLIRforBlock(toBlock), 1);
        }
    }

    /**
     * Inserts necessary moves (spilling or reloading) at edges between blocks for intervals that
     * have been split.
     */
    protected void resolveDataFlow()
    {
        MoveResolver moveResolver = allocator.createMoveResolver();
        BitSet blockCompleted = new BitSet(allocator.blockCount());

        optimizeEmptyBlocks(moveResolver, blockCompleted);

        resolveDataFlow0(moveResolver, blockCompleted);
    }

    protected void optimizeEmptyBlocks(MoveResolver moveResolver, BitSet blockCompleted)
    {
        for (AbstractBlockBase<?> block : allocator.sortedBlocks())
        {
            // check if block has only one predecessor and only one successor
            if (block.getPredecessorCount() == 1 && block.getSuccessorCount() == 1)
            {
                ArrayList<LIRInstruction> instructions = allocator.getLIR().getLIRforBlock(block);

                // check if block is empty (only label and branch)
                if (instructions.size() == 2)
                {
                    AbstractBlockBase<?> pred = block.getPredecessors()[0];
                    AbstractBlockBase<?> sux = block.getSuccessors()[0];

                    // prevent optimization of two consecutive blocks
                    if (!blockCompleted.get(pred.getLinearScanNumber()) && !blockCompleted.get(sux.getLinearScanNumber()))
                    {
                        blockCompleted.set(block.getLinearScanNumber());

                        /*
                         * Directly resolve between pred and sux (without looking at the empty block
                         * between).
                         */
                        resolveCollectMappings(pred, sux, block, moveResolver);
                        if (moveResolver.hasMappings())
                        {
                            moveResolver.setInsertPosition(instructions, 1);
                            moveResolver.resolveAndAppendMoves();
                        }
                    }
                }
            }
        }
    }

    protected void resolveDataFlow0(MoveResolver moveResolver, BitSet blockCompleted)
    {
        BitSet alreadyResolved = new BitSet(allocator.blockCount());
        for (AbstractBlockBase<?> fromBlock : allocator.sortedBlocks())
        {
            if (!blockCompleted.get(fromBlock.getLinearScanNumber()))
            {
                alreadyResolved.clear();
                alreadyResolved.or(blockCompleted);

                for (AbstractBlockBase<?> toBlock : fromBlock.getSuccessors())
                {
                    /*
                     * Check for duplicate edges between the same blocks (can happen with switch
                     * blocks).
                     */
                    if (!alreadyResolved.get(toBlock.getLinearScanNumber()))
                    {
                        alreadyResolved.set(toBlock.getLinearScanNumber());

                        // collect all intervals that have been split between
                        // fromBlock and toBlock
                        resolveCollectMappings(fromBlock, toBlock, null, moveResolver);
                        if (moveResolver.hasMappings())
                        {
                            resolveFindInsertPos(fromBlock, toBlock, moveResolver);
                            moveResolver.resolveAndAppendMoves();
                        }
                    }
                }
            }
        }
    }
}
