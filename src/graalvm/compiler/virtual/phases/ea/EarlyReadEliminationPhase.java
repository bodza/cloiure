package graalvm.compiler.virtual.phases.ea;

import graalvm.compiler.core.common.GraalOptions;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.StructuredGraph.ScheduleResult;
import graalvm.compiler.nodes.cfg.ControlFlowGraph;
import graalvm.compiler.phases.common.CanonicalizerPhase;
import graalvm.compiler.phases.tiers.PhaseContext;

public class EarlyReadEliminationPhase extends EffectsPhase<PhaseContext>
{
    private final boolean considerGuards;

    public EarlyReadEliminationPhase(CanonicalizerPhase canonicalizer)
    {
        super(1, canonicalizer, true);
        this.considerGuards = true;
    }

    public EarlyReadEliminationPhase(CanonicalizerPhase canonicalizer, boolean considerGuards)
    {
        super(1, canonicalizer, true);
        this.considerGuards = considerGuards;
    }

    @Override
    protected void run(StructuredGraph graph, PhaseContext context)
    {
        if (VirtualUtil.matches(graph, GraalOptions.EscapeAnalyzeOnly.getValue(graph.getOptions())))
        {
            runAnalysis(graph, context);
        }
    }

    @Override
    protected Closure<?> createEffectsClosure(PhaseContext context, ScheduleResult schedule, ControlFlowGraph cfg)
    {
        return new ReadEliminationClosure(cfg, considerGuards);
    }
}
