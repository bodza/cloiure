package giraaff.graph;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;

import giraaff.graph.iterators.NodeIterable;

// @class NodeList
public abstract class NodeList<T extends Node> extends AbstractList<T> implements NodeIterable<T>, RandomAccess
{
    // @def
    protected static final Node[] EMPTY_NODE_ARRAY = new Node[0];

    // @field
    protected final Node self;
    // @field
    protected Node[] nodes;
    // @field
    private int size;
    // @field
    protected final int initialSize;

    // @cons
    protected NodeList(Node __self)
    {
        super();
        this.self = __self;
        this.nodes = EMPTY_NODE_ARRAY;
        this.initialSize = 0;
    }

    // @cons
    protected NodeList(Node __self, int __initialSize)
    {
        super();
        this.self = __self;
        this.size = __initialSize;
        this.initialSize = __initialSize;
        this.nodes = new Node[__initialSize];
    }

    // @cons
    protected NodeList(Node __self, T[] __elements)
    {
        super();
        this.self = __self;
        if (__elements == null || __elements.length == 0)
        {
            this.size = 0;
            this.nodes = EMPTY_NODE_ARRAY;
            this.initialSize = 0;
        }
        else
        {
            this.size = __elements.length;
            this.initialSize = __elements.length;
            this.nodes = new Node[__elements.length];
            for (int __i = 0; __i < __elements.length; __i++)
            {
                this.nodes[__i] = __elements[__i];
            }
        }
    }

    // @cons
    protected NodeList(Node __self, List<? extends T> __elements)
    {
        super();
        this.self = __self;
        if (__elements == null || __elements.isEmpty())
        {
            this.size = 0;
            this.nodes = EMPTY_NODE_ARRAY;
            this.initialSize = 0;
        }
        else
        {
            this.size = __elements.size();
            this.initialSize = __elements.size();
            this.nodes = new Node[__elements.size()];
            for (int __i = 0; __i < __elements.size(); __i++)
            {
                this.nodes[__i] = __elements.get(__i);
            }
        }
    }

    // @cons
    protected NodeList(Node __self, Collection<? extends NodeInterface> __elements)
    {
        super();
        this.self = __self;
        if (__elements == null || __elements.isEmpty())
        {
            this.size = 0;
            this.nodes = EMPTY_NODE_ARRAY;
            this.initialSize = 0;
        }
        else
        {
            this.size = __elements.size();
            this.initialSize = __elements.size();
            this.nodes = new Node[__elements.size()];
            int __i = 0;
            for (NodeInterface __n : __elements)
            {
                this.nodes[__i] = __n.asNode();
                __i++;
            }
        }
    }

    public boolean isList()
    {
        return true;
    }

    protected abstract void update(T oldNode, T newNode);

    public abstract Edges.Type getEdgesType();

    @Override
    public final int size()
    {
        return size;
    }

    @Override
    public final boolean isEmpty()
    {
        return size == 0;
    }

    @Override
    public boolean isNotEmpty()
    {
        return size > 0;
    }

    @Override
    public int count()
    {
        return size;
    }

