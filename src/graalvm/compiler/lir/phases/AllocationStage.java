package graalvm.compiler.lir.phases;

import graalvm.compiler.core.common.GraalOptions;
import graalvm.compiler.lir.alloc.lsra.LinearScanPhase;
import graalvm.compiler.lir.alloc.trace.TraceRegisterAllocationPhase;
import graalvm.compiler.lir.dfa.LocationMarkerPhase;
import graalvm.compiler.lir.dfa.MarkBasePointersPhase;
import graalvm.compiler.lir.phases.AllocationPhase.AllocationContext;
import graalvm.compiler.lir.stackslotalloc.LSStackSlotAllocator;
import graalvm.compiler.lir.stackslotalloc.SimpleStackSlotAllocator;
import graalvm.compiler.options.OptionValues;

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
