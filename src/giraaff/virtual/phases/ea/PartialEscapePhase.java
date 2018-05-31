package giraaff.virtual.phases.ea;

import org.graalvm.collections.EconomicSet;

import giraaff.core.common.GraalOptions;
import giraaff.graph.Node;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.StructuredGraph.ScheduleResult;
import giraaff.nodes.cfg.ControlFlowGraph;
import giraaff.nodes.virtual.VirtualObjectNode;
import giraaff.phases.BasePhase;
import giraaff.phases.common.CanonicalizerPhase;
import giraaff.phases.tiers.PhaseContext;

// @class PartialEscapePhase
public final class PartialEscapePhase extends EffectsPhase<PhaseContext>
{
    private final boolean readElimination;
    private final BasePhase<PhaseContext> cleanupPhase;

    // @cons
    public PartialEscapePhase(boolean iterative, CanonicalizerPhase canonicalizer)
    {
        this(iterative, GraalOptions.optEarlyReadElimination, canonicalizer, null);
    }

    // @cons
    public PartialEscapePhase(boolean iterative, CanonicalizerPhase canonicalizer, BasePhase<PhaseContext> cleanupPhase)
    {
        this(iterative, GraalOptions.optEarlyReadElimination, canonicalizer, cleanupPhase);
    }

    // @cons
    public PartialEscapePhase(boolean iterative, boolean readElimination, CanonicalizerPhase canonicalizer, BasePhase<PhaseContext> cleanupPhase)
    {
        super(iterative ? GraalOptions.escapeAnalysisIterations : 1, canonicalizer);
        this.readElimination = readElimination;
        this.cleanupPhase = cleanupPhase;
    }

    @Override
    protected void postIteration(StructuredGraph graph, PhaseContext context, EconomicSet<Node> changedNodes)
    {
        super.postIteration(graph, context, changedNodes);
        if (cleanupPhase != null)
        {
            cleanupPhase.apply(graph, context);
        }
    }

    @Override
    protected void run(StructuredGraph graph, PhaseContext context)
    {
        if (readElimination || graph.hasVirtualizableAllocation())
        {
            runAnalysis(graph, context);
        }
    }

    @Override
    protected Closure<?> createEffectsClosure(PhaseContext context, ScheduleResult schedule, ControlFlowGraph cfg)
    {
        for (VirtualObjectNode virtual : cfg.graph.getNodes(VirtualObjectNode.TYPE))
        {
            virtual.resetObjectId();
        }
        if (readElimination)
        {
            return new PEReadEliminationClosure(schedule, context.getMetaAccess(), context.getConstantReflection(), context.getConstantFieldProvider(), context.getLowerer());
        }
        else
        {
            return new PartialEscapeClosure.Final(schedule, context.getMetaAccess(), context.getConstantReflection(), context.getConstantFieldProvider(), context.getLowerer());
        }
    }
}
