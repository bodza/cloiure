package giraaff.lir.phases;

import giraaff.core.common.GraalOptions;
import giraaff.lir.alloc.lsra.LinearScanPhase;
import giraaff.lir.alloc.trace.TraceRegisterAllocationPhase;
import giraaff.lir.dfa.LocationMarkerPhase;
import giraaff.lir.dfa.MarkBasePointersPhase;
import giraaff.lir.phases.AllocationPhase.AllocationContext;
import giraaff.lir.stackslotalloc.LSStackSlotAllocator;
import giraaff.lir.stackslotalloc.SimpleStackSlotAllocator;
import giraaff.options.OptionValues;

public class AllocationStage extends LIRPhaseSuite<AllocationContext>
{
    public AllocationStage(OptionValues options)
    {
        appendPhase(new MarkBasePointersPhase());
        if (GraalOptions.TraceRA.getValue(options))
        {
            appendPhase(new TraceRegisterAllocationPhase());
        }
        else
        {
            appendPhase(new LinearScanPhase());
        }

        // build frame map
        if (LSStackSlotAllocator.Options.LIROptLSStackSlotAllocator.getValue(options))
        {
            appendPhase(new LSStackSlotAllocator());
        }
        else
        {
            appendPhase(new SimpleStackSlotAllocator());
        }
        // currently we mark locations only if we do register allocation
        appendPhase(new LocationMarkerPhase());
    }
}
