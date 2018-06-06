package giraaff.lir.phases;

import giraaff.core.common.GraalOptions;
import giraaff.lir.alloc.lsra.LinearScanPhase;
import giraaff.lir.phases.AllocationPhase;
import giraaff.lir.stackslotalloc.LSStackSlotAllocator;
import giraaff.lir.stackslotalloc.SimpleStackSlotAllocator;

// @class AllocationStage
public final class AllocationStage extends LIRPhaseSuite<AllocationPhase.AllocationContext>
{
    // @cons AllocationStage
    public AllocationStage()
    {
        super();
        appendPhase(new LinearScanPhase());

        // build frame map
        if (GraalOptions.lirOptLSStackSlotAllocator)
        {
            appendPhase(new LSStackSlotAllocator());
        }
        else
        {
            appendPhase(new SimpleStackSlotAllocator());
        }
    }
}
