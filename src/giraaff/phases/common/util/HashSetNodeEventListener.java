package giraaff.phases.common.util;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.graalvm.collections.EconomicSet;
import org.graalvm.collections.Equivalence;

import giraaff.graph.Graph;
import giraaff.graph.Node;

///
// A simple {@link Graph.NodeEventListener} implementation that accumulates event nodes in a {@link HashSet}.
///
// @class HashSetNodeEventListener
public final class HashSetNodeEventListener extends Graph.NodeEventListener
{
    // @field
    private final EconomicSet<Node> ___nodes;
    // @field
    private final Set<Graph.NodeEvent> ___filter;

    ///
    // Creates a {@link Graph.NodeEventListener} that collects nodes from all events.
    ///
    // @cons HashSetNodeEventListener
    public HashSetNodeEventListener()
    {
        super();
        this.___nodes = EconomicSet.create(Equivalence.IDENTITY);
        this.___filter = EnumSet.allOf(Graph.NodeEvent.class);
    }

    ///
    // Creates a {@link Graph.NodeEventListener} that collects nodes from all events that match a given filter.
    ///
    // @cons HashSetNodeEventListener
    public HashSetNodeEventListener(Set<Graph.NodeEvent> __filter)
    {
        super();
        this.___nodes = EconomicSet.create(Equivalence.IDENTITY);
        this.___filter = __filter;
    }

    ///
    // Excludes a given event from those for which nodes are collected.
    ///
    public HashSetNodeEventListener exclude(Graph.NodeEvent __e)
    {
        this.___filter.remove(__e);
        return this;
    }

    @Override
    public void changed(Graph.NodeEvent __e, Node __node)
    {
        if (this.___filter.contains(__e))
        {
            this.___nodes.add(__node);
            if (__node instanceof Node.IndirectCanonicalization)
            {
                for (Node __usage : __node.usages())
                {
                    this.___nodes.add(__usage);
                }
            }
        }
    }

    ///
    // Gets the set being used to accumulate the nodes communicated to this listener.
    ///
    public EconomicSet<Node> getNodes()
    {
        return this.___nodes;
    }
}
