package graalvm.compiler.java;

import java.util.List;

import org.graalvm.collections.EconomicMap;

import graalvm.compiler.nodes.AbstractBeginNode;
import graalvm.compiler.nodes.AbstractMergeNode;
import graalvm.compiler.nodes.ControlSplitNode;
import graalvm.compiler.nodes.FixedNode;
import graalvm.compiler.nodes.LoopBeginNode;
import graalvm.compiler.nodes.LoopExitNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.cfg.ControlFlowGraph;
import graalvm.compiler.phases.Phase;
import graalvm.compiler.phases.graph.ReentrantNodeIterator;

public final class ComputeLoopFrequenciesClosure extends ReentrantNodeIterator.NodeIteratorClosure<Double>
{
    private static final ComputeLoopFrequenciesClosure INSTANCE = new ComputeLoopFrequenciesClosure();

    private ComputeLoopFrequenciesClosure()
    {
        // nothing to do
    }

    @Override
    protected Double processNode(FixedNode node, Double currentState)
    {
        // normal nodes never change the probability of a path
        return currentState;
    }

    @Override
    protected Double merge(AbstractMergeNode merge, List<Double> states)
    {
        // a merge has the sum of all predecessor probabilities
        double result = 0.0;
        for (double d : states)
        {
            result += d;
        }
        return result;
    }

    @Override
    protected Double afterSplit(AbstractBeginNode node, Double oldState)
    {
        // a control split splits up the probability
        ControlSplitNode split = (ControlSplitNode) node.predecessor();
        return oldState * split.probability(node);
    }

    @Override
    protected EconomicMap<LoopExitNode, Double> processLoop(LoopBeginNode loop, Double initialState)
    {
        EconomicMap<LoopExitNode, Double> exitStates = ReentrantNodeIterator.processLoop(this, loop, 1D).exitStates;

        double exitProbability = 0.0;
        for (double d : exitStates.getValues())
        {
            exitProbability += d;
        }
        exitProbability = Math.min(1.0, exitProbability);
        exitProbability = Math.max(ControlFlowGraph.MIN_PROBABILITY, exitProbability);
        double loopFrequency = 1.0 / exitProbability;
        loop.setLoopFrequency(loopFrequency);

        double adjustmentFactor = initialState * loopFrequency;
        exitStates.replaceAll((exitNode, probability) -> ControlFlowGraph.multiplyProbabilities(probability, adjustmentFactor));

        return exitStates;
    }

    /**
     * Computes the frequencies of all loops in the given graph. This is done by performing a
     * reverse postorder iteration and computing the probability of all fixed nodes. The combined
     * probability of all exits of a loop can be used to compute the loop's expected frequency.
     */
    public static void compute(StructuredGraph graph)
    {
        if (graph.hasLoops())
        {
            ReentrantNodeIterator.apply(INSTANCE, graph.start(), 1D);
        }
    }

    public static class ComputeLoopFrequencyPhase extends Phase
    {
        @Override
        protected void run(StructuredGraph graph)
        {
            compute(graph);
        }
    }

    public static final ComputeLoopFrequencyPhase PHASE_INSTANCE = new ComputeLoopFrequencyPhase();
}
