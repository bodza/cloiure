package graalvm.compiler.virtual.phases.ea;

import static graalvm.compiler.core.common.GraalOptions.EscapeAnalysisIterations;
import static graalvm.compiler.core.common.GraalOptions.EscapeAnalyzeOnly;

import org.graalvm.collections.EconomicSet;
import graalvm.compiler.graph.Node;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.StructuredGraph.ScheduleResult;
import graalvm.compiler.nodes.cfg.ControlFlowGraph;
import graalvm.compiler.nodes.virtual.VirtualObjectNode;
import graalvm.compiler.options.Option;
import graalvm.compiler.options.OptionKey;
import graalvm.compiler.options.OptionType;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.BasePhase;
import graalvm.compiler.phases.common.CanonicalizerPhase;
import graalvm.compiler.phases.tiers.PhaseContext;

public class PartialEscapePhase extends EffectsPhase<PhaseContext>
{
    static class Options
    {
        @Option(help = "", type = OptionType.Debug)
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
        super(iterative ? EscapeAnalysisIterations.getValue(options) : 1, canonicalizer);
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
        if (VirtualUtil.matches(graph, EscapeAnalyzeOnly.getValue(graph.getOptions())))
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
        assert schedule != null;
        if (readElimination)
        {
            return new PEReadEliminationClosure(schedule, context.getMetaAccess(), context.getConstantReflection(), context.getConstantFieldProvider(), context.getLowerer());
        }
        else
        {
            return new PartialEscapeClosure.Final(schedule, context.getMetaAccess(), context.getConstantReflection(), context.getConstantFieldProvider(), context.getLowerer());
        }
    }

    @Override
    public boolean checkContract()
    {
        return false;
    }
}
