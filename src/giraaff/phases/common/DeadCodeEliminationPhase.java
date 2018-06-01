package giraaff.phases.common;

import giraaff.core.common.GraalOptions;
import giraaff.graph.Node;
import giraaff.graph.NodeFlood;
import giraaff.nodes.AbstractEndNode;
import giraaff.nodes.GuardNode;
import giraaff.nodes.StructuredGraph;
import giraaff.phases.Phase;

// @class DeadCodeEliminationPhase
public final class DeadCodeEliminationPhase extends Phase
{
    // @enum DeadCodeEliminationPhase.Optionality
    public enum Optionality
    {
        Optional,
        Required;
    }

    /**
     * Creates a dead code elimination phase that will be run irrespective of {@link GraalOptions#reduceDCE}.
     */
    // @cons
    public DeadCodeEliminationPhase()
    {
        this(Optionality.Required);
    }

    /**
     * Creates a dead code elimination phase that will be run only if it is
     * {@linkplain Optionality#Required non-optional} or {@link GraalOptions#reduceDCE} is false.
     */
    // @cons
    public DeadCodeEliminationPhase(Optionality optionality)
    {
        super();
        this.optional = optionality == Optionality.Optional;
    }

    private final boolean optional;

    @Override
    public void run(StructuredGraph graph)
    {
        if (optional && GraalOptions.reduceDCE)
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
        }

        deleteNodes(flood, graph);
    }

    private static void iterateSuccessorsAndInputs(NodeFlood flood)
    {
        // @closure
        Node.EdgeVisitor consumer = new Node.EdgeVisitor()
        {
            @Override
            public Node apply(Node n, Node succOrInput)
            {
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
        // @closure
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

        for (Node node : graph.getNodes())
        {
            if (!flood.isMarked(node))
            {
                node.markDeleted();
                node.applyInputs(consumer);
            }
        }
    }
}
