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
    // @field
    private final boolean readElimination;
    // @field
    private final BasePhase<PhaseContext> cleanupPhase;

    // @cons
    public PartialEscapePhase(boolean __iterative, CanonicalizerPhase __canonicalizer)
    {
        this(__iterative, GraalOptions.optEarlyReadElimination, __canonicalizer, null);
    }

    // @cons
    public PartialEscapePhase(boolean __iterative, CanonicalizerPhase __canonicalizer, BasePhase<PhaseContext> __cleanupPhase)
    {
        this(__iterative, GraalOptions.optEarlyReadElimination, __canonicalizer, __cleanupPhase);
    }

    // @cons
    public PartialEscapePhase(boolean __iterative, boolean __readElimination, CanonicalizerPhase __canonicalizer, BasePhase<PhaseContext> __cleanupPhase)
    {
        super(__iterative ? GraalOptions.escapeAnalysisIterations : 1, __canonicalizer);
        this.readElimination = __readElimination;
        this.cleanupPhase = __cleanupPhase;
    }

    @Override
    protected void postIteration(StructuredGraph __graph, PhaseContext __context, EconomicSet<Node> __changedNodes)
    {
        super.postIteration(__graph, __context, __changedNodes);
        if (cleanupPhase != null)
        {
            cleanupPhase.apply(__graph, __context);
        }
    }

    @Override
    protected void run(StructuredGraph __graph, PhaseContext __context)
    {
        if (readElimination || __graph.hasVirtualizableAllocation())
        {
            runAnalysis(__graph, __context);
        }
    }

    @Override
    protected Closure<?> createEffectsClosure(PhaseContext __context, ScheduleResult __schedule, ControlFlowGraph __cfg)
    {
        for (VirtualObjectNode __virtual : __cfg.graph.getNodes(VirtualObjectNode.TYPE))
        {
            __virtual.resetObjectId();
        }
        if (readElimination)
        {
            return new PEReadEliminationClosure(__schedule, __context.getMetaAccess(), __context.getConstantReflection(), __context.getConstantFieldProvider(), __context.getLowerer());
        }
        else
        {
            return new PartialEscapeClosure.Final(__schedule, __context.getMetaAccess(), __context.getConstantReflection(), __context.getConstantFieldProvider(), __context.getLowerer());
        }
    }
}
