package giraaff.phases.common.util;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.graalvm.collections.EconomicSet;
import org.graalvm.collections.Equivalence;

import giraaff.graph.Graph.NodeEvent;
import giraaff.graph.Graph.NodeEventListener;
import giraaff.graph.Node;
import giraaff.graph.Node.IndirectCanonicalization;

///
// A simple {@link NodeEventListener} implementation that accumulates event nodes in a {@link HashSet}.
///
// @class HashSetNodeEventListener
public final class HashSetNodeEventListener extends NodeEventListener
{
    // @field
    private final EconomicSet<Node> ___nodes;
    // @field
    private final Set<NodeEvent> ___filter;

    ///
    // Creates a {@link NodeEventListener} that collects nodes from all events.
    ///
    // @cons
    public HashSetNodeEventListener()
    {
        super();
        this.___nodes = EconomicSet.create(Equivalence.IDENTITY);
        this.___filter = EnumSet.allOf(NodeEvent.class);
    }

    ///
    // Creates a {@link NodeEventListener} that collects nodes from all events that match a given filter.
    ///
    // @cons
    public HashSetNodeEventListener(Set<NodeEvent> __filter)
    {
        super();
        this.___nodes = EconomicSet.create(Equivalence.IDENTITY);
        this.___filter = __filter;
    }

    ///
    // Excludes a given event from those for which nodes are collected.
    ///
    public HashSetNodeEventListener exclude(NodeEvent __e)
    {
        this.___filter.remove(__e);
        return this;
    }

    @Override
    public void changed(NodeEvent __e, Node __node)
    {
        if (this.___filter.contains(__e))
        {
            this.___nodes.add(__node);
            if (__node instanceof IndirectCanonicalization)
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
