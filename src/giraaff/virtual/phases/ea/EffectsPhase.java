package giraaff.virtual.phases.ea;

import org.graalvm.collections.EconomicSet;

import giraaff.graph.Graph;
import giraaff.graph.Node;
import giraaff.graph.spi.Simplifiable;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.cfg.ControlFlowGraph;
import giraaff.phases.BasePhase;
import giraaff.phases.common.CanonicalizerPhase;
import giraaff.phases.common.DeadCodeEliminationPhase;
import giraaff.phases.common.util.HashSetNodeEventListener;
import giraaff.phases.graph.ReentrantBlockIterator;
import giraaff.phases.schedule.SchedulePhase;
import giraaff.phases.tiers.PhaseContext;

// @class EffectsPhase
public abstract class EffectsPhase<PhaseContextT extends PhaseContext> extends BasePhase<PhaseContextT>
{
    // @class EffectsPhase.Closure
    public abstract static class Closure<T> extends ReentrantBlockIterator.BlockIteratorClosure<T>
    {
        public abstract boolean hasChanged();

        public abstract boolean needsApplyEffects();

        public abstract void applyEffects();
    }

    // @field
    private final int ___maxIterations;
    // @field
    protected final CanonicalizerPhase ___canonicalizer;
    // @field
    private final boolean ___unscheduled;

    // @cons EffectsPhase
    protected EffectsPhase(int __maxIterations, CanonicalizerPhase __canonicalizer)
    {
        this(__maxIterations, __canonicalizer, false);
    }

    // @cons EffectsPhase
    protected EffectsPhase(int __maxIterations, CanonicalizerPhase __canonicalizer, boolean __unscheduled)
    {
        super();
        this.___maxIterations = __maxIterations;
        this.___canonicalizer = __canonicalizer;
        this.___unscheduled = __unscheduled;
    }

    @Override
    protected void run(StructuredGraph __graph, PhaseContextT __context)
    {
        runAnalysis(__graph, __context);
    }

    public boolean runAnalysis(StructuredGraph __graph, PhaseContextT __context)
    {
        boolean __changed = false;
        for (int __iteration = 0; __iteration < this.___maxIterations; __iteration++)
        {
            StructuredGraph.ScheduleResult __schedule;
            ControlFlowGraph __cfg;
            if (this.___unscheduled)
            {
                __schedule = null;
                __cfg = ControlFlowGraph.compute(__graph, true, true, false, false);
            }
            else
            {
                new SchedulePhase(SchedulePhase.SchedulingStrategy.EARLIEST).apply(__graph);
                __schedule = __graph.getLastSchedule();
                __cfg = __schedule.getCFG();
            }
            EffectsPhase.Closure<?> __closure = createEffectsClosure(__context, __schedule, __cfg);
            ReentrantBlockIterator.apply(__closure, __cfg.getStartBlock());

            if (__closure.needsApplyEffects())
            {
                // apply the effects collected during this iteration
                HashSetNodeEventListener __listener = new HashSetNodeEventListener();
                try (Graph.NodeEventScope __nes = __graph.trackNodeEvents(__listener))
                {
                    __closure.applyEffects();
                }

                new DeadCodeEliminationPhase(DeadCodeEliminationPhase.Optionality.Required).apply(__graph);

                EconomicSet<Node> __changedNodes = __listener.getNodes();
                for (Node __node : __graph.getNodes())
                {
                    if (__node instanceof Simplifiable)
                    {
                        __changedNodes.add(__node);
                    }
                }
                postIteration(__graph, __context, __changedNodes);
            }

            if (__closure.hasChanged())
            {
                __changed = true;
            }
            else
            {
                break;
            }
        }
        return __changed;
    }

    protected void postIteration(final StructuredGraph __graph, final PhaseContextT __context, EconomicSet<Node> __changedNodes)
    {
        if (this.___canonicalizer != null)
        {
            this.___canonicalizer.applyIncremental(__graph, __context, __changedNodes);
        }
    }

    protected abstract EffectsPhase.Closure<?> createEffectsClosure(PhaseContextT __context, StructuredGraph.ScheduleResult __schedule, ControlFlowGraph __cfg);
}
