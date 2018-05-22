package giraaff.lir.alloc.trace;

import jdk.vm.ci.code.TargetDescription;

import giraaff.core.common.alloc.BiDirectionalTraceBuilder;
import giraaff.core.common.alloc.SingleBlockTraceBuilder;
import giraaff.core.common.alloc.Trace;
import giraaff.core.common.alloc.TraceBuilderResult;
import giraaff.core.common.alloc.TraceBuilderResult.TrivialTracePredicate;
import giraaff.core.common.alloc.UniDirectionalTraceBuilder;
import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.debug.GraalError;
import giraaff.lir.LIR;
import giraaff.lir.alloc.trace.TraceUtil;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.phases.AllocationPhase;
import giraaff.options.EnumOptionKey;
import giraaff.options.OptionKey;
import giraaff.options.OptionValues;

public class TraceBuilderPhase extends AllocationPhase
{
    public enum TraceBuilder
    {
        UniDirectional,
        BiDirectional,
        SingleBlock
    }

    public static class Options
    {
        // Option "Trace building algorithm."
        public static final EnumOptionKey<TraceBuilder> TraceBuilding = new EnumOptionKey<>(TraceBuilder.UniDirectional);
        // Option "Schedule trivial traces as early as possible."
        public static final OptionKey<Boolean> TraceRAScheduleTrivialTracesEarly = new OptionKey<>(true);
    }

    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, AllocationContext context)
    {
        AbstractBlockBase<?>[] linearScanOrder = lirGenRes.getLIR().linearScanOrder();
        AbstractBlockBase<?> startBlock = linearScanOrder[0];
        LIR lir = lirGenRes.getLIR();

        final TraceBuilderResult traceBuilderResult = getTraceBuilderResult(lir, startBlock, linearScanOrder);

        context.contextAdd(traceBuilderResult);
    }

    private static TraceBuilderResult getTraceBuilderResult(LIR lir, AbstractBlockBase<?> startBlock, AbstractBlockBase<?>[] linearScanOrder)
    {
        TraceBuilderResult.TrivialTracePredicate pred = getTrivialTracePredicate(lir);

        OptionValues options = lir.getOptions();
        TraceBuilder selectedTraceBuilder = Options.TraceBuilding.getValue(options);
        switch (Options.TraceBuilding.getValue(options))
        {
            case SingleBlock:
                return SingleBlockTraceBuilder.computeTraces(startBlock, linearScanOrder, pred);
            case BiDirectional:
                return BiDirectionalTraceBuilder.computeTraces(startBlock, linearScanOrder, pred);
            case UniDirectional:
                return UniDirectionalTraceBuilder.computeTraces(startBlock, linearScanOrder, pred);
        }
        throw GraalError.shouldNotReachHere("Unknown trace building algorithm: " + Options.TraceBuilding.getValue(options));
    }

    public static TraceBuilderResult.TrivialTracePredicate getTrivialTracePredicate(LIR lir)
    {
        if (!Options.TraceRAScheduleTrivialTracesEarly.getValue(lir.getOptions()))
        {
            return null;
        }
        return new TrivialTracePredicate()
        {
            @Override
            public boolean isTrivialTrace(Trace trace)
            {
                return TraceUtil.isTrivialTrace(lir, trace);
            }
        };
    }
}
