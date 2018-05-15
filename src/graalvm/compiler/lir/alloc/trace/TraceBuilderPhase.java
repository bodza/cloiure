package graalvm.compiler.lir.alloc.trace;

import static graalvm.compiler.lir.alloc.trace.TraceUtil.isTrivialTrace;

import java.util.ArrayList;

import graalvm.compiler.core.common.alloc.BiDirectionalTraceBuilder;
import graalvm.compiler.core.common.alloc.SingleBlockTraceBuilder;
import graalvm.compiler.core.common.alloc.Trace;
import graalvm.compiler.core.common.alloc.TraceBuilderResult;
import graalvm.compiler.core.common.alloc.TraceBuilderResult.TrivialTracePredicate;
import graalvm.compiler.core.common.alloc.TraceStatisticsPrinter;
import graalvm.compiler.core.common.alloc.UniDirectionalTraceBuilder;
import graalvm.compiler.core.common.cfg.AbstractBlockBase;
import graalvm.compiler.debug.DebugContext;
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

public class TraceBuilderPhase extends AllocationPhase {

    public enum TraceBuilder {
        UniDirectional,
        BiDirectional,
        SingleBlock
    }

    public static class Options {
        // @formatter:off
        @Option(help = "Trace building algorithm.", type = OptionType.Debug)
        public static final EnumOptionKey<TraceBuilder> TraceBuilding = new EnumOptionKey<>(TraceBuilder.UniDirectional);
        @Option(help = "Schedule trivial traces as early as possible.", type = OptionType.Debug)
        public static final OptionKey<Boolean> TraceRAScheduleTrivialTracesEarly = new OptionKey<>(true);
        // @formatter:on
    }

    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, AllocationContext context) {
        AbstractBlockBase<?>[] linearScanOrder = lirGenRes.getLIR().linearScanOrder();
        AbstractBlockBase<?> startBlock = linearScanOrder[0];
        LIR lir = lirGenRes.getLIR();
        assert startBlock.equals(lir.getControlFlowGraph().getStartBlock());

        final TraceBuilderResult traceBuilderResult = getTraceBuilderResult(lir, startBlock, linearScanOrder);

        DebugContext debug = lir.getDebug();
        if (debug.isLogEnabled(DebugContext.BASIC_LEVEL)) {
            ArrayList<Trace> traces = traceBuilderResult.getTraces();
            for (int i = 0; i < traces.size(); i++) {
                Trace trace = traces.get(i);
                debug.log(DebugContext.BASIC_LEVEL, "Trace %5d: %s%s", i, trace, isTrivialTrace(lirGenRes.getLIR(), trace) ? " (trivial)" : "");
            }
        }
        TraceStatisticsPrinter.printTraceStatistics(debug, traceBuilderResult, lirGenRes.getCompilationUnitName());
        debug.dump(DebugContext.VERBOSE_LEVEL, traceBuilderResult, "TraceBuilderResult");
        context.contextAdd(traceBuilderResult);
    }

    private static TraceBuilderResult getTraceBuilderResult(LIR lir, AbstractBlockBase<?> startBlock, AbstractBlockBase<?>[] linearScanOrder) {
        TraceBuilderResult.TrivialTracePredicate pred = getTrivialTracePredicate(lir);

        OptionValues options = lir.getOptions();
        TraceBuilder selectedTraceBuilder = Options.TraceBuilding.getValue(options);
        DebugContext debug = lir.getDebug();
        debug.log(DebugContext.BASIC_LEVEL, "Building Traces using %s", selectedTraceBuilder);
        switch (Options.TraceBuilding.getValue(options)) {
            case SingleBlock:
                return SingleBlockTraceBuilder.computeTraces(debug, startBlock, linearScanOrder, pred);
            case BiDirectional:
                return BiDirectionalTraceBuilder.computeTraces(debug, startBlock, linearScanOrder, pred);
            case UniDirectional:
                return UniDirectionalTraceBuilder.computeTraces(debug, startBlock, linearScanOrder, pred);
        }
        throw GraalError.shouldNotReachHere("Unknown trace building algorithm: " + Options.TraceBuilding.getValue(options));
    }

    public static TraceBuilderResult.TrivialTracePredicate getTrivialTracePredicate(LIR lir) {
        if (!Options.TraceRAScheduleTrivialTracesEarly.getValue(lir.getOptions())) {
            return null;
        }
        return new TrivialTracePredicate() {
            @Override
            public boolean isTrivialTrace(Trace trace) {
                return TraceUtil.isTrivialTrace(lir, trace);
            }
        };
    }
}
