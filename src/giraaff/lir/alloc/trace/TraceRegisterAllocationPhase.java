package giraaff.lir.alloc.trace;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.AllocatableValue;

import giraaff.core.common.alloc.RegisterAllocationConfig;
import giraaff.core.common.alloc.Trace;
import giraaff.core.common.alloc.TraceBuilderResult;
import giraaff.core.common.cfg.AbstractBlockBase;
import giraaff.lir.LIR;
import giraaff.lir.alloc.RegisterAllocationPhase;
import giraaff.lir.alloc.trace.TraceAllocationPhase.TraceAllocationContext;
import giraaff.lir.gen.LIRGenerationResult;
import giraaff.lir.gen.LIRGeneratorTool.MoveFactory;
import giraaff.lir.ssa.SSAUtil;
import giraaff.options.OptionKey;

/**
 * Implements the Trace Register Allocation approach as described in
 * <a href="http://dx.doi.org/10.1145/2972206.2972211">"Trace-based Register Allocation in a JIT
 * Compiler"</a> by Josef Eisl et al.
 */
public final class TraceRegisterAllocationPhase extends RegisterAllocationPhase
{
    public static class Options
    {
        // Option "Use inter-trace register hints."
        public static final OptionKey<Boolean> TraceRAuseInterTraceHints = new OptionKey<>(true);
        // Option "Share information about spilled values to other traces."
        public static final OptionKey<Boolean> TraceRAshareSpillInformation = new OptionKey<>(true);
        // Option "Reuse spill slots for global move resolution cycle breaking."
        public static final OptionKey<Boolean> TraceRAreuseStackSlotsForMoveResolutionCycleBreaking = new OptionKey<>(true);
        // Option "Cache stack slots globally (i.e. a variable always gets the same slot in every trace)."
        public static final OptionKey<Boolean> TraceRACacheStackSlots = new OptionKey<>(true);
    }

    private final TraceBuilderPhase traceBuilder;
    private final GlobalLivenessAnalysisPhase livenessAnalysis;

    public TraceRegisterAllocationPhase()
    {
        this.traceBuilder = new TraceBuilderPhase();
        this.livenessAnalysis = new GlobalLivenessAnalysisPhase();
    }

    @Override
    protected void run(TargetDescription target, LIRGenerationResult lirGenRes, AllocationContext context)
    {
        traceBuilder.apply(target, lirGenRes, context);
        livenessAnalysis.apply(target, lirGenRes, context);

        MoveFactory spillMoveFactory = context.spillMoveFactory;
        RegisterAllocationConfig registerAllocationConfig = context.registerAllocationConfig;
        LIR lir = lirGenRes.getLIR();
        TraceBuilderResult resultTraces = context.contextLookup(TraceBuilderResult.class);
        GlobalLivenessInfo livenessInfo = context.contextLookup(GlobalLivenessInfo.class);
        TraceAllocationContext traceContext = new TraceAllocationContext(spillMoveFactory, registerAllocationConfig, resultTraces, livenessInfo);
        AllocatableValue[] cachedStackSlots = Options.TraceRACacheStackSlots.getValue(lir.getOptions()) ? new AllocatableValue[lir.numVariables()] : null;

        boolean neverSpillConstant = getNeverSpillConstants();

        final TraceRegisterAllocationPolicy plan = DefaultTraceRegisterAllocationPolicy.allocationPolicy(target, lirGenRes, spillMoveFactory, registerAllocationConfig, cachedStackSlots, resultTraces, neverSpillConstant, livenessInfo, lir.getOptions());

        for (Trace trace : resultTraces.getTraces())
        {
            TraceAllocationPhase<TraceAllocationContext> allocator = plan.selectStrategy(trace);
            allocator.apply(target, lirGenRes, trace, traceContext);
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
