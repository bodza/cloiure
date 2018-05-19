package graalvm.compiler.virtual.phases.ea;

import static graalvm.compiler.phases.common.DeadCodeEliminationPhase.Optionality.Required;

import org.graalvm.collections.EconomicSet;
import graalvm.compiler.core.common.util.CompilationAlarm;
import graalvm.compiler.graph.Graph.NodeEventScope;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.spi.Simplifiable;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.StructuredGraph.ScheduleResult;
import graalvm.compiler.nodes.cfg.ControlFlowGraph;
import graalvm.compiler.phases.BasePhase;
import graalvm.compiler.phases.common.CanonicalizerPhase;
import graalvm.compiler.phases.common.DeadCodeEliminationPhase;
import graalvm.compiler.phases.common.util.HashSetNodeEventListener;
import graalvm.compiler.phases.graph.ReentrantBlockIterator;
import graalvm.compiler.phases.schedule.SchedulePhase;
import graalvm.compiler.phases.tiers.PhaseContext;

public abstract class EffectsPhase<PhaseContextT extends PhaseContext> extends BasePhase<PhaseContextT>
{
    public abstract static class Closure<T> extends ReentrantBlockIterator.BlockIteratorClosure<T>
    {
        public abstract boolean hasChanged();

        public abstract boolean needsApplyEffects();

        public abstract void applyEffects();
    }

    private final int maxIterations;
    protected final CanonicalizerPhase canonicalizer;
    private final boolean unscheduled;

    protected EffectsPhase(int maxIterations, CanonicalizerPhase canonicalizer)
    {
        this(maxIterations, canonicalizer, false);
    }

    protected EffectsPhase(int maxIterations, CanonicalizerPhase canonicalizer, boolean unscheduled)
    {
        this.maxIterations = maxIterations;
        this.canonicalizer = canonicalizer;
        this.unscheduled = unscheduled;
    }

    @Override
    protected void run(StructuredGraph graph, PhaseContextT context)
    {
        runAnalysis(graph, context);
    }

    public boolean runAnalysis(StructuredGraph graph, PhaseContextT context)
    {
        boolean changed = false;
        CompilationAlarm compilationAlarm = CompilationAlarm.current();
        for (int iteration = 0; iteration < maxIterations && !compilationAlarm.hasExpired(); iteration++)
        {
            ScheduleResult schedule;
            ControlFlowGraph cfg;
            if (unscheduled)
            {
                schedule = null;
                cfg = ControlFlowGraph.compute(graph, true, true, false, false);
            }
            else
            {
                new SchedulePhase(SchedulePhase.SchedulingStrategy.EARLIEST).apply(graph);
                schedule = graph.getLastSchedule();
                cfg = schedule.getCFG();
            }
            Closure<?> closure = createEffectsClosure(context, schedule, cfg);
            ReentrantBlockIterator.apply(closure, cfg.getStartBlock());

            if (closure.needsApplyEffects())
            {
                // apply the effects collected during this iteration
                HashSetNodeEventListener listener = new HashSetNodeEventListener();
                try (NodeEventScope nes = graph.trackNodeEvents(listener))
                {
                    closure.applyEffects();
                }

                new DeadCodeEliminationPhase(Required).apply(graph);

                EconomicSet<Node> changedNodes = listener.getNodes();
                for (Node node : graph.getNodes())
                {
                    if (node instanceof Simplifiable)
                    {
                        changedNodes.add(node);
                    }
                }
                postIteration(graph, context, changedNodes);
            }

            if (closure.hasChanged())
            {
                changed = true;
            }
            else
            {
                break;
            }
        }
        return changed;
    }

    protected void postIteration(final StructuredGraph graph, final PhaseContextT context, EconomicSet<Node> changedNodes)
    {
        if (canonicalizer != null)
        {
            canonicalizer.applyIncremental(graph, context, changedNodes);
        }
    }

    protected abstract Closure<?> createEffectsClosure(PhaseContextT context, ScheduleResult schedule, ControlFlowGraph cfg);
}
