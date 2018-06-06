package giraaff.graph;

import java.util.ArrayList;
import java.util.Iterator;

import giraaff.core.common.Fields;
import giraaff.core.common.FieldsScanner;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.util.UnsafeAccess;

///
// Describes {@link Node} fields representing the set of inputs for the node or the set of the
// node's successors.
///
// @class Edges
public abstract class Edges extends Fields
{
    ///
    // Constants denoting whether a set of edges are inputs or successors.
    ///
    // @enum Edges.EdgesType
    public enum EdgesType
    {
        Inputs,
        Successors;
    }

    // @field
    private final int ___directCount;
    // @field
    private final Edges.EdgesType ___type;

    // @cons Edges
    public Edges(Edges.EdgesType __type, int __directCount, ArrayList<? extends FieldsScanner.FieldInfo> __edges)
    {
        super(__edges);
        this.___type = __type;
        this.___directCount = __directCount;
    }

    public static void translateInto(Edges __edges, ArrayList<NodeClass.EdgeInfo> __infos)
    {
        for (int __index = 0; __index < __edges.getCount(); __index++)
        {
            __infos.add(new NodeClass.EdgeInfo(__edges.___offsets[__index], __edges.getName(__index), __edges.getType(__index), __edges.getDeclaringClass(__index)));
        }
    }

    public static Node getNodeUnsafe(Node __node, long __offset)
    {
        return (Node) UnsafeAccess.UNSAFE.getObject(__node, __offset);
    }

    @SuppressWarnings("unchecked")
    public static NodeList<Node> getNodeListUnsafe(Node __node, long __offset)
    {
        return (NodeList<Node>) UnsafeAccess.UNSAFE.getObject(__node, __offset);
    }

    public static void putNodeUnsafe(Node __node, long __offset, Node __value)
    {
        UnsafeAccess.UNSAFE.putObject(__node, __offset, __value);
    }

    public static void putNodeListUnsafe(Node __node, long __offset, NodeList<?> __value)
    {
        UnsafeAccess.UNSAFE.putObject(__node, __offset, __value);
    }

    ///
    // Get the number of direct edges represented by this object. A direct edge goes directly to
    // another {@link Node}. An indirect edge goes via a {@link NodeList}.
    ///
    public int getDirectCount()
    {
        return this.___directCount;
    }

    ///
    // Gets the {@link Node} at the end point of a {@linkplain #getDirectCount() direct} edge.
    //
    // @param node one end point of the edge
    // @param index the index of a non-list the edge (must be less than {@link #getDirectCount()})
    // @return the Node at the other edge of the requested edge
    ///
    public static Node getNode(Node __node, long[] __offsets, int __index)
    {
        return getNodeUnsafe(__node, __offsets[__index]);
    }

    ///
    // Gets the {@link NodeList} at the end point of a {@linkplain #getDirectCount() direct} edge.
    //
    // @param node one end point of the edge
    // @param index the index of a non-list the edge (must be equal to or greater than
    //            {@link #getDirectCount()})
    // @return the {@link NodeList} at the other edge of the requested edge
    ///
    public static NodeList<Node> getNodeList(Node __node, long[] __offsets, int __index)
    {
        return getNodeListUnsafe(__node, __offsets[__index]);
    }

    ///
    // Clear edges in a given node. This is accomplished by setting {@linkplain #getDirectCount()
    // direct} edges to null and replacing the lists containing indirect edges with new lists. The
    // latter is important so that this method can be used to clear the edges of cloned nodes.
    //
    // @param node the node whose edges are to be cleared
    ///
    public void clear(Node __node)
    {
        final long[] __curOffsets = this.___offsets;
        final Edges.EdgesType __curType = this.___type;
        int __index = 0;
        int __curDirectCount = getDirectCount();
        while (__index < __curDirectCount)
        {
            initializeNode(__node, __index++, null);
        }
        int __curCount = getCount();
        while (__index < __curCount)
        {
            NodeList<Node> __list = getNodeList(__node, __curOffsets, __index);
            if (__list != null)
            {
                int __size = __list.___initialSize;
                NodeList<Node> __newList = __curType == Edges.EdgesType.Inputs ? new NodeInputList<>(__node, __size) : new NodeSuccessorList<>(__node, __size);

                // replacing with a new list object is the expected behavior!
                initializeList(__node, __index, __newList);
            }
            __index++;
        }
    }

    ///
    // Initializes the list edges in a given node based on the size of the list edges in a prototype node.
    //
    // @param node the node whose list edges are to be initialized
    // @param prototype the node whose list edge sizes are used when creating new edge lists
    ///
    public void initializeLists(Node __node, Node __prototype)
    {
        int __index = getDirectCount();
        final long[] __curOffsets = this.___offsets;
        final Edges.EdgesType __curType = this.___type;
        while (__index < getCount())
        {
            NodeList<Node> __list = getNodeList(__prototype, __curOffsets, __index);
            if (__list != null)
            {
                int __size = __list.___initialSize;
                NodeList<Node> __newList = __curType == Edges.EdgesType.Inputs ? new NodeInputList<>(__node, __size) : new NodeSuccessorList<>(__node, __size);
                initializeList(__node, __index, __newList);
            }
            __index++;
        }
    }

