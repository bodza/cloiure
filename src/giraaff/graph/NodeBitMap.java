package giraaff.graph;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import giraaff.graph.iterators.NodeIterable;

// @class NodeBitMap
public final class NodeBitMap extends NodeIdAccessor implements NodeIterable<Node>
{
    // @def
    private static final int SHIFT = 6;

    // @field
    private long[] ___bits;
    // @field
    private int ___nodeCount;
    // @field
    private int ___counter;

    // @cons
    public NodeBitMap(Graph __graph)
    {
        super(__graph);
        this.___nodeCount = __graph.nodeIdCount();
        this.___bits = new long[sizeForNodeCount(this.___nodeCount)];
    }

    private static int sizeForNodeCount(int __nodeCount)
    {
        return (__nodeCount + Long.SIZE - 1) >> SHIFT;
    }

    public int getCounter()
    {
        return this.___counter;
    }

    // @cons
    private NodeBitMap(NodeBitMap __other)
    {
        super(__other.___graph);
        this.___bits = __other.___bits.clone();
        this.___nodeCount = __other.___nodeCount;
    }

    public Graph graph()
    {
        return this.___graph;
    }

    public boolean isNew(Node __node)
    {
        return getNodeId(__node) >= this.___nodeCount;
    }

    public boolean isMarked(Node __node)
    {
        return isMarked(getNodeId(__node));
    }

