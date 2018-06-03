package giraaff.phases.graph;

import java.util.ListIterator;

import giraaff.graph.Node;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.StructuredGraph.ScheduleResult;
import giraaff.nodes.cfg.Block;

/**
 * Iterates over a list of nodes, which usually comes from
 * {@link ScheduleResult#getBlockToNodesMap()}.
 *
 * While iterating, it is possible to {@link #insert(FixedNode, FixedWithNextNode) insert} and
 * {@link #replaceCurrent(FixedWithNextNode) replace} nodes.
 */
// @class ScheduledNodeIterator
public abstract class ScheduledNodeIterator
{
    // @field
    private FixedWithNextNode lastFixed;
    // @field
    private FixedWithNextNode reconnect;
    // @field
    private ListIterator<Node> iterator;

    public void processNodes(Block __block, ScheduleResult __schedule)
    {
        lastFixed = __block.getBeginNode();
        reconnect = null;
        iterator = __schedule.nodesFor(__block).listIterator();

        while (iterator.hasNext())
        {
            Node __node = iterator.next();
            if (!__node.isAlive())
            {
                continue;
            }
            if (reconnect != null && __node instanceof FixedNode)
            {
                reconnect.setNext((FixedNode) __node);
                reconnect = null;
            }
            if (__node instanceof FixedWithNextNode)
            {
                lastFixed = (FixedWithNextNode) __node;
            }
            processNode(__node);
        }
        if (reconnect != null)
        {
            reconnect.setNext(__block.getFirstSuccessor().getBeginNode());
        }
    }

    protected void insert(FixedNode __start, FixedWithNextNode __end)
    {
        this.lastFixed.setNext(__start);
        this.lastFixed = __end;
        this.reconnect = __end;
    }

    protected void replaceCurrent(FixedWithNextNode __newNode)
    {
        Node __current = iterator.previous();
        iterator.next(); // needed because of the previous() call
        __current.replaceAndDelete(__newNode);
        insert(__newNode, __newNode);
        iterator.set(__newNode);
    }

    protected abstract void processNode(Node node);
}