    ///
    // Copies edges from {@code fromNode} to {@code toNode}. The nodes are expected to be of the
    // exact same type.
    //
    // @param fromNode the node from which the edges should be copied.
    // @param toNode the node to which the edges should be copied.
    ///
    public void copy(Node __fromNode, Node __toNode)
    {
        int __index = 0;
        final long[] __curOffsets = this.___offsets;
        final Edges.EdgesType __curType = this.___type;
        int __curDirectCount = getDirectCount();
        while (__index < __curDirectCount)
        {
            initializeNode(__toNode, __index, getNode(__fromNode, __curOffsets, __index));
            __index++;
        }
        int __curCount = getCount();
        while (__index < __curCount)
        {
            NodeList<Node> __list = getNodeList(__toNode, __curOffsets, __index);
            NodeList<Node> __fromList = getNodeList(__fromNode, __curOffsets, __index);
            if (__list == null || __list == __fromList)
            {
                __list = __curType == Edges.EdgesType.Inputs ? new NodeInputList<>(__toNode, __fromList) : new NodeSuccessorList<>(__toNode, __fromList);
                initializeList(__toNode, __index, __list);
            }
            else
            {
                __list.copy(__fromList);
            }
            __index++;
        }
    }

    @Override
    public void set(Object __node, int __index, Object __value)
    {
        throw new IllegalArgumentException("Cannot call set on " + this);
    }

    ///
    // Sets the value of a given edge without notifying the new and old nodes on the other end of
    // the edge of the change.
    //
    // @param node the node whose edge is to be updated
    // @param index the index of the edge (between 0 and {@link #getCount()})
    // @param value the node to be written to the edge
    ///
    public void initializeNode(Node __node, int __index, Node __value)
    {
        verifyUpdateValid(__node, __index, __value);
        putNodeUnsafe(__node, this.___offsets[__index], __value);
    }

    public void initializeList(Node __node, int __index, NodeList<Node> __value)
    {
        verifyUpdateValid(__node, __index, __value);
        putNodeListUnsafe(__node, this.___offsets[__index], __value);
    }

    private void verifyUpdateValid(Node __node, int __index, Object __newValue)
    {
        if (__newValue != null && !getType(__index).isAssignableFrom(__newValue.getClass()))
        {
            throw new IllegalArgumentException("Can not assign " + __newValue.getClass() + " to " + getType(__index) + " in " + __node);
        }
    }

    ///
    // Sets the value of a given edge and notifies the new and old nodes on the other end of the
    // edge of the change.
    //
    // @param node the node whose edge is to be updated
    // @param index the index of the edge (between 0 and {@link #getCount()})
    // @param value the node to be written to the edge
    ///
    public void setNode(Node __node, int __index, Node __value)
    {
        Node __old = getNodeUnsafe(__node, this.___offsets[__index]);
        initializeNode(__node, __index, __value);
        update(__node, __old, __value);
    }

    public abstract void update(Node __node, Node __oldValue, Node __newValue);

    public boolean contains(Node __node, Node __value)
    {
        final long[] __curOffsets = this.___offsets;
        for (int __i = 0; __i < this.___directCount; __i++)
        {
            if (getNode(__node, __curOffsets, __i) == __value)
            {
                return true;
            }
        }
        for (int __i = this.___directCount; __i < getCount(); __i++)
        {
            NodeList<?> __curList = getNodeList(__node, __curOffsets, __i);
            if (__curList != null && __curList.contains(__value))
            {
                return true;
            }
        }
        return false;
    }

    ///
    // An iterator that will iterate over edges.
    //
    // An iterator of this type will not return null values, unless edges are modified concurrently.
    // Concurrent modifications are detected by an assertion on a best-effort basis.
    ///
    // @class Edges.EdgesIterator
    private static final class EdgesIterator implements Iterator<Position>
    {
        // @field
        protected final Node ___node;
        // @field
        protected final Edges ___edges;
        // @field
        protected int ___index;
        // @field
        protected int ___subIndex;
        // @field
        protected boolean ___needsForward;
        // @field
        protected final int ___directCount;
        // @field
        protected final long[] ___offsets;

        ///
        // Creates an iterator that will iterate over some given edges in a given node.
        ///
        // @cons Edges.EdgesIterator
        EdgesIterator(Node __node, Edges __edges)
        {
            super();
            this.___node = __node;
            this.___edges = __edges;
            this.___index = Node.NOT_ITERABLE;
            this.___subIndex = 0;
            this.___needsForward = true;
            this.___directCount = __edges.getDirectCount();
            this.___offsets = __edges.getOffsets();
        }

        void forward()
        {
            this.___needsForward = false;
            if (this.___index < this.___directCount)
            {
                this.___index++;
                if (this.___index < this.___directCount)
                {
                    return;
                }
            }
            else
            {
                this.___subIndex++;
            }
            if (this.___index < this.___edges.getCount())
            {
                forwardNodeList();
            }
        }

        private void forwardNodeList()
        {
            do
            {
                NodeList<?> __list = Edges.getNodeList(this.___node, this.___offsets, this.___index);
                if (__list != null)
                {
                    if (this.___subIndex < __list.size())
                    {
                        return;
                    }
                }
                this.___subIndex = 0;
                this.___index++;
            } while (this.___index < this.___edges.getCount());
        }

        @Override
        public boolean hasNext()
        {
            if (this.___needsForward)
            {
                forward();
            }
            return this.___index < this.___edges.getCount();
        }

        @Override
        public Position next()
        {
            if (this.___needsForward)
            {
                forward();
            }
            this.___needsForward = true;
            if (this.___index < this.___directCount)
            {
                return new Position(this.___edges, this.___index, Node.NOT_ITERABLE);
            }
            else
            {
                return new Position(this.___edges, this.___index, this.___subIndex);
            }
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }

    public Iterable<Position> getPositionsIterable(final Node __node)
    {
        // @closure
        return new Iterable<Position>()
        {
            @Override
            public Iterator<Position> iterator()
            {
                return new Edges.EdgesIterator(__node, Edges.this);
            }
        };
    }

    public Edges.EdgesType type()
    {
        return this.___type;
    }
}
