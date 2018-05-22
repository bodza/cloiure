package graalvm.compiler.lir.phases;

import graalvm.compiler.core.common.GraalOptions;
import graalvm.compiler.lir.alloc.lsra.LinearScanPhase;
import graalvm.compiler.lir.alloc.trace.TraceRegisterAllocationPhase;
import graalvm.compiler.lir.dfa.LocationMarkerPhase;
import graalvm.compiler.lir.phases.AllocationPhase.AllocationContext;
import graalvm.compiler.lir.stackslotalloc.SimpleStackSlotAllocator;
import graalvm.compiler.options.OptionValues;

public class EconomyAllocationStage extends LIRPhaseSuite<AllocationContext>
{
    public EconomyAllocationStage(OptionValues options)
    {
        if (GraalOptions.TraceRA.getValue(options))
        {
            appendPhase(new TraceRegisterAllocationPhase());
        }
        else
        {
            appendPhase(new LinearScanPhase());
        }

        // build frame map
        appendPhase(new SimpleStackSlotAllocator());

        // currently we mark locations only if we do register allocation
        appendPhase(new LocationMarkerPhase());
    }
}
