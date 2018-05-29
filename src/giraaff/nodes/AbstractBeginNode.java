package giraaff.nodes;

import java.util.Iterator;
import java.util.NoSuchElementException;

import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.IterableNodeType;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.iterators.NodeIterable;
import giraaff.nodes.extended.AnchoringNode;
import giraaff.nodes.extended.GuardingNode;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @NodeInfo.allowedUsageTypes "Guard, Anchor"
// @class AbstractBeginNode
public abstract class AbstractBeginNode extends FixedWithNextNode implements LIRLowerable, GuardingNode, AnchoringNode, IterableNodeType
{
    public static final NodeClass<AbstractBeginNode> TYPE = NodeClass.create(AbstractBeginNode.class);

    // @cons
    protected AbstractBeginNode(NodeClass<? extends AbstractBeginNode> c)
    {
        this(c, StampFactory.forVoid());
    }

    // @cons
    protected AbstractBeginNode(NodeClass<? extends AbstractBeginNode> c, Stamp stamp)
    {
        super(c, stamp);
    }

    public static AbstractBeginNode prevBegin(FixedNode from)
    {
        Node next = from;
        while (next != null)
        {
            if (next instanceof AbstractBeginNode)
            {
                return (AbstractBeginNode) next;
            }
            next = next.predecessor();
        }
        return null;
    }

    private void evacuateGuards(FixedNode evacuateFrom)
    {
        if (!hasNoUsages())
        {
            AbstractBeginNode prevBegin = prevBegin(evacuateFrom);
            for (Node anchored : anchored().snapshot())
            {
                anchored.replaceFirstInput(this, prevBegin);
            }
        }
    }

    public void prepareDelete()
    {
        prepareDelete((FixedNode) predecessor());
    }

    public void prepareDelete(FixedNode evacuateFrom)
    {
        evacuateGuards(evacuateFrom);
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        // nop
    }

    public NodeIterable<GuardNode> guards()
    {
        return usages().filter(GuardNode.class);
    }

    public NodeIterable<Node> anchored()
    {
        return usages();
    }

    public NodeIterable<FixedNode> getBlockNodes()
    {
        return new NodeIterable<FixedNode>()
        {
            @Override
            public Iterator<FixedNode> iterator()
            {
                return new BlockNodeIterator(AbstractBeginNode.this);
            }
        };
    }

    // @class AbstractBeginNode.BlockNodeIterator
    private final class BlockNodeIterator implements Iterator<FixedNode>
    {
        private FixedNode current;

        // @cons
        BlockNodeIterator(FixedNode next)
        {
            super();
            this.current = next;
        }

        @Override
        public boolean hasNext()
        {
            return current != null;
        }

        @Override
        public FixedNode next()
        {
            FixedNode ret = current;
            if (ret == null)
            {
                throw new NoSuchElementException();
            }
            if (current instanceof FixedWithNextNode)
            {
                current = ((FixedWithNextNode) current).next();
                if (current instanceof AbstractBeginNode)
                {
                    current = null;
                }
            }
            else
            {
                current = null;
            }
            return ret;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
}
