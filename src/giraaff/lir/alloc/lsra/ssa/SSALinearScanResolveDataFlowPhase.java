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

// @class SSALinearScanResolveDataFlowPhase
final class SSALinearScanResolveDataFlowPhase extends LinearScanResolveDataFlowPhase
{
    // @cons
    SSALinearScanResolveDataFlowPhase(LinearScan __allocator)
    {
        super(__allocator);
    }

    @Override
    protected void resolveCollectMappings(AbstractBlockBase<?> __fromBlock, AbstractBlockBase<?> __toBlock, AbstractBlockBase<?> __midBlock, MoveResolver __moveResolver)
    {
        super.resolveCollectMappings(__fromBlock, __toBlock, __midBlock, __moveResolver);

        if (__toBlock.getPredecessorCount() > 1)
        {
            int __toBlockFirstInstructionId = allocator.getFirstLirInstructionId(__toBlock);
            int __fromBlockLastInstructionId = allocator.getLastLirInstructionId(__fromBlock) + 1;

            AbstractBlockBase<?> __phiOutBlock = __midBlock != null ? __midBlock : __fromBlock;
            ArrayList<LIRInstruction> __instructions = allocator.getLIR().getLIRforBlock(__phiOutBlock);
            int __phiOutIdx = SSAUtil.phiOutIndex(allocator.getLIR(), __phiOutBlock);
            int __phiOutId = __midBlock != null ? __fromBlockLastInstructionId : __instructions.get(__phiOutIdx).id();

            // @closure
            PhiValueVisitor visitor = new PhiValueVisitor()
            {
                @Override
                public void visit(Value __phiIn, Value __phiOut)
                {
                    Interval __toInterval = allocator.splitChildAtOpId(allocator.intervalFor(__phiIn), __toBlockFirstInstructionId, LIRInstruction.OperandMode.DEF);
                    if (LIRValueUtil.isConstantValue(__phiOut))
                    {
                        __moveResolver.addMapping(LIRValueUtil.asConstant(__phiOut), __toInterval);
                    }
                    else
                    {
                        Interval __fromInterval = allocator.splitChildAtOpId(allocator.intervalFor(__phiOut), __phiOutId, LIRInstruction.OperandMode.DEF);
                        if (__fromInterval != __toInterval && !__fromInterval.location().equals(__toInterval.location()))
                        {
                            if (!(LIRValueUtil.isStackSlotValue(__toInterval.location()) && LIRValueUtil.isStackSlotValue(__fromInterval.location())))
                            {
                                __moveResolver.addMapping(__fromInterval, __toInterval);
                            }
                            else
                            {
                                __moveResolver.addMapping(__fromInterval, __toInterval);
                            }
                        }
                    }
                }
            };

            SSAUtil.forEachPhiValuePair(allocator.getLIR(), __toBlock, __phiOutBlock, visitor);
            SSAUtil.removePhiOut(allocator.getLIR(), __phiOutBlock);
        }
    }
}
