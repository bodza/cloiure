package giraaff.lir.alloc.lsra;

import java.util.ArrayList;
import java.util.BitSet;

import jdk.vm.ci.code.TargetDescription;

import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.lir.LIRInstruction;
import giraaff.lir.StandardOp;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.phases.AllocationPhase.AllocationContext;

///
// Phase 6: resolve data flow
//
// Insert moves at edges between blocks if intervals have been split.
///
// @class LinearScanResolveDataFlowPhase
public class LinearScanResolveDataFlowPhase extends LinearScanAllocationPhase
{
    // @field
    protected final LinearScan ___allocator;

    // @cons
    protected LinearScanResolveDataFlowPhase(LinearScan __allocator)
    {
        super();
        this.___allocator = __allocator;
    }

    @Override
    protected void run(TargetDescription __target, LIRGenerationResult __lirGenRes, AllocationContext __context)
    {
        resolveDataFlow();
    }

    protected void resolveCollectMappings(AbstractBlockBase<?> __fromBlock, AbstractBlockBase<?> __toBlock, AbstractBlockBase<?> __midBlock, MoveResolver __moveResolver)
    {
        int __toBlockFirstInstructionId = this.___allocator.getFirstLirInstructionId(__toBlock);
        int __fromBlockLastInstructionId = this.___allocator.getLastLirInstructionId(__fromBlock) + 1;
        int __numOperands = this.___allocator.operandSize();
        BitSet __liveAtEdge = this.___allocator.getBlockData(__toBlock).___liveIn;

        // visit all variables for which the liveAtEdge bit is set
        for (int __operandNum = __liveAtEdge.nextSetBit(0); __operandNum >= 0; __operandNum = __liveAtEdge.nextSetBit(__operandNum + 1))
        {
            Interval __fromInterval = this.___allocator.splitChildAtOpId(this.___allocator.intervalFor(__operandNum), __fromBlockLastInstructionId, LIRInstruction.OperandMode.DEF);
            Interval __toInterval = this.___allocator.splitChildAtOpId(this.___allocator.intervalFor(__operandNum), __toBlockFirstInstructionId, LIRInstruction.OperandMode.DEF);

            if (__fromInterval != __toInterval && !__fromInterval.location().equals(__toInterval.location()))
            {
                // need to insert move instruction
                __moveResolver.addMapping(__fromInterval, __toInterval);
            }
        }
    }

    void resolveFindInsertPos(AbstractBlockBase<?> __fromBlock, AbstractBlockBase<?> __toBlock, MoveResolver __moveResolver)
    {
        if (__fromBlock.getSuccessorCount() <= 1)
        {
            ArrayList<LIRInstruction> __instructions = this.___allocator.getLIR().getLIRforBlock(__fromBlock);
            LIRInstruction __instr = __instructions.get(__instructions.size() - 1);
            if (__instr instanceof StandardOp.JumpOp)
            {
                // insert moves before branch
                __moveResolver.setInsertPosition(__instructions, __instructions.size() - 1);
            }
            else
            {
                __moveResolver.setInsertPosition(__instructions, __instructions.size());
            }
        }
        else
        {
            __moveResolver.setInsertPosition(this.___allocator.getLIR().getLIRforBlock(__toBlock), 1);
        }
    }

    ///
    // Inserts necessary moves (spilling or reloading) at edges between blocks for intervals that
    // have been split.
    ///
    protected void resolveDataFlow()
    {
        MoveResolver __moveResolver = this.___allocator.createMoveResolver();
        BitSet __blockCompleted = new BitSet(this.___allocator.blockCount());

        optimizeEmptyBlocks(__moveResolver, __blockCompleted);

        resolveDataFlow0(__moveResolver, __blockCompleted);
    }

    protected void optimizeEmptyBlocks(MoveResolver __moveResolver, BitSet __blockCompleted)
    {
        for (AbstractBlockBase<?> __block : this.___allocator.sortedBlocks())
        {
            // check if block has only one predecessor and only one successor
            if (__block.getPredecessorCount() == 1 && __block.getSuccessorCount() == 1)
            {
                ArrayList<LIRInstruction> __instructions = this.___allocator.getLIR().getLIRforBlock(__block);

                // check if block is empty (only label and branch)
                if (__instructions.size() == 2)
                {
                    AbstractBlockBase<?> __pred = __block.getPredecessors()[0];
                    AbstractBlockBase<?> __sux = __block.getSuccessors()[0];

                    // prevent optimization of two consecutive blocks
                    if (!__blockCompleted.get(__pred.getLinearScanNumber()) && !__blockCompleted.get(__sux.getLinearScanNumber()))
                    {
                        __blockCompleted.set(__block.getLinearScanNumber());

                        // Directly resolve between pred and sux (without looking at the empty block between).
                        resolveCollectMappings(__pred, __sux, __block, __moveResolver);
                        if (__moveResolver.hasMappings())
                        {
                            __moveResolver.setInsertPosition(__instructions, 1);
                            __moveResolver.resolveAndAppendMoves();
                        }
                    }
                }
            }
        }
    }

    protected void resolveDataFlow0(MoveResolver __moveResolver, BitSet __blockCompleted)
    {
        BitSet __alreadyResolved = new BitSet(this.___allocator.blockCount());
        for (AbstractBlockBase<?> __fromBlock : this.___allocator.sortedBlocks())
        {
            if (!__blockCompleted.get(__fromBlock.getLinearScanNumber()))
            {
                __alreadyResolved.clear();
                __alreadyResolved.or(__blockCompleted);

                for (AbstractBlockBase<?> __toBlock : __fromBlock.getSuccessors())
                {
                    // Check for duplicate edges between the same blocks (can happen with switch blocks).
                    if (!__alreadyResolved.get(__toBlock.getLinearScanNumber()))
                    {
                        __alreadyResolved.set(__toBlock.getLinearScanNumber());

                        // collect all intervals that have been split between fromBlock and toBlock
                        resolveCollectMappings(__fromBlock, __toBlock, null, __moveResolver);
                        if (__moveResolver.hasMappings())
                        {
                            resolveFindInsertPos(__fromBlock, __toBlock, __moveResolver);
                            __moveResolver.resolveAndAppendMoves();
                        }
                    }
                }
            }
        }
    }
}