    public boolean checkAndMarkInc(Node __node)
    {
        if (!isMarked(__node))
        {
            this.___counter++;
            this.mark(__node);
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean isMarked(int __id)
    {
        return (this.___bits[__id >> SHIFT] & (1L << __id)) != 0;
    }

    public boolean isMarkedAndGrow(Node __node)
    {
        int __id = getNodeId(__node);
        checkGrow(__id);
        return isMarked(__id);
    }

    public void mark(Node __node)
    {
        int __id = getNodeId(__node);
        this.___bits[__id >> SHIFT] |= (1L << __id);
    }

    public void markAndGrow(Node __node)
    {
        int __id = getNodeId(__node);
        checkGrow(__id);
        this.___bits[__id >> SHIFT] |= (1L << __id);
    }

    public void clear(Node __node)
    {
        int __id = getNodeId(__node);
        this.___bits[__id >> SHIFT] &= ~(1L << __id);
    }

    public void clearAndGrow(Node __node)
    {
        int __id = getNodeId(__node);
        checkGrow(__id);
        this.___bits[__id >> SHIFT] &= ~(1L << __id);
    }

    private void checkGrow(int __id)
    {
        if (__id >= this.___nodeCount)
        {
            if ((__id >> SHIFT) >= this.___bits.length)
            {
                grow();
            }
            else
            {
                this.___nodeCount = __id + 1;
            }
        }
    }

    public void clearAll()
    {
        Arrays.fill(this.___bits, 0);
    }

    public void intersect(NodeBitMap __other)
    {
        int __commonLength = Math.min(this.___bits.length, __other.___bits.length);
        for (int __i = __commonLength; __i < this.___bits.length; __i++)
        {
            this.___bits[__i] = 0;
        }
        for (int __i = 0; __i < __commonLength; __i++)
        {
            this.___bits[__i] &= __other.___bits[__i];
        }
    }

    public void subtract(NodeBitMap __other)
    {
        int __commonLength = Math.min(this.___bits.length, __other.___bits.length);
        for (int __i = 0; __i < __commonLength; __i++)
        {
            this.___bits[__i] &= ~__other.___bits[__i];
        }
    }

    public void union(NodeBitMap __other)
    {
        grow();
        if (this.___bits.length < __other.___bits.length)
        {
            this.___bits = Arrays.copyOf(this.___bits, __other.___bits.length);
        }
        for (int __i = 0; __i < Math.min(this.___bits.length, __other.___bits.length); __i++)
        {
            this.___bits[__i] |= __other.___bits[__i];
        }
    }

    public void invert()
    {
        for (int __i = 0; __i < this.___bits.length; __i++)
        {
            this.___bits[__i] = ~this.___bits[__i];
        }
    }

    public void grow()
    {
        this.___nodeCount = Math.max(this.___nodeCount, graph().nodeIdCount());
        int __newLength = sizeForNodeCount(this.___nodeCount);
        if (__newLength > this.___bits.length)
        {
            __newLength = Math.max(__newLength, (this.___bits.length * 3 / 2) + 1);
            this.___bits = Arrays.copyOf(this.___bits, __newLength);
        }
    }

    public <T extends Node> void markAll(Iterable<T> __nodes)
    {
        for (Node __node : __nodes)
        {
            mark(__node);
        }
    }

    protected Node nextMarkedNode(int __fromNodeId)
    {
        int __wordIndex = __fromNodeId >> SHIFT;
        int __wordsInUse = this.___bits.length;
        if (__wordIndex < __wordsInUse)
        {
            long __word = getPartOfWord(this.___bits[__wordIndex], __fromNodeId);
            while (true)
            {
                while (__word != 0)
                {
                    int __bitIndex = Long.numberOfTrailingZeros(__word);
                    int __nodeId = __wordIndex * Long.SIZE + __bitIndex;
                    Node __result = this.___graph.getNode(__nodeId);
                    if (__result == null)
                    {
                        // node was deleted -> clear the bit and continue searching
                        this.___bits[__wordIndex] = this.___bits[__wordIndex] & ~(1L << __bitIndex);
                        int __nextNodeId = __nodeId + 1;
                        if ((__nextNodeId & (Long.SIZE - 1)) == 0)
                        {
                            // we reached the end of this word
                            break;
                        }
                        else
                        {
                            __word = getPartOfWord(__word, __nextNodeId);
                        }
                    }
                    else
                    {
                        return __result;
                    }
                }
                if (++__wordIndex == __wordsInUse)
                {
                    break;
                }
                __word = this.___bits[__wordIndex];
            }
        }
        return null;
    }

    private static long getPartOfWord(long __word, int __firstNodeIdToInclude)
    {
        return __word & (0xFFFFFFFFFFFFFFFFL << __firstNodeIdToInclude);
    }

    ///
    // This iterator only returns nodes that are marked in the {@link NodeBitMap} and are alive
    // in the corresponding {@link Graph}.
    ///
    // @class NodeBitMap.MarkedNodeIterator
    // @closure
    private final class MarkedNodeIterator implements Iterator<Node>
    {
        // @field
        private int ___currentNodeId;
        // @field
        private Node ___currentNode;

        // @cons
        MarkedNodeIterator()
        {
            super();
            this.___currentNodeId = -1;
            forward();
        }

        private void forward()
        {
            this.___currentNode = NodeBitMap.this.nextMarkedNode(this.___currentNodeId + 1);
            if (this.___currentNode != null)
            {
                this.___currentNodeId = NodeBitMap.this.getNodeId(this.___currentNode);
            }
            else
            {
                this.___currentNodeId = -1;
            }
        }

        @Override
        public boolean hasNext()
        {
            if (this.___currentNode == null && this.___currentNodeId >= 0)
            {
                forward();
            }
            return this.___currentNodeId >= 0;
        }

        @Override
        public Node next()
        {
            if (!hasNext())
            {
                throw new NoSuchElementException();
            }
            if (!this.___currentNode.isAlive())
            {
                throw new ConcurrentModificationException("NodeBitMap was modified between the calls to hasNext() and next()");
            }

            Node __result = this.___currentNode;
            this.___currentNode = null;
            return __result;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Iterator<Node> iterator()
    {
        return new MarkedNodeIterator();
    }

    public NodeBitMap copy()
    {
        return new NodeBitMap(this);
    }

    @Override
    public int count()
    {
        int __count = 0;
        for (long __l : this.___bits)
        {
            __count += Long.bitCount(__l);
        }
        return __count;
    }

    @Override
    public boolean contains(Node __node)
    {
        return isMarked(__node);
    }
}
