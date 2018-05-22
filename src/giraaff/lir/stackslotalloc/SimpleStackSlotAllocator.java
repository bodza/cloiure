package giraaff.lir.stackslotalloc;

import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.TargetDescription;

import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.debug.GraalError;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRValueUtil;
import giraaff.lir.ValueProcedure;
import giraaff.lir.VirtualStackSlot;
import giraaff.lir.framemap.FrameMapBuilderTool;
import giraaff.lir.framemap.SimpleVirtualStackSlot;
import giraaff.lir.framemap.VirtualStackSlotRange;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.phases.AllocationPhase;

public class SimpleStackSlotAllocator extends AllocationPhase
{
    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, AllocationContext context)
    {
        allocateStackSlots((FrameMapBuilderTool) lirGenRes.getFrameMapBuilder(), lirGenRes);
        lirGenRes.buildFrameMap();
    }

    public void allocateStackSlots(FrameMapBuilderTool builder, LIRGenerationResult res)
    {
        StackSlot[] mapping = new StackSlot[builder.getNumberOfStackSlots()];
        for (VirtualStackSlot virtualSlot : builder.getStackSlots())
        {
            final StackSlot slot;
            if (virtualSlot instanceof SimpleVirtualStackSlot)
            {
                slot = mapSimpleVirtualStackSlot(builder, (SimpleVirtualStackSlot) virtualSlot);
            }
            else if (virtualSlot instanceof VirtualStackSlotRange)
            {
                VirtualStackSlotRange slotRange = (VirtualStackSlotRange) virtualSlot;
                slot = mapVirtualStackSlotRange(builder, slotRange);
            }
            else
            {
                throw GraalError.shouldNotReachHere("Unknown VirtualStackSlot: " + virtualSlot);
            }
            mapping[virtualSlot.getId()] = slot;
        }
        updateLIR(res, mapping);
    }

    protected void updateLIR(LIRGenerationResult res, StackSlot[] mapping)
    {
        ValueProcedure updateProc = (value, mode, flags) ->
        {
            if (LIRValueUtil.isVirtualStackSlot(value))
            {
                StackSlot stackSlot = mapping[LIRValueUtil.asVirtualStackSlot(value).getId()];
                return stackSlot;
            }
            return value;
        };
        for (AbstractBlockBase<?> block : res.getLIR().getControlFlowGraph().getBlocks())
        {
            for (LIRInstruction inst : res.getLIR().getLIRforBlock(block))
            {
                inst.forEachAlive(updateProc);
                inst.forEachInput(updateProc);
                inst.forEachOutput(updateProc);
                inst.forEachTemp(updateProc);
                inst.forEachState(updateProc);
            }
        }
    }

    protected StackSlot mapSimpleVirtualStackSlot(FrameMapBuilderTool builder, SimpleVirtualStackSlot virtualStackSlot)
    {
        return builder.getFrameMap().allocateSpillSlot(virtualStackSlot.getValueKind());
    }

    protected StackSlot mapVirtualStackSlotRange(FrameMapBuilderTool builder, VirtualStackSlotRange virtualStackSlot)
    {
        return builder.getFrameMap().allocateStackSlots(virtualStackSlot.getSlots(), virtualStackSlot.getObjects());
    }
}
