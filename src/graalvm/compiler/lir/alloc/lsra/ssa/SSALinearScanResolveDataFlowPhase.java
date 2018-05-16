package graalvm.compiler.lir.alloc.lsra.ssa;

import static jdk.vm.ci.code.ValueUtil.isRegister;
import static graalvm.compiler.lir.LIRValueUtil.asConstant;
import static graalvm.compiler.lir.LIRValueUtil.isConstantValue;
import static graalvm.compiler.lir.LIRValueUtil.isStackSlotValue;

import java.util.ArrayList;

import graalvm.compiler.core.common.cfg.AbstractBlockBase;
import graalvm.compiler.debug.CounterKey;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.alloc.lsra.Interval;
import graalvm.compiler.lir.alloc.lsra.LinearScan;
import graalvm.compiler.lir.alloc.lsra.LinearScanResolveDataFlowPhase;
import graalvm.compiler.lir.alloc.lsra.MoveResolver;
import graalvm.compiler.lir.ssa.SSAUtil;
import graalvm.compiler.lir.ssa.SSAUtil.PhiValueVisitor;

import jdk.vm.ci.meta.Value;

class SSALinearScanResolveDataFlowPhase extends LinearScanResolveDataFlowPhase
{
    private static final CounterKey numPhiResolutionMoves = DebugContext.counter("SSA LSRA[numPhiResolutionMoves]");
    private static final CounterKey numStackToStackMoves = DebugContext.counter("SSA LSRA[numStackToStackMoves]");

    SSALinearScanResolveDataFlowPhase(LinearScan allocator)
    {
        super(allocator);
    }

    @Override
    protected void resolveCollectMappings(AbstractBlockBase<?> fromBlock, AbstractBlockBase<?> toBlock, AbstractBlockBase<?> midBlock, MoveResolver moveResolver)
    {
        super.resolveCollectMappings(fromBlock, toBlock, midBlock, moveResolver);

        if (toBlock.getPredecessorCount() > 1)
        {
            int toBlockFirstInstructionId = allocator.getFirstLirInstructionId(toBlock);
            int fromBlockLastInstructionId = allocator.getLastLirInstructionId(fromBlock) + 1;

            AbstractBlockBase<?> phiOutBlock = midBlock != null ? midBlock : fromBlock;
            ArrayList<LIRInstruction> instructions = allocator.getLIR().getLIRforBlock(phiOutBlock);
            int phiOutIdx = SSAUtil.phiOutIndex(allocator.getLIR(), phiOutBlock);
            int phiOutId = midBlock != null ? fromBlockLastInstructionId : instructions.get(phiOutIdx).id();
            assert phiOutId >= 0;

            PhiValueVisitor visitor = new PhiValueVisitor()
            {
                @Override
                public void visit(Value phiIn, Value phiOut)
                {
                    assert !isRegister(phiOut) : "phiOut is a register: " + phiOut;
                    assert !isRegister(phiIn) : "phiIn is a register: " + phiIn;
                    Interval toInterval = allocator.splitChildAtOpId(allocator.intervalFor(phiIn), toBlockFirstInstructionId, LIRInstruction.OperandMode.DEF);
                    DebugContext debug = allocator.getDebug();
                    if (isConstantValue(phiOut))
                    {
                        numPhiResolutionMoves.increment(debug);
                        moveResolver.addMapping(asConstant(phiOut), toInterval);
                    }
                    else
                    {
                        Interval fromInterval = allocator.splitChildAtOpId(allocator.intervalFor(phiOut), phiOutId, LIRInstruction.OperandMode.DEF);
                        if (fromInterval != toInterval && !fromInterval.location().equals(toInterval.location()))
                        {
                            numPhiResolutionMoves.increment(debug);
                            if (!(isStackSlotValue(toInterval.location()) && isStackSlotValue(fromInterval.location())))
                            {
                                moveResolver.addMapping(fromInterval, toInterval);
                            }
                            else
                            {
                                numStackToStackMoves.increment(debug);
                                moveResolver.addMapping(fromInterval, toInterval);
                            }
                        }
                    }
                }
            };

            SSAUtil.forEachPhiValuePair(allocator.getLIR(), toBlock, phiOutBlock, visitor);
            SSAUtil.removePhiOut(allocator.getLIR(), phiOutBlock);
        }
    }
}
