package giraaff.phases.graph;

import java.util.ListIterator;

import giraaff.graph.Node;
import giraaff.nodes.FixedNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.StructuredGraph.ScheduleResult;
import giraaff.nodes.cfg.Block;

///
// Iterates over a list of nodes, which usually comes from
// {@link ScheduleResult#getBlockToNodesMap()}.
//
// While iterating, it is possible to {@link #insert(FixedNode, FixedWithNextNode) insert} and
// {@link #replaceCurrent(FixedWithNextNode) replace} nodes.
///
// @class ScheduledNodeIterator
public abstract class ScheduledNodeIterator
{
    // @field
    private FixedWithNextNode ___lastFixed;
    // @field
    private FixedWithNextNode ___reconnect;
    // @field
    private ListIterator<Node> ___iterator;

    public void processNodes(Block __block, ScheduleResult __schedule)
    {
        this.___lastFixed = __block.getBeginNode();
        this.___reconnect = null;
        this.___iterator = __schedule.nodesFor(__block).listIterator();

        while (this.___iterator.hasNext())
        {
            Node __node = this.___iterator.next();
            if (!__node.isAlive())
            {
                continue;
            }
            if (this.___reconnect != null && __node instanceof FixedNode)
            {
                this.___reconnect.setNext((FixedNode) __node);
                this.___reconnect = null;
            }
            if (__node instanceof FixedWithNextNode)
            {
                this.___lastFixed = (FixedWithNextNode) __node;
            }
            processNode(__node);
        }
        if (this.___reconnect != null)
        {
            this.___reconnect.setNext(__block.getFirstSuccessor().getBeginNode());
        }
    }

    protected void insert(FixedNode __start, FixedWithNextNode __end)
    {
        this.___lastFixed.setNext(__start);
        this.___lastFixed = __end;
        this.___reconnect = __end;
    }

    protected void replaceCurrent(FixedWithNextNode __newNode)
    {
        Node __current = this.___iterator.previous();
        this.___iterator.next(); // needed because of the previous() call
        __current.replaceAndDelete(__newNode);
        insert(__newNode, __newNode);
        this.___iterator.set(__newNode);
    }

    protected abstract void processNode(Node __node);
}
