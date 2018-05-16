package graalvm.compiler.phases.common;

import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.graph.Node;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.options.Option;
import graalvm.compiler.options.OptionKey;
import graalvm.compiler.options.OptionType;
import graalvm.compiler.phases.BasePhase;
import graalvm.compiler.phases.tiers.PhaseContext;

public class NodeCounterPhase extends BasePhase<PhaseContext>
{
    public static class Options
    {
        @Option(help = "Counts the number of instances of each node class.", type = OptionType.Debug)
        public static final OptionKey<Boolean> NodeCounters = new OptionKey<>(false);
    }

    @Override
    protected void run(StructuredGraph graph, PhaseContext context)
    {
        for (Node node : graph.getNodes())
        {
            DebugContext.counter("NodeCounter_%s", node.getNodeClass().getClazz().getSimpleName()).increment(node.getDebug());
        }
    }
}
