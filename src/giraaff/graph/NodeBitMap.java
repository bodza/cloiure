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
    private long[] bits;
    // @field
    private int nodeCount;
    // @field
    private int counter;

    // @cons
    public NodeBitMap(Graph __graph)
    {
        super(__graph);
        this.nodeCount = __graph.nodeIdCount();
        this.bits = new long[sizeForNodeCount(nodeCount)];
    }

    private static int sizeForNodeCount(int __nodeCount)
    {
        return (__nodeCount + Long.SIZE - 1) >> SHIFT;
    }

    public int getCounter()
    {
        return counter;
    }

    // @cons
    private NodeBitMap(NodeBitMap __other)
    {
        super(__other.graph);
        this.bits = __other.bits.clone();
        this.nodeCount = __other.nodeCount;
    }

    public Graph graph()
    {
        return graph;
    }

    public boolean isNew(Node __node)
    {
        return getNodeId(__node) >= nodeCount;
    }

    public boolean isMarked(Node __node)
    {
        return isMarked(getNodeId(__node));
    }

    public boolean checkAndMarkInc(Node __node)
    {
        if (!isMarked(__node))
        {
            this.counter++;
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
        return (bits[__id >> SHIFT] & (1L << __id)) != 0;
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
        bits[__id >> SHIFT] |= (1L << __id);
    }

    public void markAndGrow(Node __node)
    {
        int __id = getNodeId(__node);
        checkGrow(__id);
        bits[__id >> SHIFT] |= (1L << __id);
    }

    public void clear(Node __node)
    {
        int __id = getNodeId(__node);
        bits[__id >> SHIFT] &= ~(1L << __id);
    }

    public void clearAndGrow(Node __node)
    {
        int __id = getNodeId(__node);
        checkGrow(__id);
        bits[__id >> SHIFT] &= ~(1L << __id);
    }

    private void checkGrow(int __id)
    {
        if (__id >= nodeCount)
        {
            if ((__id >> SHIFT) >= bits.length)
            {
                grow();
            }
            else
            {
                nodeCount = __id + 1;
            }
        }
    }

    public void clearAll()
    {
        Arrays.fill(bits, 0);
    }

    public void intersect(NodeBitMap __other)
    {
        int __commonLength = Math.min(bits.length, __other.bits.length);
        for (int __i = __commonLength; __i < bits.length; __i++)
        {
            bits[__i] = 0;
        }
        for (int __i = 0; __i < __commonLength; __i++)
        {
            bits[__i] &= __other.bits[__i];
        }
    }

    public void subtract(NodeBitMap __other)
    {
        int __commonLength = Math.min(bits.length, __other.bits.length);
        for (int __i = 0; __i < __commonLength; __i++)
        {
            bits[__i] &= ~__other.bits[__i];
        }
    }

    public void union(NodeBitMap __other)
    {
        grow();
        if (bits.length < __other.bits.length)
        {
            bits = Arrays.copyOf(bits, __other.bits.length);
        }
        for (int __i = 0; __i < Math.min(bits.length, __other.bits.length); __i++)
        {
            bits[__i] |= __other.bits[__i];
        }
    }

    public void invert()
    {
        for (int __i = 0; __i < bits.length; __i++)
        {
            bits[__i] = ~bits[__i];
        }
    }

    public void grow()
    {
        nodeCount = Math.max(nodeCount, graph().nodeIdCount());
        int __newLength = sizeForNodeCount(nodeCount);
        if (__newLength > bits.length)
        {
            __newLength = Math.max(__newLength, (bits.length * 3 / 2) + 1);
            bits = Arrays.copyOf(bits, __newLength);
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
        int __wordsInUse = bits.length;
        if (__wordIndex < __wordsInUse)
        {
            long __word = getPartOfWord(bits[__wordIndex], __fromNodeId);
            while (true)
            {
                while (__word != 0)
                {
                    int __bitIndex = Long.numberOfTrailingZeros(__word);
                    int __nodeId = __wordIndex * Long.SIZE + __bitIndex;
                    Node __result = graph.getNode(__nodeId);
                    if (__result == null)
                    {
                        // node was deleted -> clear the bit and continue searching
                        bits[__wordIndex] = bits[__wordIndex] & ~(1L << __bitIndex);
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
                __word = bits[__wordIndex];
            }
        }
        return null;
    }

    private static long getPartOfWord(long __word, int __firstNodeIdToInclude)
    {
        return __word & (0xFFFFFFFFFFFFFFFFL << __firstNodeIdToInclude);
    }

    /**
     * This iterator only returns nodes that are marked in the {@link NodeBitMap} and are alive
     * in the corresponding {@link Graph}.
     */
    // @class NodeBitMap.MarkedNodeIterator
    // @closure
    private final class MarkedNodeIterator implements Iterator<Node>
    {
        // @field
        private int currentNodeId;
        // @field
        private Node currentNode;

        // @cons
        MarkedNodeIterator()
        {
            super();
            currentNodeId = -1;
            forward();
        }

        private void forward()
        {
            currentNode = NodeBitMap.this.nextMarkedNode(currentNodeId + 1);
            if (currentNode != null)
            {
                currentNodeId = NodeBitMap.this.getNodeId(currentNode);
            }
            else
            {
                currentNodeId = -1;
            }
        }

        @Override
        public boolean hasNext()
        {
            if (currentNode == null && currentNodeId >= 0)
            {
                forward();
            }
            return currentNodeId >= 0;
        }

        @Override
        public Node next()
        {
            if (!hasNext())
            {
                throw new NoSuchElementException();
            }
            if (!currentNode.isAlive())
            {
                throw new ConcurrentModificationException("NodeBitMap was modified between the calls to hasNext() and next()");
            }

            Node __result = currentNode;
            currentNode = null;
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
        for (long __l : bits)
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
