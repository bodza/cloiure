package giraaff.lir.phases;

import giraaff.lir.alloc.lsra.LinearScanPhase;
import giraaff.lir.phases.AllocationPhase.AllocationContext;
import giraaff.lir.stackslotalloc.LSStackSlotAllocator;
import giraaff.lir.stackslotalloc.SimpleStackSlotAllocator;
import giraaff.options.OptionValues;

// @class AllocationStage
public final class AllocationStage extends LIRPhaseSuite<AllocationContext>
{
    // @cons
    public AllocationStage(OptionValues options)
    {
        super();
        appendPhase(new LinearScanPhase());

        // build frame map
        if (LSStackSlotAllocator.Options.LIROptLSStackSlotAllocator.getValue(options))
        {
            appendPhase(new LSStackSlotAllocator());
        }
        else
        {
            appendPhase(new SimpleStackSlotAllocator());
        }
    }
}
