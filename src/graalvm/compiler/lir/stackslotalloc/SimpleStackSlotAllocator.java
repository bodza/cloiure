package graalvm.compiler.lir.stackslotalloc;

import static graalvm.compiler.lir.LIRValueUtil.asVirtualStackSlot;
import static graalvm.compiler.lir.LIRValueUtil.isVirtualStackSlot;

import graalvm.compiler.core.common.cfg.AbstractBlockBase;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.lir.LIRInstruction;
import graalvm.compiler.lir.ValueProcedure;
import graalvm.compiler.lir.VirtualStackSlot;
import graalvm.compiler.lir.framemap.FrameMapBuilderTool;
import graalvm.compiler.lir.framemap.SimpleVirtualStackSlot;
import graalvm.compiler.lir.framemap.VirtualStackSlotRange;
import graalvm.compiler.lir.gen.LIRGenerationResult;
import graalvm.compiler.lir.phases.AllocationPhase;

import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.TargetDescription;

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
            if (isVirtualStackSlot(value))
            {
                StackSlot stackSlot = mapping[asVirtualStackSlot(value).getId()];
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
