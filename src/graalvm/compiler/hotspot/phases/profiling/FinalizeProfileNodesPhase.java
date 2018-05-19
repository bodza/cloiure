package graalvm.compiler.hotspot.phases.profiling;

import static graalvm.compiler.hotspot.nodes.profiling.ProfileInvokeNode.getProfileInvokeNodes;
import static graalvm.compiler.hotspot.nodes.profiling.ProfileNode.getProfileNodes;

import java.util.HashMap;
import java.util.Map;

import graalvm.compiler.core.common.cfg.Loop;
import graalvm.compiler.hotspot.nodes.profiling.ProfileInvokeNode;
import graalvm.compiler.hotspot.nodes.profiling.ProfileNode;
import graalvm.compiler.hotspot.nodes.profiling.RandomSeedNode;
import graalvm.compiler.nodes.ConstantNode;
import graalvm.compiler.nodes.InvokeNode;
import graalvm.compiler.nodes.LoopBeginNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.PhiNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.ValuePhiNode;
import graalvm.compiler.nodes.calc.AddNode;
import graalvm.compiler.nodes.calc.MulNode;
import graalvm.compiler.nodes.cfg.Block;
import graalvm.compiler.nodes.cfg.ControlFlowGraph;
import graalvm.compiler.nodes.util.GraphUtil;
import graalvm.compiler.options.Option;
import graalvm.compiler.options.OptionKey;
import graalvm.compiler.options.OptionType;
import graalvm.compiler.phases.BasePhase;
import graalvm.compiler.phases.tiers.PhaseContext;

import jdk.vm.ci.meta.ResolvedJavaMethod;

public class FinalizeProfileNodesPhase extends BasePhase<PhaseContext>
{
    private int inlineeInvokeNotificationFreqLog;

    public static class Options
    {
        @Option(help = "Profile simple methods", type = OptionType.Expert)
        public static final OptionKey<Boolean> ProfileSimpleMethods = new OptionKey<>(true);
        @Option(help = "Maximum number of nodes in a graph for a simple method", type = OptionType.Expert)
        public static final OptionKey<Integer> SimpleMethodGraphSize = new OptionKey<>(256);
        @Option(help = "Maximum number of calls in a simple method", type = OptionType.Expert)
        public static final OptionKey<Integer> SimpleMethodCalls = new OptionKey<>(1);
        @Option(help = "Maximum number of indirect calls in a simple moethod", type = OptionType.Expert)
        public static final OptionKey<Integer> SimpleMethodIndirectCalls = new OptionKey<>(0);
    }

    public FinalizeProfileNodesPhase(int inlineeInvokeNotificationFreqLog)
    {
        this.inlineeInvokeNotificationFreqLog = inlineeInvokeNotificationFreqLog;
    }

    private static void removeAllProfilingNodes(StructuredGraph graph)
    {
        getProfileNodes(graph).forEach((n) -> GraphUtil.removeFixedWithUnusedInputs(n));
    }

    private void assignInlineeInvokeFrequencies(StructuredGraph graph)
    {
        for (ProfileInvokeNode node : getProfileInvokeNodes(graph))
        {
            ResolvedJavaMethod profiledMethod = node.getProfiledMethod();
            if (!profiledMethod.equals(graph.method()))
            {
                // Some inlinee, reassign the inlinee frequency
                node.setNotificationFreqLog(inlineeInvokeNotificationFreqLog);
            }
        }
    }

    // Hacky heuristic to determine whether we want any profiling in this method.
    // The heuristic is applied after the graph is fully formed and before the first lowering.
    private static boolean simpleMethodHeuristic(StructuredGraph graph)
    {
        if (Options.ProfileSimpleMethods.getValue(graph.getOptions()))
        {
            return false;
        }

        // Check if the graph is smallish..
        if (graph.getNodeCount() > Options.SimpleMethodGraphSize.getValue(graph.getOptions()))
        {
            return false;
        }

        // Check if method has loops
        if (graph.hasLoops())
        {
            return false;
        }

        // Check if method has calls
        if (graph.getNodes().filter(InvokeNode.class).count() > Options.SimpleMethodCalls.getValue(graph.getOptions()))
        {
            return false;
        }

        // Check if method has calls that need profiling
        if (graph.getNodes().filter(InvokeNode.class).filter((n) -> ((InvokeNode) n).getInvokeKind().isIndirect()).count() > Options.SimpleMethodIndirectCalls.getDefaultValue())
        {
            return false;
        }

        return true;
    }

    private static void assignRandomSources(StructuredGraph graph)
    {
        ValueNode seed = graph.unique(new RandomSeedNode());
        ControlFlowGraph cfg = ControlFlowGraph.compute(graph, false, true, false, false);
        Map<LoopBeginNode, ValueNode> loopRandomValueCache = new HashMap<>();

        for (ProfileNode node : getProfileNodes(graph))
        {
            ValueNode random;
            Block block = cfg.blockFor(node);
            Loop<Block> loop = block.getLoop();
            // Inject <a href=https://en.wikipedia.org/wiki/Linear_congruential_generator>LCG</a>
            // pseudo-random number generator into the loop
            if (loop != null)
            {
                LoopBeginNode loopBegin = (LoopBeginNode) loop.getHeader().getBeginNode();
                random = loopRandomValueCache.get(loopBegin);
                if (random == null)
                {
                    PhiNode phi = graph.addWithoutUnique(new ValuePhiNode(seed.stamp(NodeView.DEFAULT), loopBegin));
                    phi.addInput(seed);
                    // X_{n+1} = a*X_n + c, using glibc-like constants
                    ValueNode a = ConstantNode.forInt(1103515245, graph);
                    ValueNode c = ConstantNode.forInt(12345, graph);
                    ValueNode next = graph.addOrUniqueWithInputs(new AddNode(c, new MulNode(phi, a)));
                    for (int i = 0; i < loopBegin.getLoopEndCount(); i++)
                    {
                        phi.addInput(next);
                    }
                    random = phi;
                    loopRandomValueCache.put(loopBegin, random);
                }
            }
            else
            {
                // Graal doesn't compile methods with irreducible loops. So all profile nodes that
                // are not in a loop are guaranteed to be executed at most once. We feed the seed
                // value to such nodes directly.
                random = seed;
            }
            node.setRandom(random);
        }
    }

    @Override
    protected void run(StructuredGraph graph, PhaseContext context)
    {
        if (simpleMethodHeuristic(graph))
        {
            removeAllProfilingNodes(graph);
            return;
        }

        assignInlineeInvokeFrequencies(graph);
        if (ProfileNode.Options.ProbabilisticProfiling.getValue(graph.getOptions()))
        {
            assignRandomSources(graph);
        }
    }

    @Override
    public boolean checkContract()
    {
        return false;
    }
}
