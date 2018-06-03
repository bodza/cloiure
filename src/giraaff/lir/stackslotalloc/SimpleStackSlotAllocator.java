package giraaff.lir.stackslotalloc;

import jdk.vm.ci.code.StackSlot;
import jdk.vm.ci.code.TargetDescription;

import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.lir.LIRInstruction;
import giraaff.lir.LIRValueUtil;
import giraaff.lir.ValueProcedure;
import giraaff.lir.VirtualStackSlot;
import giraaff.lir.framemap.FrameMapBuilderTool;
import giraaff.lir.framemap.SimpleVirtualStackSlot;
import giraaff.lir.framemap.VirtualStackSlotRange;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.phases.AllocationPhase;
import giraaff.util.GraalError;

// @class SimpleStackSlotAllocator
public final class SimpleStackSlotAllocator extends AllocationPhase
{
    @Override
    protected void run(TargetDescription __target, LIRGenerationResult __lirGenRes, AllocationContext __context)
    {
        allocateStackSlots((FrameMapBuilderTool) __lirGenRes.getFrameMapBuilder(), __lirGenRes);
        __lirGenRes.buildFrameMap();
    }

    public void allocateStackSlots(FrameMapBuilderTool __builder, LIRGenerationResult __res)
    {
        StackSlot[] __mapping = new StackSlot[__builder.getNumberOfStackSlots()];
        for (VirtualStackSlot __virtualSlot : __builder.getStackSlots())
        {
            final StackSlot __slot;
            if (__virtualSlot instanceof SimpleVirtualStackSlot)
            {
                __slot = mapSimpleVirtualStackSlot(__builder, (SimpleVirtualStackSlot) __virtualSlot);
            }
            else if (__virtualSlot instanceof VirtualStackSlotRange)
            {
                VirtualStackSlotRange __slotRange = (VirtualStackSlotRange) __virtualSlot;
                __slot = mapVirtualStackSlotRange(__builder, __slotRange);
            }
            else
            {
                throw GraalError.shouldNotReachHere("Unknown VirtualStackSlot: " + __virtualSlot);
            }
            __mapping[__virtualSlot.getId()] = __slot;
        }
        updateLIR(__res, __mapping);
    }

    protected void updateLIR(LIRGenerationResult __res, StackSlot[] __mapping)
    {
        ValueProcedure __updateProc = (__value, __mode, __flags) ->
        {
            if (LIRValueUtil.isVirtualStackSlot(__value))
            {
                return __mapping[LIRValueUtil.asVirtualStackSlot(__value).getId()];
            }
            return __value;
        };
        for (AbstractBlockBase<?> __block : __res.getLIR().getControlFlowGraph().getBlocks())
        {
            for (LIRInstruction __inst : __res.getLIR().getLIRforBlock(__block))
            {
                __inst.forEachAlive(__updateProc);
                __inst.forEachInput(__updateProc);
                __inst.forEachOutput(__updateProc);
                __inst.forEachTemp(__updateProc);
            }
        }
    }

    protected StackSlot mapSimpleVirtualStackSlot(FrameMapBuilderTool __builder, SimpleVirtualStackSlot __virtualStackSlot)
    {
        return __builder.getFrameMap().allocateSpillSlot(__virtualStackSlot.getValueKind());
    }

    protected StackSlot mapVirtualStackSlotRange(FrameMapBuilderTool __builder, VirtualStackSlotRange __virtualStackSlot)
    {
        return __builder.getFrameMap().allocateStackSlots(__virtualStackSlot.getSlots(), __virtualStackSlot.getObjects());
    }
}
