package giraaff.java;

import java.util.List;

import org.graalvm.collections.EconomicMap;

import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.AbstractMergeNode;
import giraaff.nodes.ControlSplitNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.LoopBeginNode;
import giraaff.nodes.LoopExitNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.cfg.ControlFlowGraph;
import giraaff.phases.Phase;
import giraaff.phases.graph.ReentrantNodeIterator;

// @class ComputeLoopFrequenciesClosure
public final class ComputeLoopFrequenciesClosure extends ReentrantNodeIterator.NodeIteratorClosure<Double>
{
    // @def
    private static final ComputeLoopFrequenciesClosure INSTANCE = new ComputeLoopFrequenciesClosure();

    // @cons
    private ComputeLoopFrequenciesClosure()
    {
        super();
    }

    @Override
    protected Double processNode(FixedNode __node, Double __currentState)
    {
        // normal nodes never change the probability of a path
        return __currentState;
    }

    @Override
    protected Double merge(AbstractMergeNode __merge, List<Double> __states)
    {
        // a merge has the sum of all predecessor probabilities
        double __result = 0.0;
        for (double __d : __states)
        {
            __result += __d;
        }
        return __result;
    }

    @Override
    protected Double afterSplit(AbstractBeginNode __node, Double __oldState)
    {
        // a control split splits up the probability
        ControlSplitNode __split = (ControlSplitNode) __node.predecessor();
        return __oldState * __split.probability(__node);
    }

    @Override
    protected EconomicMap<LoopExitNode, Double> processLoop(LoopBeginNode __loop, Double __initialState)
    {
        EconomicMap<LoopExitNode, Double> __exitStates = ReentrantNodeIterator.processLoop(this, __loop, 1D).exitStates;

        double __exitProbability = 0.0;
        for (double __d : __exitStates.getValues())
        {
            __exitProbability += __d;
        }
        __exitProbability = Math.min(1.0, __exitProbability);
        __exitProbability = Math.max(ControlFlowGraph.MIN_PROBABILITY, __exitProbability);
        double __loopFrequency = 1.0 / __exitProbability;
        __loop.setLoopFrequency(__loopFrequency);

        double __adjustmentFactor = __initialState * __loopFrequency;
        __exitStates.replaceAll((__exitNode, __probability) -> ControlFlowGraph.multiplyProbabilities(__probability, __adjustmentFactor));

        return __exitStates;
    }

    /**
     * Computes the frequencies of all loops in the given graph. This is done by performing a
     * reverse postorder iteration and computing the probability of all fixed nodes. The combined
     * probability of all exits of a loop can be used to compute the loop's expected frequency.
     */
    public static void compute(StructuredGraph __graph)
    {
        if (__graph.hasLoops())
        {
            ReentrantNodeIterator.apply(INSTANCE, __graph.start(), 1D);
        }
    }

    // @class ComputeLoopFrequenciesClosure.ComputeLoopFrequencyPhase
    public static final class ComputeLoopFrequencyPhase extends Phase
    {
        @Override
        protected void run(StructuredGraph __graph)
        {
            compute(__graph);
        }
    }

    // @def
    public static final ComputeLoopFrequencyPhase PHASE_INSTANCE = new ComputeLoopFrequencyPhase();
}
