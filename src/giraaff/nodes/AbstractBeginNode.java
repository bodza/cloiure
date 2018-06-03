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
    // @def
    public static final NodeClass<AbstractBeginNode> TYPE = NodeClass.create(AbstractBeginNode.class);

    // @cons
    protected AbstractBeginNode(NodeClass<? extends AbstractBeginNode> __c)
    {
        this(__c, StampFactory.forVoid());
    }

    // @cons
    protected AbstractBeginNode(NodeClass<? extends AbstractBeginNode> __c, Stamp __stamp)
    {
        super(__c, __stamp);
    }

    public static AbstractBeginNode prevBegin(FixedNode __from)
    {
        Node __next = __from;
        while (__next != null)
        {
            if (__next instanceof AbstractBeginNode)
            {
                return (AbstractBeginNode) __next;
            }
            __next = __next.predecessor();
        }
        return null;
    }

    private void evacuateGuards(FixedNode __evacuateFrom)
    {
        if (!hasNoUsages())
        {
            AbstractBeginNode __prevBegin = prevBegin(__evacuateFrom);
            for (Node __anchored : anchored().snapshot())
            {
                __anchored.replaceFirstInput(this, __prevBegin);
            }
        }
    }

    public void prepareDelete()
    {
        prepareDelete((FixedNode) predecessor());
    }

    public void prepareDelete(FixedNode __evacuateFrom)
    {
        evacuateGuards(__evacuateFrom);
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
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
        // @closure
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
    private static final class BlockNodeIterator implements Iterator<FixedNode>
    {
        // @field
        private FixedNode ___current;

        // @cons
        BlockNodeIterator(FixedNode __next)
        {
            super();
            this.___current = __next;
        }

        @Override
        public boolean hasNext()
        {
            return this.___current != null;
        }

        @Override
        public FixedNode next()
        {
            FixedNode __ret = this.___current;
            if (__ret == null)
            {
                throw new NoSuchElementException();
            }
            if (this.___current instanceof FixedWithNextNode)
            {
                this.___current = ((FixedWithNextNode) this.___current).next();
                if (this.___current instanceof AbstractBeginNode)
                {
                    this.___current = null;
                }
            }
            else
            {
                this.___current = null;
            }
            return __ret;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
}
