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
    public DeadCodeEliminationPhase(Optionality __optionality)
    {
        super();
        this.optional = __optionality == Optionality.Optional;
    }

    // @field
    private final boolean optional;

    @Override
    public void run(StructuredGraph __graph)
    {
        if (optional && GraalOptions.reduceDCE)
        {
            return;
        }

        NodeFlood __flood = __graph.createNodeFlood();
        int __totalNodeCount = __graph.getNodeCount();
        __flood.add(__graph.start());
        iterateSuccessorsAndInputs(__flood);
        boolean __changed = false;
        for (GuardNode __guard : __graph.getNodes(GuardNode.TYPE))
        {
            if (__flood.isMarked(__guard.getAnchor().asNode()))
            {
                __flood.add(__guard);
                __changed = true;
            }
        }
        if (__changed)
        {
            iterateSuccessorsAndInputs(__flood);
        }
        int __totalMarkedCount = __flood.getTotalMarkedCount();
        if (__totalNodeCount == __totalMarkedCount)
        {
            // All nodes are live => nothing more to do.
            return;
        }
        else
        {
            // Some nodes are not marked alive and therefore dead => proceed.
        }

        deleteNodes(__flood, __graph);
    }

    private static void iterateSuccessorsAndInputs(NodeFlood __flood)
    {
        // @closure
        Node.EdgeVisitor consumer = new Node.EdgeVisitor()
        {
            @Override
            public Node apply(Node __n, Node __succOrInput)
            {
                __flood.add(__succOrInput);
                return __succOrInput;
            }
        };

        for (Node __current : __flood)
        {
            if (__current instanceof AbstractEndNode)
            {
                AbstractEndNode __end = (AbstractEndNode) __current;
                __flood.add(__end.merge());
            }
            else
            {
                __current.applySuccessors(consumer);
                __current.applyInputs(consumer);
            }
        }
    }

    private static void deleteNodes(NodeFlood __flood, StructuredGraph __graph)
    {
        // @closure
        Node.EdgeVisitor consumer = new Node.EdgeVisitor()
        {
            @Override
            public Node apply(Node __n, Node __input)
            {
                if (__input.isAlive() && __flood.isMarked(__input))
                {
                    __input.removeUsage(__n);
                }
                return __input;
            }
        };

        for (Node __node : __graph.getNodes())
        {
            if (!__flood.isMarked(__node))
            {
                __node.markDeleted();
                __node.applyInputs(consumer);
            }
        }
    }
}
