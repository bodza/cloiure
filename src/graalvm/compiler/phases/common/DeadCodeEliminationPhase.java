package graalvm.compiler.phases.common;

import graalvm.compiler.debug.CounterKey;
import graalvm.compiler.debug.DebugContext;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeFlood;
import graalvm.compiler.nodes.AbstractEndNode;
import graalvm.compiler.nodes.GuardNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.options.Option;
import graalvm.compiler.options.OptionKey;
import graalvm.compiler.options.OptionType;
import graalvm.compiler.phases.Phase;

public class DeadCodeEliminationPhase extends Phase
{
    public static class Options
    {
        @Option(help = "Disable optional dead code eliminations", type = OptionType.Debug)
        public static final OptionKey<Boolean> ReduceDCE = new OptionKey<>(true);
    }

    private static final CounterKey counterNodesRemoved = DebugContext.counter("NodesRemoved");

    public enum Optionality
    {
        Optional,
        Required;
    }

    /**
     * Creates a dead code elimination phase that will be run irrespective of
     * {@link Options#ReduceDCE}.
     */
    public DeadCodeEliminationPhase()
    {
        this(Optionality.Required);
    }

    /**
     * Creates a dead code elimination phase that will be run only if it is
     * {@linkplain Optionality#Required non-optional} or {@link Options#ReduceDCE} is false.
     */
    public DeadCodeEliminationPhase(Optionality optionality)
    {
        this.optional = optionality == Optionality.Optional;
    }

    private final boolean optional;

    @Override
    public void run(StructuredGraph graph)
    {
        if (optional && Options.ReduceDCE.getValue(graph.getOptions()))
        {
            return;
        }

        NodeFlood flood = graph.createNodeFlood();
        int totalNodeCount = graph.getNodeCount();
        flood.add(graph.start());
        iterateSuccessorsAndInputs(flood);
        boolean changed = false;
        for (GuardNode guard : graph.getNodes(GuardNode.TYPE))
        {
            if (flood.isMarked(guard.getAnchor().asNode()))
            {
                flood.add(guard);
                changed = true;
            }
        }
        if (changed)
        {
            iterateSuccessorsAndInputs(flood);
        }
        int totalMarkedCount = flood.getTotalMarkedCount();
        if (totalNodeCount == totalMarkedCount)
        {
            // All nodes are live => nothing more to do.
            return;
        }
        else
        {
            // Some nodes are not marked alive and therefore dead => proceed.
            assert totalNodeCount > totalMarkedCount;
        }

        deleteNodes(flood, graph);
    }

    private static void iterateSuccessorsAndInputs(NodeFlood flood)
    {
        Node.EdgeVisitor consumer = new Node.EdgeVisitor()
        {
            @Override
            public Node apply(Node n, Node succOrInput)
            {
                assert succOrInput.isAlive() : "dead successor or input " + succOrInput + " in " + n;
                flood.add(succOrInput);
                return succOrInput;
            }
        };

        for (Node current : flood)
        {
            if (current instanceof AbstractEndNode)
            {
                AbstractEndNode end = (AbstractEndNode) current;
                flood.add(end.merge());
            }
            else
            {
                current.applySuccessors(consumer);
                current.applyInputs(consumer);
            }
        }
    }

    private static void deleteNodes(NodeFlood flood, StructuredGraph graph)
    {
        Node.EdgeVisitor consumer = new Node.EdgeVisitor()
        {
            @Override
            public Node apply(Node n, Node input)
            {
                if (input.isAlive() && flood.isMarked(input))
                {
                    input.removeUsage(n);
                }
                return input;
            }
        };

        DebugContext debug = graph.getDebug();
        for (Node node : graph.getNodes())
        {
            if (!flood.isMarked(node))
            {
                node.markDeleted();
                node.applyInputs(consumer);
                counterNodesRemoved.increment(debug);
            }
        }
    }
}
