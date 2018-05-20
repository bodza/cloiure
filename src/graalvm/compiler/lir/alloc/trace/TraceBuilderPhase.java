package graalvm.compiler.lir.alloc.trace;

import static graalvm.compiler.lir.alloc.trace.TraceUtil.isTrivialTrace;

import graalvm.compiler.core.common.alloc.BiDirectionalTraceBuilder;
import graalvm.compiler.core.common.alloc.SingleBlockTraceBuilder;
import graalvm.compiler.core.common.alloc.Trace;
import graalvm.compiler.core.common.alloc.TraceBuilderResult;
import graalvm.compiler.core.common.alloc.TraceBuilderResult.TrivialTracePredicate;
import graalvm.compiler.core.common.alloc.UniDirectionalTraceBuilder;
import graalvm.compiler.core.common.cfg.AbstractBlockBase;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.lir.LIR;
import graalvm.compiler.lir.gen.LIRGenerationResult;
import graalvm.compiler.lir.phases.AllocationPhase;
import graalvm.compiler.options.EnumOptionKey;
import graalvm.compiler.options.Option;
import graalvm.compiler.options.OptionKey;
import graalvm.compiler.options.OptionType;
import graalvm.compiler.options.OptionValues;

import jdk.vm.ci.code.TargetDescription;

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
        @Option(help = "Trace building algorithm.", type = OptionType.Debug)
        public static final EnumOptionKey<TraceBuilder> TraceBuilding = new EnumOptionKey<>(TraceBuilder.UniDirectional);
        @Option(help = "Schedule trivial traces as early as possible.", type = OptionType.Debug)
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
