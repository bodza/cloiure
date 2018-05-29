package giraaff.graph;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import giraaff.graph.iterators.NodeIterable;

// @class NodeBitMap
public final class NodeBitMap extends NodeIdAccessor implements NodeIterable<Node>
{
    private static final int SHIFT = 6;

    private long[] bits;
    private int nodeCount;
    private int counter;

    // @cons
    public NodeBitMap(Graph graph)
    {
        super(graph);
        this.nodeCount = graph.nodeIdCount();
        this.bits = new long[sizeForNodeCount(nodeCount)];
    }

    private static int sizeForNodeCount(int nodeCount)
    {
        return (nodeCount + Long.SIZE - 1) >> SHIFT;
    }

    public int getCounter()
    {
        return counter;
    }

    // @cons
    private NodeBitMap(NodeBitMap other)
    {
        super(other.graph);
        this.bits = other.bits.clone();
        this.nodeCount = other.nodeCount;
    }

    public Graph graph()
    {
        return graph;
    }

    public boolean isNew(Node node)
    {
        return getNodeId(node) >= nodeCount;
    }

    public boolean isMarked(Node node)
    {
        return isMarked(getNodeId(node));
    }

    public boolean checkAndMarkInc(Node node)
    {
        if (!isMarked(node))
        {
            this.counter++;
            this.mark(node);
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean isMarked(int id)
    {
        return (bits[id >> SHIFT] & (1L << id)) != 0;
    }

    public boolean isMarkedAndGrow(Node node)
    {
        int id = getNodeId(node);
        checkGrow(id);
        return isMarked(id);
    }

    public void mark(Node node)
    {
        int id = getNodeId(node);
        bits[id >> SHIFT] |= (1L << id);
    }

    public void markAndGrow(Node node)
    {
        int id = getNodeId(node);
        checkGrow(id);
        bits[id >> SHIFT] |= (1L << id);
    }

    public void clear(Node node)
    {
        int id = getNodeId(node);
        bits[id >> SHIFT] &= ~(1L << id);
    }

    public void clearAndGrow(Node node)
    {
        int id = getNodeId(node);
        checkGrow(id);
        bits[id >> SHIFT] &= ~(1L << id);
    }

    private void checkGrow(int id)
    {
        if (id >= nodeCount)
        {
            if ((id >> SHIFT) >= bits.length)
            {
                grow();
            }
            else
            {
                nodeCount = id + 1;
            }
        }
    }

    public void clearAll()
    {
        Arrays.fill(bits, 0);
    }

    public void intersect(NodeBitMap other)
    {
        int commonLength = Math.min(bits.length, other.bits.length);
        for (int i = commonLength; i < bits.length; i++)
        {
            bits[i] = 0;
        }
        for (int i = 0; i < commonLength; i++)
        {
            bits[i] &= other.bits[i];
        }
    }

    public void subtract(NodeBitMap other)
    {
        int commonLength = Math.min(bits.length, other.bits.length);
        for (int i = 0; i < commonLength; i++)
        {
            bits[i] &= ~other.bits[i];
        }
    }

    public void union(NodeBitMap other)
    {
        grow();
        if (bits.length < other.bits.length)
        {
            bits = Arrays.copyOf(bits, other.bits.length);
        }
        for (int i = 0; i < Math.min(bits.length, other.bits.length); i++)
        {
            bits[i] |= other.bits[i];
        }
    }

    public void invert()
    {
        for (int i = 0; i < bits.length; i++)
        {
            bits[i] = ~bits[i];
        }
    }

    public void grow()
    {
        nodeCount = Math.max(nodeCount, graph().nodeIdCount());
        int newLength = sizeForNodeCount(nodeCount);
        if (newLength > bits.length)
        {
            newLength = Math.max(newLength, (bits.length * 3 / 2) + 1);
            bits = Arrays.copyOf(bits, newLength);
        }
    }

    public <T extends Node> void markAll(Iterable<T> nodes)
    {
        for (Node node : nodes)
        {
            mark(node);
        }
    }

    protected Node nextMarkedNode(int fromNodeId)
    {
        int wordIndex = fromNodeId >> SHIFT;
        int wordsInUse = bits.length;
        if (wordIndex < wordsInUse)
        {
            long word = getPartOfWord(bits[wordIndex], fromNodeId);
            while (true)
            {
                while (word != 0)
                {
                    int bitIndex = Long.numberOfTrailingZeros(word);
                    int nodeId = wordIndex * Long.SIZE + bitIndex;
                    Node result = graph.getNode(nodeId);
                    if (result == null)
                    {
                        // node was deleted -> clear the bit and continue searching
                        bits[wordIndex] = bits[wordIndex] & ~(1L << bitIndex);
                        int nextNodeId = nodeId + 1;
                        if ((nextNodeId & (Long.SIZE - 1)) == 0)
                        {
                            // we reached the end of this word
                            break;
                        }
                        else
                        {
                            word = getPartOfWord(word, nextNodeId);
                        }
                    }
                    else
                    {
                        return result;
                    }
                }
                if (++wordIndex == wordsInUse)
                {
                    break;
                }
                word = bits[wordIndex];
            }
        }
        return null;
    }

    private static long getPartOfWord(long word, int firstNodeIdToInclude)
    {
        return word & (0xFFFFFFFFFFFFFFFFL << firstNodeIdToInclude);
    }

    /**
     * This iterator only returns nodes that are marked in the {@link NodeBitMap} and are alive in
     * the corresponding {@link Graph}.
     */
    // @class NodeBitMap.MarkedNodeIterator
    private final class MarkedNodeIterator implements Iterator<Node>
    {
        private int currentNodeId;
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
                currentNodeId = getNodeId(currentNode);
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

            Node result = currentNode;
            currentNode = null;
            return result;
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
        int count = 0;
        for (long l : bits)
        {
            count += Long.bitCount(l);
        }
        return count;
    }

    @Override
    public boolean contains(Node node)
    {
        return isMarked(node);
    }

    @Override
    public String toString()
    {
        return snapshot().toString();
    }
}
