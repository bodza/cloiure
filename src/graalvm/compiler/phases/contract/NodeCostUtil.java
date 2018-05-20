package graalvm.compiler.phases.contract;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import graalvm.compiler.core.common.cfg.BlockMap;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.VerificationError;
import graalvm.compiler.nodes.FixedNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.cfg.Block;
import graalvm.compiler.nodes.cfg.ControlFlowGraph;
import graalvm.compiler.phases.schedule.SchedulePhase;

import jdk.vm.ci.meta.ResolvedJavaMethod;

public class NodeCostUtil
{
    public static int computeGraphSize(StructuredGraph graph)
    {
        int size = 0;
        for (Node n : graph.getNodes())
        {
            size += n.estimatedNodeSize().value;
        }
        return size;
    }

    public static double computeGraphCycles(StructuredGraph graph, boolean fullSchedule)
    {
        Function<Block, Iterable<? extends Node>> blockToNodes;
        ControlFlowGraph cfg;
        if (fullSchedule)
        {
            SchedulePhase schedule = new SchedulePhase(SchedulePhase.SchedulingStrategy.LATEST_OUT_OF_LOOPS, true);
            schedule.apply(graph);
            cfg = graph.getLastSchedule().getCFG();
            blockToNodes = b -> graph.getLastSchedule().getBlockToNodesMap().get(b);
        }
        else
        {
            cfg = ControlFlowGraph.compute(graph, true, true, false, false);
            BlockMap<List<FixedNode>> nodes = new BlockMap<>(cfg);
            for (Block b : cfg.getBlocks())
            {
                ArrayList<FixedNode> curNodes = new ArrayList<>();
                for (FixedNode node : b.getNodes())
                {
                    curNodes.add(node);
                }
                nodes.put(b, curNodes);
            }
            blockToNodes = b -> nodes.get(b);
        }
        double weightedCycles = 0D;
        for (Block block : cfg.getBlocks())
        {
            for (Node n : blockToNodes.apply(block))
            {
                double probWeighted = n.estimatedNodeCycles().value * block.probability();
                weightedCycles += probWeighted;
            }
        }
        return weightedCycles;
    }

    private static int deltaCompare(double a, double b, double delta)
    {
        if (Math.abs(a - b) <= delta)
        {
            return 0;
        }
        return Double.compare(a, b);
    }

    /**
     * Factor to control the "imprecision" of the before - after relation when verifying phase
     * effects. If the cost model is perfect the best theoretical value is 0.0D (Ignoring the fact
     * that profiling information is not reliable and thus the, probability based, profiling view on
     * a graph is different than the reality).
     */
    private static final double DELTA = 0.001D;

    public static void phaseFulfillsSizeContract(StructuredGraph graph, int codeSizeBefore, int codeSizeAfter, PhaseSizeContract contract)
    {
        final double codeSizeIncrease = contract.codeSizeIncrease();
        final double graphSizeDelta = codeSizeBefore * DELTA;
        if (deltaCompare(codeSizeAfter, codeSizeBefore * codeSizeIncrease, graphSizeDelta) > 0)
        {
            ResolvedJavaMethod method = graph.method();
            double increase = (double) codeSizeAfter / (double) codeSizeBefore;
            throw new VerificationError("Phase %s expects to increase code size by at most a factor of %.2f but an increase of %.2f was seen (code size before: %d, after: %d)%s", contract.contractorName(), codeSizeIncrease, increase, codeSizeBefore, codeSizeAfter, method != null ? " when compiling method " + method.format("%H.%n(%p)") + "." : ".");
        }
    }
}
