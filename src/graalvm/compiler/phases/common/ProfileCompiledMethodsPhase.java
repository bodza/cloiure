package graalvm.compiler.phases.common;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import graalvm.compiler.core.common.cfg.Loop;
import graalvm.compiler.graph.Node;
import graalvm.compiler.nodes.AbstractBeginNode;
import graalvm.compiler.nodes.AbstractEndNode;
import graalvm.compiler.nodes.AbstractMergeNode;
import graalvm.compiler.nodes.CallTargetNode;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.DeoptimizeNode;
import graalvm.compiler.nodes.FixedNode;
import graalvm.compiler.nodes.FixedWithNextNode;
import graalvm.compiler.nodes.IfNode;
import graalvm.compiler.nodes.Invoke;
import graalvm.compiler.nodes.LogicNode;
import graalvm.compiler.nodes.ParameterNode;
import graalvm.compiler.nodes.ReturnNode;
import graalvm.compiler.nodes.SafepointNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.StructuredGraph.ScheduleResult;
import graalvm.compiler.nodes.UnwindNode;
import graalvm.compiler.nodes.VirtualState;
import graalvm.compiler.nodes.calc.BinaryNode;
import graalvm.compiler.nodes.calc.ConvertNode;
import graalvm.compiler.nodes.calc.FloatDivNode;
import graalvm.compiler.nodes.calc.IntegerDivRemNode;
import graalvm.compiler.nodes.calc.MulNode;
import graalvm.compiler.nodes.calc.NotNode;
import graalvm.compiler.nodes.calc.ReinterpretNode;
import graalvm.compiler.nodes.calc.RemNode;
import graalvm.compiler.nodes.cfg.Block;
import graalvm.compiler.nodes.cfg.ControlFlowGraph;
import graalvm.compiler.nodes.debug.DynamicCounterNode;
import graalvm.compiler.nodes.extended.SwitchNode;
import graalvm.compiler.nodes.java.AbstractNewObjectNode;
import graalvm.compiler.nodes.java.AccessMonitorNode;
import graalvm.compiler.nodes.java.MonitorIdNode;
import graalvm.compiler.nodes.memory.Access;
import graalvm.compiler.nodes.spi.ValueProxy;
import graalvm.compiler.nodes.virtual.VirtualObjectNode;
import graalvm.compiler.phases.Phase;
import graalvm.compiler.phases.schedule.SchedulePhase;

/**
 * This phase add counters for the dynamically executed number of nodes. Incrementing the counter
 * for each node would be too costly, so this phase takes the compromise that it trusts split
 * probabilities, but not loop frequencies. This means that it will insert counters at the start of
 * a method and at each loop header.
 *
 * A schedule is created so that floating nodes can also be taken into account. The weight of a node
 * is determined heuristically in the {@link ProfileCompiledMethodsPhase#getNodeWeight(Node)}
 * method.
 *
 * Additionally, there's a second counter that's only increased for code sections without invokes.
 */
public class ProfileCompiledMethodsPhase extends Phase {

    private static final String GROUP_NAME = "~profiled weight";
    private static final String GROUP_NAME_WITHOUT = "~profiled weight (invoke-free sections)";
    private static final String GROUP_NAME_INVOKES = "~profiled invokes";

    private static final boolean WITH_SECTION_HEADER = Boolean.parseBoolean(System.getProperty("ProfileCompiledMethodsPhase.WITH_SECTION_HEADER", "false"));
    private static final boolean WITH_INVOKE_FREE_SECTIONS = Boolean.parseBoolean(System.getProperty("ProfileCompiledMethodsPhase.WITH_FREE_SECTIONS", "false"));
    private static final boolean WITH_INVOKES = Boolean.parseBoolean(System.getProperty("ProfileCompiledMethodsPhase.WITH_INVOKES", "true"));

