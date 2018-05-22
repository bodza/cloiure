package giraaff.lir.alloc.lsra.ssa;

import java.util.ArrayList;

import jdk.vm.ci.meta.Value;

import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRValueUtil;
import giraaff.lir.alloc.lsra.Interval;
import giraaff.lir.alloc.lsra.LinearScan;
import giraaff.lir.alloc.lsra.LinearScanResolveDataFlowPhase;
import giraaff.lir.alloc.lsra.MoveResolver;
import giraaff.lir.ssa.SSAUtil;
import giraaff.lir.ssa.SSAUtil.PhiValueVisitor;

class SSALinearScanResolveDataFlowPhase extends LinearScanResolveDataFlowPhase
{
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

            PhiValueVisitor visitor = new PhiValueVisitor()
            {
                @Override
                public void visit(Value phiIn, Value phiOut)
                {
                    Interval toInterval = allocator.splitChildAtOpId(allocator.intervalFor(phiIn), toBlockFirstInstructionId, LIRInstruction.OperandMode.DEF);
                    if (LIRValueUtil.isConstantValue(phiOut))
                    {
                        moveResolver.addMapping(LIRValueUtil.asConstant(phiOut), toInterval);
                    }
                    else
                    {
                        Interval fromInterval = allocator.splitChildAtOpId(allocator.intervalFor(phiOut), phiOutId, LIRInstruction.OperandMode.DEF);
                        if (fromInterval != toInterval && !fromInterval.location().equals(toInterval.location()))
                        {
                            if (!(LIRValueUtil.isStackSlotValue(toInterval.location()) && LIRValueUtil.isStackSlotValue(fromInterval.location())))
                            {
                                moveResolver.addMapping(fromInterval, toInterval);
                            }
                            else
                            {
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
