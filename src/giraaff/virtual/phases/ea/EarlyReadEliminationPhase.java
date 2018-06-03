package giraaff.virtual.phases.ea;

import giraaff.core.common.GraalOptions;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.StructuredGraph.ScheduleResult;
import giraaff.nodes.cfg.ControlFlowGraph;
import giraaff.phases.common.CanonicalizerPhase;
import giraaff.phases.tiers.PhaseContext;

// @class EarlyReadEliminationPhase
public final class EarlyReadEliminationPhase extends EffectsPhase<PhaseContext>
{
    // @field
    private final boolean ___considerGuards;

    // @cons
    public EarlyReadEliminationPhase(CanonicalizerPhase __canonicalizer)
    {
        super(1, __canonicalizer, true);
        this.___considerGuards = true;
    }

    // @cons
    public EarlyReadEliminationPhase(CanonicalizerPhase __canonicalizer, boolean __considerGuards)
    {
        super(1, __canonicalizer, true);
        this.___considerGuards = __considerGuards;
    }

    @Override
    protected void run(StructuredGraph __graph, PhaseContext __context)
    {
        runAnalysis(__graph, __context);
    }

    @Override
    protected Closure<?> createEffectsClosure(PhaseContext __context, ScheduleResult __schedule, ControlFlowGraph __cfg)
    {
        return new ReadEliminationClosure(__cfg, this.___considerGuards);
    }
}
