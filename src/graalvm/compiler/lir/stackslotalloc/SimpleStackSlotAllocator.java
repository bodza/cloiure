package graalvm.compiler.lir.stackslotalloc;

import static graalvm.compiler.lir.LIRValueUtil.asVirtualStackSlot;
import static graalvm.compiler.lir.LIRValueUtil.isVirtualStackSlot;
import static graalvm.compiler.lir.stackslotalloc.StackSlotAllocatorUtil.allocatedFramesize;
import static graalvm.compiler.lir.stackslotalloc.StackSlotAllocatorUtil.allocatedSlots;
import static graalvm.compiler.lir.stackslotalloc.StackSlotAllocatorUtil.virtualFramesize;

import graalvm.compiler.core.common.cfg.AbstractBlockBase;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.debug.Indent;
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

public class SimpleStackSlotAllocator extends AllocationPhase {

    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, AllocationContext context) {
        allocateStackSlots((FrameMapBuilderTool) lirGenRes.getFrameMapBuilder(), lirGenRes);
        lirGenRes.buildFrameMap();
    }

    public void allocateStackSlots(FrameMapBuilderTool builder, LIRGenerationResult res) {
        DebugContext debug = res.getLIR().getDebug();
        StackSlot[] mapping = new StackSlot[builder.getNumberOfStackSlots()];
        boolean allocatedFramesizeEnabled = allocatedFramesize.isEnabled(debug);
        long currentFrameSize = allocatedFramesizeEnabled ? builder.getFrameMap().currentFrameSize() : 0;
        for (VirtualStackSlot virtualSlot : builder.getStackSlots()) {
            final StackSlot slot;
            if (virtualSlot instanceof SimpleVirtualStackSlot) {
                slot = mapSimpleVirtualStackSlot(builder, (SimpleVirtualStackSlot) virtualSlot);
                virtualFramesize.add(debug, builder.getFrameMap().spillSlotSize(virtualSlot.getValueKind()));
            } else if (virtualSlot instanceof VirtualStackSlotRange) {
                VirtualStackSlotRange slotRange = (VirtualStackSlotRange) virtualSlot;
                slot = mapVirtualStackSlotRange(builder, slotRange);
                virtualFramesize.add(debug, builder.getFrameMap().spillSlotRangeSize(slotRange.getSlots()));
            } else {
                throw GraalError.shouldNotReachHere("Unknown VirtualStackSlot: " + virtualSlot);
            }
            allocatedSlots.increment(debug);
            mapping[virtualSlot.getId()] = slot;
        }
        updateLIR(res, mapping);
        if (allocatedFramesizeEnabled) {
            allocatedFramesize.add(debug, builder.getFrameMap().currentFrameSize() - currentFrameSize);
        }
    }

    @SuppressWarnings("try")
    protected void updateLIR(LIRGenerationResult res, StackSlot[] mapping) {
        DebugContext debug = res.getLIR().getDebug();
        try (DebugContext.Scope scope = debug.scope("StackSlotMappingLIR")) {
            ValueProcedure updateProc = (value, mode, flags) -> {
                if (isVirtualStackSlot(value)) {
                    StackSlot stackSlot = mapping[asVirtualStackSlot(value).getId()];
                    debug.log("map %s -> %s", value, stackSlot);
                    return stackSlot;
                }
                return value;
            };
            for (AbstractBlockBase<?> block : res.getLIR().getControlFlowGraph().getBlocks()) {
                try (Indent indent0 = debug.logAndIndent("block: %s", block)) {
                    for (LIRInstruction inst : res.getLIR().getLIRforBlock(block)) {
                        try (Indent indent1 = debug.logAndIndent("Inst: %d: %s", inst.id(), inst)) {
                            inst.forEachAlive(updateProc);
                            inst.forEachInput(updateProc);
                            inst.forEachOutput(updateProc);
                            inst.forEachTemp(updateProc);
                            inst.forEachState(updateProc);
                        }
                    }
                }
            }
        }
    }

    protected StackSlot mapSimpleVirtualStackSlot(FrameMapBuilderTool builder, SimpleVirtualStackSlot virtualStackSlot) {
        return builder.getFrameMap().allocateSpillSlot(virtualStackSlot.getValueKind());
    }

    protected StackSlot mapVirtualStackSlotRange(FrameMapBuilderTool builder, VirtualStackSlotRange virtualStackSlot) {
        return builder.getFrameMap().allocateStackSlots(virtualStackSlot.getSlots(), virtualStackSlot.getObjects());
    }
}
