package graalvm.compiler.lir.alloc.trace;

import graalvm.compiler.core.common.alloc.RegisterAllocationConfig;
import graalvm.compiler.core.common.alloc.Trace;
import graalvm.compiler.core.common.alloc.TraceBuilderResult;
import graalvm.compiler.core.common.cfg.AbstractBlockBase;
import graalvm.compiler.debug.CounterKey;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.debug.Indent;
import graalvm.compiler.lir.LIR;
import graalvm.compiler.lir.alloc.RegisterAllocationPhase;
import graalvm.compiler.lir.alloc.trace.TraceAllocationPhase.TraceAllocationContext;
import graalvm.compiler.lir.gen.LIRGenerationResult;
import graalvm.compiler.lir.gen.LIRGeneratorTool.MoveFactory;
import graalvm.compiler.lir.ssa.SSAUtil;
import graalvm.compiler.options.Option;
import graalvm.compiler.options.OptionKey;
import graalvm.compiler.options.OptionType;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.AllocatableValue;

/**
 * Implements the Trace Register Allocation approach as described in
 * <a href="http://dx.doi.org/10.1145/2972206.2972211">"Trace-based Register Allocation in a JIT
 * Compiler"</a> by Josef Eisl et al.
 */
public final class TraceRegisterAllocationPhase extends RegisterAllocationPhase
{
    public static class Options
    {
        @Option(help = "Use inter-trace register hints.", type = OptionType.Debug)
        public static final OptionKey<Boolean> TraceRAuseInterTraceHints = new OptionKey<>(true);
        @Option(help = "Share information about spilled values to other traces.", type = OptionType.Debug)
        public static final OptionKey<Boolean> TraceRAshareSpillInformation = new OptionKey<>(true);
        @Option(help = "Reuse spill slots for global move resolution cycle breaking.", type = OptionType.Debug)
        public static final OptionKey<Boolean> TraceRAreuseStackSlotsForMoveResolutionCycleBreaking = new OptionKey<>(true);
        @Option(help = "Cache stack slots globally (i.e. a variable always gets the same slot in every trace).", type = OptionType.Debug)
        public static final OptionKey<Boolean> TraceRACacheStackSlots = new OptionKey<>(true);
    }

    private static final CounterKey tracesCounter = DebugContext.counter("TraceRA[traces]");

    public static final CounterKey globalStackSlots = DebugContext.counter("TraceRA[GlobalStackSlots]");
    public static final CounterKey allocatedStackSlots = DebugContext.counter("TraceRA[AllocatedStackSlots]");

    private final TraceBuilderPhase traceBuilder;
    private final GlobalLivenessAnalysisPhase livenessAnalysis;

    public TraceRegisterAllocationPhase()
    {
        this.traceBuilder = new TraceBuilderPhase();
        this.livenessAnalysis = new GlobalLivenessAnalysisPhase();
    }

    @Override
    @SuppressWarnings("try")
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, AllocationContext context)
    {
        traceBuilder.apply(target, lirGenRes, context);
        livenessAnalysis.apply(target, lirGenRes, context);

        MoveFactory spillMoveFactory = context.spillMoveFactory;
        RegisterAllocationConfig registerAllocationConfig = context.registerAllocationConfig;
        LIR lir = lirGenRes.getLIR();
        DebugContext debug = lir.getDebug();
        TraceBuilderResult resultTraces = context.contextLookup(TraceBuilderResult.class);
        GlobalLivenessInfo livenessInfo = context.contextLookup(GlobalLivenessInfo.class);
        assert livenessInfo != null;
        TraceAllocationContext traceContext = new TraceAllocationContext(spillMoveFactory, registerAllocationConfig, resultTraces, livenessInfo);
        AllocatableValue[] cachedStackSlots = Options.TraceRACacheStackSlots.getValue(lir.getOptions()) ? new AllocatableValue[lir.numVariables()] : null;

        boolean neverSpillConstant = getNeverSpillConstants();
        assert !neverSpillConstant : "currently this is not supported";

        final TraceRegisterAllocationPolicy plan = DefaultTraceRegisterAllocationPolicy.allocationPolicy(target, lirGenRes, spillMoveFactory, registerAllocationConfig, cachedStackSlots, resultTraces, neverSpillConstant, livenessInfo, lir.getOptions());

        try (DebugContext.Scope s0 = debug.scope("AllocateTraces", resultTraces, livenessInfo))
        {
            for (Trace trace : resultTraces.getTraces())
            {
                tracesCounter.increment(debug);
                TraceAllocationPhase<TraceAllocationContext> allocator = plan.selectStrategy(trace);
                try (Indent i = debug.logAndIndent("Allocating Trace%d: %s (%s)", trace.getId(), trace, allocator); DebugContext.Scope s = debug.scope("AllocateTrace", trace))
                {
                    allocator.apply(target, lirGenRes, trace, traceContext);
                }
            }
        }
        catch (Throwable e)
        {
            throw debug.handle(e);
        }

        TraceGlobalMoveResolutionPhase.resolve(target, lirGenRes, traceContext);
        deconstructSSAForm(lir);
    }

    /**
     * Remove Phi In/Out.
     */
    private static void deconstructSSAForm(LIR lir)
    {
        for (AbstractBlockBase<?> block : lir.getControlFlowGraph().getBlocks())
        {
            if (SSAUtil.isMerge(block))
            {
                SSAUtil.phiIn(lir, block).clearIncomingValues();
                for (AbstractBlockBase<?> pred : block.getPredecessors())
                {
                    SSAUtil.phiOut(lir, pred).clearOutgoingValues();
                }
            }
        }
    }
}