    @Override
    protected void run(StructuredGraph graph) {
        SchedulePhase schedule = new SchedulePhase(graph.getOptions());
        schedule.apply(graph, false);

        ControlFlowGraph cfg = ControlFlowGraph.compute(graph, true, true, true, true);
        for (Loop<Block> loop : cfg.getLoops()) {
            double loopProbability = cfg.blockFor(loop.getHeader().getBeginNode()).probability();
            if (loopProbability > (1D / Integer.MAX_VALUE)) {
                addSectionCounters(loop.getHeader().getBeginNode(), loop.getBlocks(), loop.getChildren(), graph.getLastSchedule(), cfg);
            }
        }
        // don't put the counter increase directly after the start (problems with OSR)
        FixedWithNextNode current = graph.start();
        while (current.next() instanceof FixedWithNextNode) {
            current = (FixedWithNextNode) current.next();
        }
        addSectionCounters(current, Arrays.asList(cfg.getBlocks()), cfg.getLoops(), graph.getLastSchedule(), cfg);

        if (WITH_INVOKES) {
            for (Node node : graph.getNodes()) {
                if (node instanceof Invoke) {
                    Invoke invoke = (Invoke) node;
                    DynamicCounterNode.addCounterBefore(GROUP_NAME_INVOKES, invoke.callTarget().targetName(), 1, true, invoke.asNode());

                }
            }
        }
    }

    private static void addSectionCounters(FixedWithNextNode start, Collection<Block> sectionBlocks, Collection<Loop<Block>> childLoops, ScheduleResult schedule, ControlFlowGraph cfg) {
        HashSet<Block> blocks = new HashSet<>(sectionBlocks);
        for (Loop<Block> loop : childLoops) {
            blocks.removeAll(loop.getBlocks());
        }
        double weight = getSectionWeight(schedule, blocks) / cfg.blockFor(start).probability();
        DynamicCounterNode.addCounterBefore(GROUP_NAME, sectionHead(start), (long) weight, true, start.next());
        if (WITH_INVOKE_FREE_SECTIONS && !hasInvoke(blocks)) {
            DynamicCounterNode.addCounterBefore(GROUP_NAME_WITHOUT, sectionHead(start), (long) weight, true, start.next());
        }
    }

    private static String sectionHead(Node node) {
        if (WITH_SECTION_HEADER) {
            return node.toString();
        } else {
            return "";
        }
    }

    private static double getSectionWeight(ScheduleResult schedule, Collection<Block> blocks) {
        double count = 0;
        for (Block block : blocks) {
            double blockProbability = block.probability();
            for (Node node : schedule.getBlockToNodesMap().get(block)) {
                count += blockProbability * getNodeWeight(node);
            }
        }
        return count;
    }

    private static double getNodeWeight(Node node) {
        if (node instanceof AbstractMergeNode) {
            return ((AbstractMergeNode) node).phiPredecessorCount();
        } else if (node instanceof AbstractBeginNode || node instanceof AbstractEndNode || node instanceof MonitorIdNode || node instanceof ConstantNode || node instanceof ParameterNode ||
                        node instanceof CallTargetNode || node instanceof ValueProxy || node instanceof VirtualObjectNode || node instanceof ReinterpretNode) {
            return 0;
        } else if (node instanceof AccessMonitorNode) {
            return 10;
        } else if (node instanceof Access) {
            return 2;
        } else if (node instanceof LogicNode || node instanceof ConvertNode || node instanceof NotNode) {
            return 1;
        } else if (node instanceof IntegerDivRemNode || node instanceof FloatDivNode || node instanceof RemNode) {
            return 10;
        } else if (node instanceof MulNode) {
            return 3;
        } else if (node instanceof Invoke) {
            return 5;
        } else if (node instanceof IfNode || node instanceof SafepointNode || node instanceof BinaryNode) {
            return 1;
        } else if (node instanceof SwitchNode) {
            return node.successors().count();
        } else if (node instanceof ReturnNode || node instanceof UnwindNode || node instanceof DeoptimizeNode) {
            return node.successors().count();
        } else if (node instanceof AbstractNewObjectNode) {
            return 10;
        } else if (node instanceof VirtualState) {
            return 0;
        }
        return 2;
    }

    private static boolean hasInvoke(Collection<Block> blocks) {
        boolean hasInvoke = false;
        for (Block block : blocks) {
            for (FixedNode fixed : block.getNodes()) {
                if (fixed instanceof Invoke) {
                    hasInvoke = true;
                }
            }
        }
        return hasInvoke;
    }

    @Override
    public boolean checkContract() {
        return false;
    }

}
