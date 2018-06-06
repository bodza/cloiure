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
    protected final Node ___self;
    // @field
    protected Node[] ___nodes;
    // @field
    private int ___size;
    // @field
    protected final int ___initialSize;

    // @cons NodeList
    protected NodeList(Node __self)
    {
        super();
        this.___self = __self;
        this.___nodes = EMPTY_NODE_ARRAY;
        this.___initialSize = 0;
    }

    // @cons NodeList
    protected NodeList(Node __self, int __initialSize)
    {
        super();
        this.___self = __self;
        this.___size = __initialSize;
        this.___initialSize = __initialSize;
        this.___nodes = new Node[__initialSize];
    }

    // @cons NodeList
    protected NodeList(Node __self, T[] __elements)
    {
        super();
        this.___self = __self;
        if (__elements == null || __elements.length == 0)
        {
            this.___size = 0;
            this.___nodes = EMPTY_NODE_ARRAY;
            this.___initialSize = 0;
        }
        else
        {
            this.___size = __elements.length;
            this.___initialSize = __elements.length;
            this.___nodes = new Node[__elements.length];
            for (int __i = 0; __i < __elements.length; __i++)
            {
                this.___nodes[__i] = __elements[__i];
            }
        }
    }

    // @cons NodeList
    protected NodeList(Node __self, List<? extends T> __elements)
    {
        super();
        this.___self = __self;
        if (__elements == null || __elements.isEmpty())
        {
            this.___size = 0;
            this.___nodes = EMPTY_NODE_ARRAY;
            this.___initialSize = 0;
        }
        else
        {
            this.___size = __elements.size();
            this.___initialSize = __elements.size();
            this.___nodes = new Node[__elements.size()];
            for (int __i = 0; __i < __elements.size(); __i++)
            {
                this.___nodes[__i] = __elements.get(__i);
            }
        }
    }

    // @cons NodeList
    protected NodeList(Node __self, Collection<? extends NodeInterface> __elements)
    {
        super();
        this.___self = __self;
        if (__elements == null || __elements.isEmpty())
        {
            this.___size = 0;
            this.___nodes = EMPTY_NODE_ARRAY;
            this.___initialSize = 0;
        }
        else
        {
            this.___size = __elements.size();
            this.___initialSize = __elements.size();
            this.___nodes = new Node[__elements.size()];
            int __i = 0;
            for (NodeInterface __n : __elements)
            {
                this.___nodes[__i] = __n.asNode();
                __i++;
            }
        }
    }

    public boolean isList()
    {
        return true;
    }

    protected abstract void update(T __oldNode, T __newNode);

    public abstract Edges.EdgesType getEdgesType();

    @Override
    public final int size()
    {
        return this.___size;
    }

    @Override
    public final boolean isEmpty()
    {
        return this.___size == 0;
    }

    @Override
    public boolean isNotEmpty()
    {
        return this.___size > 0;
    }

    @Override
    public int count()
    {
        return this.___size;
    }

    protected final void incModCount()
    {
        modCount++;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean add(Node __node)
    {
        this.___self.incModCount();
        incModCount();
        int __length = this.___nodes.length;
        if (__length == 0)
        {
            this.___nodes = new Node[2];
        }
        else if (this.___size == __length)
        {
            Node[] __newNodes = new Node[this.___nodes.length * 2 + 1];
            System.arraycopy(this.___nodes, 0, __newNodes, 0, __length);
            this.___nodes = __newNodes;
        }
        this.___nodes[this.___size++] = __node;
        update(null, (T) __node);
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get(int __index)
    {
        return (T) this.___nodes[__index];
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
        T __oldValue = (T) this.___nodes[__index];
        update((T) this.___nodes[__index], (T) __node);
        this.___nodes[__index] = __node;
        return __oldValue;
    }

    public void initialize(int __index, Node __node)
    {
        incModCount();
        this.___nodes[__index] = __node;
    }

    void copy(NodeList<? extends Node> __other)
    {
        this.___self.incModCount();
        incModCount();
        Node[] __newNodes = new Node[__other.___size];
        System.arraycopy(__other.___nodes, 0, __newNodes, 0, __newNodes.length);
        this.___nodes = __newNodes;
        this.___size = __other.___size;
    }

    public boolean equals(NodeList<T> __other)
    {
        if (this.___size != __other.___size)
        {
            return false;
        }
        for (int __i = 0; __i < this.___size; __i++)
        {
            if (this.___nodes[__i] != __other.___nodes[__i])
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
        this.___self.incModCount();
        incModCount();
        for (int __i = 0; __i < this.___size; __i++)
        {
            update((T) this.___nodes[__i], null);
        }
        clearWithoutUpdate();
    }

    void clearWithoutUpdate()
    {
        this.___nodes = EMPTY_NODE_ARRAY;
        this.___size = 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean remove(Object __node)
    {
        this.___self.incModCount();
        int __i = 0;
        incModCount();
        while (__i < this.___size && this.___nodes[__i] != __node)
        {
            __i++;
        }
        if (__i < this.___size)
        {
            T __oldValue = (T) this.___nodes[__i];
            __i++;
            while (__i < this.___size)
            {
                this.___nodes[__i - 1] = this.___nodes[__i];
                __i++;
            }
            this.___nodes[--this.___size] = null;
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
        this.___self.incModCount();
        T __oldValue = (T) this.___nodes[__index];
        int __i = __index + 1;
        incModCount();
        while (__i < this.___size)
        {
            this.___nodes[__i - 1] = this.___nodes[__i];
            __i++;
        }
        this.___nodes[--this.___size] = null;
        update(__oldValue, null);
        return __oldValue;
    }

    boolean replaceFirst(Node __node, Node __other)
    {
        for (int __i = 0; __i < this.___size; __i++)
        {
            if (this.___nodes[__i] == __node)
            {
                this.___nodes[__i] = __other;
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<T> iterator()
    {
        return new NodeList.NodeListIterator<>(this, 0);
    }

    @Override
    public boolean contains(T __other)
    {
        for (int __i = 0; __i < this.___size; __i++)
        {
            if (this.___nodes[__i] == __other)
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
        return (List<T>) Arrays.asList(Arrays.copyOf(this.___nodes, this.___size));
    }

    @Override
    public void snapshotTo(Collection<? super T> __to)
    {
        for (int __i = 0; __i < this.___size; __i++)
        {
            __to.add(get(__i));
        }
    }

    @SuppressWarnings("unchecked")
    public void setAll(NodeList<T> __values)
    {
        this.___self.incModCount();
        incModCount();
        for (int __i = 0; __i < size(); __i++)
        {
            update((T) this.___nodes[__i], null);
        }
        this.___nodes = Arrays.copyOf(__values.___nodes, __values.size());
        this.___size = __values.size();

        for (int __i = 0; __i < size(); __i++)
        {
            update(null, (T) this.___nodes[__i]);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A> A[] toArray(A[] __a)
    {
        if (__a.length >= this.___size)
        {
            System.arraycopy(this.___nodes, 0, __a, 0, this.___size);
            return __a;
        }
        return (A[]) Arrays.copyOf(this.___nodes, this.___size, __a.getClass());
    }

    @Override
    public Object[] toArray()
    {
        return Arrays.copyOf(this.___nodes, this.___size);
    }

    protected void replace(T __node, T __other)
    {
        incModCount();
        for (int __i = 0; __i < size(); __i++)
        {
            if (this.___nodes[__i] == __node)
            {
                this.___nodes[__i] = __other;
                update(__node, __other);
            }
        }
    }

    @Override
    public int indexOf(Object __node)
    {
        for (int __i = 0; __i < this.___size; __i++)
        {
            if (this.___nodes[__i] == __node)
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

    public NodeList.SubList<T> subList(int __startIndex)
    {
        return new NodeList.SubList<>(this, __startIndex);
    }

    // @class NodeList.SubList
    public static final class SubList<R extends Node> extends AbstractList<R> implements NodeIterable<R>, RandomAccess
    {
        // @field
        private final NodeList<R> ___list;
        // @field
        private final int ___offset;

        // @cons NodeList.SubList
        private SubList(NodeList<R> __list, int __offset)
        {
            super();
            this.___list = __list;
            this.___offset = __offset;
        }

        @Override
        public R get(int __index)
        {
            return this.___list.get(this.___offset + __index);
        }

        @Override
        public int size()
        {
            return this.___list.size() - this.___offset;
        }

        public NodeList.SubList<R> subList(int __startIndex)
        {
            return new NodeList.SubList<>(this.___list, __startIndex + this.___offset);
        }

        @Override
        public Iterator<R> iterator()
        {
            return new NodeList.NodeListIterator<>(this.___list, this.___offset);
        }
    }

    // @class NodeList.NodeListIterator
    private static final class NodeListIterator<R extends Node> implements Iterator<R>
    {
        // @field
        private final NodeList<R> ___list;
        // @field
        private final int ___expectedModCount;
        // @field
        private int ___index;

        // @cons NodeList.NodeListIterator
        private NodeListIterator(NodeList<R> __list, int __startIndex)
        {
            super();
            this.___list = __list;
            this.___expectedModCount = __list.modCount;
            this.___index = __startIndex;
        }

        @Override
        public boolean hasNext()
        {
            return this.___index < this.___list.___size;
        }

        @SuppressWarnings("unchecked")
        @Override
        public R next()
        {
            return (R) this.___list.___nodes[this.___index++];
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
}