    protected final void incModCount()
    {
        modCount++;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean add(Node __node)
    {
        self.incModCount();
        incModCount();
        int __length = nodes.length;
        if (__length == 0)
        {
            nodes = new Node[2];
        }
        else if (size == __length)
        {
            Node[] __newNodes = new Node[nodes.length * 2 + 1];
            System.arraycopy(nodes, 0, __newNodes, 0, __length);
            nodes = __newNodes;
        }
        nodes[size++] = __node;
        update(null, (T) __node);
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get(int __index)
    {
        return (T) nodes[__index];
    }

    private boolean assertInRange(int __index)
    {
        return true;
    }

    public T last()
    {
        return get(size() - 1);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T set(int __index, Node __node)
    {
        incModCount();
        T __oldValue = (T) nodes[__index];
        update((T) nodes[__index], (T) __node);
        nodes[__index] = __node;
        return __oldValue;
    }

    public void initialize(int __index, Node __node)
    {
        incModCount();
        nodes[__index] = __node;
    }

    void copy(NodeList<? extends Node> __other)
    {
        self.incModCount();
        incModCount();
        Node[] __newNodes = new Node[__other.size];
        System.arraycopy(__other.nodes, 0, __newNodes, 0, __newNodes.length);
        nodes = __newNodes;
        size = __other.size;
    }

    public boolean equals(NodeList<T> __other)
    {
        if (size != __other.size)
        {
            return false;
        }
        for (int __i = 0; __i < size; __i++)
        {
            if (nodes[__i] != __other.nodes[__i])
            {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void clear()
    {
        self.incModCount();
        incModCount();
        for (int __i = 0; __i < size; __i++)
        {
            update((T) nodes[__i], null);
        }
        clearWithoutUpdate();
    }

    void clearWithoutUpdate()
    {
        nodes = EMPTY_NODE_ARRAY;
        size = 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean remove(Object __node)
    {
        self.incModCount();
        int __i = 0;
        incModCount();
        while (__i < size && nodes[__i] != __node)
        {
            __i++;
        }
        if (__i < size)
        {
            T __oldValue = (T) nodes[__i];
            __i++;
            while (__i < size)
            {
                nodes[__i - 1] = nodes[__i];
                __i++;
            }
            nodes[--size] = null;
            update(__oldValue, null);
            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public T remove(int __index)
    {
        self.incModCount();
        T __oldValue = (T) nodes[__index];
        int __i = __index + 1;
        incModCount();
        while (__i < size)
        {
            nodes[__i - 1] = nodes[__i];
            __i++;
        }
        nodes[--size] = null;
        update(__oldValue, null);
        return __oldValue;
    }

    boolean replaceFirst(Node __node, Node __other)
    {
        for (int __i = 0; __i < size; __i++)
        {
            if (nodes[__i] == __node)
            {
                nodes[__i] = __other;
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<T> iterator()
    {
        return new NodeListIterator<>(this, 0);
    }

    @Override
    public boolean contains(T __other)
    {
        for (int __i = 0; __i < size; __i++)
        {
            if (nodes[__i] == __other)
            {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<T> snapshot()
    {
        return (List<T>) Arrays.asList(Arrays.copyOf(this.nodes, this.size));
    }

    @Override
    public void snapshotTo(Collection<? super T> __to)
    {
        for (int __i = 0; __i < size; __i++)
        {
            __to.add(get(__i));
        }
    }

    @SuppressWarnings("unchecked")
    public void setAll(NodeList<T> __values)
    {
        self.incModCount();
        incModCount();
        for (int __i = 0; __i < size(); __i++)
        {
            update((T) nodes[__i], null);
        }
        nodes = Arrays.copyOf(__values.nodes, __values.size());
        size = __values.size();

        for (int __i = 0; __i < size(); __i++)
        {
            update(null, (T) nodes[__i]);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A> A[] toArray(A[] __a)
    {
        if (__a.length >= size)
        {
            System.arraycopy(nodes, 0, __a, 0, size);
            return __a;
        }
        return (A[]) Arrays.copyOf(nodes, size, __a.getClass());
    }

    @Override
    public Object[] toArray()
    {
        return Arrays.copyOf(nodes, size);
    }

    protected void replace(T __node, T __other)
    {
        incModCount();
        for (int __i = 0; __i < size(); __i++)
        {
            if (nodes[__i] == __node)
            {
                nodes[__i] = __other;
                update(__node, __other);
            }
        }
    }

    @Override
    public int indexOf(Object __node)
    {
        for (int __i = 0; __i < size; __i++)
        {
            if (nodes[__i] == __node)
            {
                return __i;
            }
        }
        return -1;
    }

    @Override
    public boolean contains(Object __o)
    {
        return indexOf(__o) != -1;
    }

    @Override
    public boolean containsAll(Collection<?> __c)
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean addAll(Collection<? extends T> __c)
    {
        for (T __e : __c)
        {
            add(__e);
        }
        return true;
    }

    public boolean addAll(T[] __c)
    {
        for (T __e : __c)
        {
            add(__e);
        }
        return true;
    }

    @Override
    public T first()
    {
        if (size() > 0)
        {
            return get(0);
        }
        return null;
    }

    public SubList<T> subList(int __startIndex)
    {
        return new SubList<>(this, __startIndex);
    }

    // @class NodeList.SubList
    public static final class SubList<R extends Node> extends AbstractList<R> implements NodeIterable<R>, RandomAccess
    {
        // @field
        private final NodeList<R> list;
        // @field
        private final int offset;

        // @cons
        private SubList(NodeList<R> __list, int __offset)
        {
            super();
            this.list = __list;
            this.offset = __offset;
        }

        @Override
        public R get(int __index)
        {
            return list.get(offset + __index);
        }

        @Override
        public int size()
        {
            return list.size() - offset;
        }

        public SubList<R> subList(int __startIndex)
        {
            return new SubList<>(this.list, __startIndex + offset);
        }

        @Override
        public Iterator<R> iterator()
        {
            return new NodeListIterator<>(list, offset);
        }
    }

    // @class NodeList.NodeListIterator
    private static final class NodeListIterator<R extends Node> implements Iterator<R>
    {
        // @field
        private final NodeList<R> list;
        // @field
        private final int expectedModCount;
        // @field
        private int index;

        // @cons
        private NodeListIterator(NodeList<R> __list, int __startIndex)
        {
            super();
            this.list = __list;
            this.expectedModCount = __list.modCount;
            this.index = __startIndex;
        }

        @Override
        public boolean hasNext()
        {
            return index < list.size;
        }

        @SuppressWarnings("unchecked")
        @Override
        public R next()
        {
            return (R) list.nodes[index++];
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
}
