package giraaff.virtual.phases.ea;

import org.graalvm.collections.EconomicSet;

import giraaff.graph.Graph.NodeEventScope;
import giraaff.graph.Node;
import giraaff.graph.spi.Simplifiable;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.StructuredGraph.ScheduleResult;
import giraaff.nodes.cfg.ControlFlowGraph;
import giraaff.phases.BasePhase;
import giraaff.phases.common.CanonicalizerPhase;
import giraaff.phases.common.DeadCodeEliminationPhase;
import giraaff.phases.common.DeadCodeEliminationPhase.Optionality;
import giraaff.phases.common.util.HashSetNodeEventListener;
import giraaff.phases.graph.ReentrantBlockIterator;
import giraaff.phases.schedule.SchedulePhase;
import giraaff.phases.tiers.PhaseContext;

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
        for (int iteration = 0; iteration < maxIterations; iteration++)
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

                new DeadCodeEliminationPhase(Optionality.Required).apply(graph);

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
