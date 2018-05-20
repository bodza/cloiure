package graalvm.compiler.lir.alloc.trace.lsra;

import static graalvm.compiler.lir.LIRValueUtil.asConstant;
import static graalvm.compiler.lir.LIRValueUtil.asVariable;
import static graalvm.compiler.lir.LIRValueUtil.isConstantValue;
import static graalvm.compiler.lir.LIRValueUtil.isVirtualStackSlot;

import java.util.ArrayList;

import graalvm.compiler.core.common.alloc.RegisterAllocationConfig;
import graalvm.compiler.core.common.alloc.Trace;
import graalvm.compiler.core.common.alloc.TraceBuilderResult;
import graalvm.compiler.core.common.cfg.AbstractBlockBase;
import graalvm.compiler.lir.LIR;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.StandardOp;
import graalvm.compiler.lir.StandardOp.JumpOp;
import graalvm.compiler.lir.StandardOp.LabelOp;
import graalvm.compiler.lir.alloc.trace.GlobalLivenessInfo;
import graalvm.compiler.lir.alloc.trace.lsra.TraceLinearScanPhase.TraceLinearScan;
import graalvm.compiler.lir.gen.LIRGenerationResult;
import graalvm.compiler.lir.gen.LIRGeneratorTool.MoveFactory;
import graalvm.compiler.lir.ssa.SSAUtil;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.Value;

/**
 * Phase 6: resolve data flow
 *
 * Insert moves at edges between blocks if intervals have been split.
 */
final class TraceLinearScanResolveDataFlowPhase extends TraceLinearScanAllocationPhase
{
    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, Trace trace, MoveFactory spillMoveFactory, RegisterAllocationConfig registerAllocationConfig, TraceBuilderResult traceBuilderResult, TraceLinearScan allocator)
    {
        new Resolver(allocator, traceBuilderResult).resolveDataFlow(trace, allocator.sortedBlocks());
    }

    private static final class Resolver
    {
        private final TraceLinearScan allocator;
        private final TraceBuilderResult traceBuilderResult;

        private Resolver(TraceLinearScan allocator, TraceBuilderResult traceBuilderResult)
        {
            this.allocator = allocator;
            this.traceBuilderResult = traceBuilderResult;
        }

        private void resolveFindInsertPos(AbstractBlockBase<?> fromBlock, AbstractBlockBase<?> toBlock, TraceLocalMoveResolver moveResolver)
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
         * Inserts necessary moves (spilling or reloading) at edges between blocks for intervals
         * that have been split.
         */
        private void resolveDataFlow(Trace currentTrace, AbstractBlockBase<?>[] blocks)
        {
            if (blocks.length < 2)
            {
                // no resolution necessary
                return;
            }

            TraceLocalMoveResolver moveResolver = allocator.createMoveResolver();
            AbstractBlockBase<?> toBlock = null;
            for (int i = 0; i < blocks.length - 1; i++)
            {
                AbstractBlockBase<?> fromBlock = blocks[i];
                toBlock = blocks[i + 1];
                resolveCollectMappings(fromBlock, toBlock, moveResolver);
            }
            if (toBlock.isLoopEnd())
            {
                AbstractBlockBase<?> loopHeader = toBlock.getSuccessors()[0];
                if (containedInTrace(currentTrace, loopHeader))
                {
                    resolveCollectMappings(toBlock, loopHeader, moveResolver);
                }
            }
        }

        private void resolveCollectMappings(AbstractBlockBase<?> fromBlock, AbstractBlockBase<?> toBlock, TraceLocalMoveResolver moveResolver)
        {
            // collect all intervals that have been split between
            // fromBlock and toBlock
            int toId = allocator.getFirstLirInstructionId(toBlock);
            int fromId = allocator.getLastLirInstructionId(fromBlock);
            LIR lir = allocator.getLIR();
            if (SSAUtil.isMerge(toBlock))
            {
                JumpOp blockEnd = SSAUtil.phiOut(lir, fromBlock);
                LabelOp label = SSAUtil.phiIn(lir, toBlock);
                for (int i = 0; i < label.getPhiSize(); i++)
                {
                    addMapping(blockEnd.getOutgoingValue(i), label.getIncomingValue(i), fromId, toId, moveResolver);
                }
            }
            GlobalLivenessInfo livenessInfo = allocator.getGlobalLivenessInfo();
            int[] locTo = livenessInfo.getBlockIn(toBlock);
            for (int i = 0; i < locTo.length; i++)
            {
                TraceInterval interval = allocator.intervalFor(locTo[i]);
                addMapping(interval, interval, fromId, toId, moveResolver);
            }

            if (moveResolver.hasMappings())
            {
                resolveFindInsertPos(fromBlock, toBlock, moveResolver);
                moveResolver.resolveAndAppendMoves();
            }
        }

        private boolean containedInTrace(Trace currentTrace, AbstractBlockBase<?> block)
        {
            return currentTrace.getId() == traceBuilderResult.getTraceForBlock(block).getId();
        }

        private void addMapping(Value phiFrom, Value phiTo, int fromId, int toId, TraceLocalMoveResolver moveResolver)
        {
            if (isVirtualStackSlot(phiTo) && isVirtualStackSlot(phiFrom) && phiTo.equals(phiFrom))
            {
                // no need to handle virtual stack slots
                return;
            }
            TraceInterval toParent = allocator.intervalFor(asVariable(phiTo));
            if (isConstantValue(phiFrom))
            {
                TraceInterval toInterval = allocator.splitChildAtOpId(toParent, toId, LIRInstruction.OperandMode.DEF);
                moveResolver.addMapping(asConstant(phiFrom), toInterval);
            }
            else
            {
                addMapping(allocator.intervalFor(asVariable(phiFrom)), toParent, fromId, toId, moveResolver);
            }
        }

        private void addMapping(TraceInterval fromParent, TraceInterval toParent, int fromId, int toId, TraceLocalMoveResolver moveResolver)
        {
            TraceInterval fromInterval = allocator.splitChildAtOpId(fromParent, fromId, LIRInstruction.OperandMode.USE);
            TraceInterval toInterval = toParent.getSplitChildAtOpIdOrNull(toId, LIRInstruction.OperandMode.DEF);
            if (toInterval == null)
            {
                // not alive
                return;
            }
            if (fromInterval != toInterval)
            {
                moveResolver.addMapping(fromInterval, toInterval);
            }
        }
    }
}
