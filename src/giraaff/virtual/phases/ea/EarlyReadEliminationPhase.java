package giraaff.virtual.phases.ea;

import giraaff.core.common.GraalOptions;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.StructuredGraph.ScheduleResult;
import giraaff.nodes.cfg.ControlFlowGraph;
import giraaff.phases.common.CanonicalizerPhase;
import giraaff.phases.tiers.PhaseContext;

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
