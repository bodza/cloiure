package giraaff.virtual.phases.ea;

import org.graalvm.collections.EconomicSet;

import giraaff.core.common.GraalOptions;
import giraaff.graph.Node;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.StructuredGraph.ScheduleResult;
import giraaff.nodes.cfg.ControlFlowGraph;
import giraaff.nodes.virtual.VirtualObjectNode;
import giraaff.options.OptionKey;
import giraaff.options.OptionValues;
import giraaff.phases.BasePhase;
import giraaff.phases.common.CanonicalizerPhase;
import giraaff.phases.tiers.PhaseContext;

public class PartialEscapePhase extends EffectsPhase<PhaseContext>
{
    static class Options
    {
        public static final OptionKey<Boolean> OptEarlyReadElimination = new OptionKey<>(true);
    }

    private final boolean readElimination;
    private final BasePhase<PhaseContext> cleanupPhase;

    public PartialEscapePhase(boolean iterative, CanonicalizerPhase canonicalizer, OptionValues options)
    {
        this(iterative, Options.OptEarlyReadElimination.getValue(options), canonicalizer, null, options);
    }

    public PartialEscapePhase(boolean iterative, CanonicalizerPhase canonicalizer, BasePhase<PhaseContext> cleanupPhase, OptionValues options)
    {
        this(iterative, Options.OptEarlyReadElimination.getValue(options), canonicalizer, cleanupPhase, options);
    }

    public PartialEscapePhase(boolean iterative, boolean readElimination, CanonicalizerPhase canonicalizer, BasePhase<PhaseContext> cleanupPhase, OptionValues options)
    {
        super(iterative ? GraalOptions.EscapeAnalysisIterations.getValue(options) : 1, canonicalizer);
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
        if (VirtualUtil.matches(graph, GraalOptions.EscapeAnalyzeOnly.getValue(graph.getOptions())))
        {
            if (readElimination || graph.hasVirtualizableAllocation())
            {
                runAnalysis(graph, context);
            }
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
