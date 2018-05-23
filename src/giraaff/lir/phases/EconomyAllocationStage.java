package giraaff.lir.phases;

import giraaff.core.common.GraalOptions;
import giraaff.lir.alloc.lsra.LinearScanPhase;
import giraaff.lir.dfa.LocationMarkerPhase;
import giraaff.lir.phases.AllocationPhase.AllocationContext;
import giraaff.lir.stackslotalloc.SimpleStackSlotAllocator;
import giraaff.options.OptionValues;

public class EconomyAllocationStage extends LIRPhaseSuite<AllocationContext>
{
    public EconomyAllocationStage(OptionValues options)
    {
        appendPhase(new LinearScanPhase());

        // build frame map
        appendPhase(new SimpleStackSlotAllocator());

        // currently we mark locations only if we do register allocation
        appendPhase(new LocationMarkerPhase());
    }
}
