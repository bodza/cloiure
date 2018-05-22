package giraaff.graph;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;

import giraaff.graph.iterators.NodeIterable;

public abstract class NodeList<T extends Node> extends AbstractList<T> implements NodeIterable<T>, RandomAccess
{
    protected static final Node[] EMPTY_NODE_ARRAY = new Node[0];

    protected final Node self;
    protected Node[] nodes;
    private int size;
    protected final int initialSize;

    protected NodeList(Node self)
    {
        this.self = self;
        this.nodes = EMPTY_NODE_ARRAY;
        this.initialSize = 0;
    }

    protected NodeList(Node self, int initialSize)
    {
        this.self = self;
        this.size = initialSize;
        this.initialSize = initialSize;
        this.nodes = new Node[initialSize];
    }

    protected NodeList(Node self, T[] elements)
    {
        this.self = self;
        if (elements == null || elements.length == 0)
        {
            this.size = 0;
            this.nodes = EMPTY_NODE_ARRAY;
            this.initialSize = 0;
        }
        else
        {
            this.size = elements.length;
            this.initialSize = elements.length;
            this.nodes = new Node[elements.length];
            for (int i = 0; i < elements.length; i++)
            {
                this.nodes[i] = elements[i];
            }
        }
    }

    protected NodeList(Node self, List<? extends T> elements)
    {
        this.self = self;
        if (elements == null || elements.isEmpty())
        {
            this.size = 0;
            this.nodes = EMPTY_NODE_ARRAY;
            this.initialSize = 0;
        }
        else
        {
            this.size = elements.size();
            this.initialSize = elements.size();
            this.nodes = new Node[elements.size()];
            for (int i = 0; i < elements.size(); i++)
            {
                this.nodes[i] = elements.get(i);
            }
        }
    }

    protected NodeList(Node self, Collection<? extends NodeInterface> elements)
    {
        this.self = self;
        if (elements == null || elements.isEmpty())
        {
            this.size = 0;
            this.nodes = EMPTY_NODE_ARRAY;
            this.initialSize = 0;
        }
        else
        {
            this.size = elements.size();
            this.initialSize = elements.size();
            this.nodes = new Node[elements.size()];
            int i = 0;
            for (NodeInterface n : elements)
            {
                this.nodes[i] = n.asNode();
                i++;
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
    public boolean add(Node node)
    {
        self.incModCount();
        incModCount();
        int length = nodes.length;
        if (length == 0)
        {
            nodes = new Node[2];
        }
        else if (size == length)
        {
            Node[] newNodes = new Node[nodes.length * 2 + 1];
            System.arraycopy(nodes, 0, newNodes, 0, length);
            nodes = newNodes;
        }
        nodes[size++] = node;
        update(null, (T) node);
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get(int index)
    {
        return (T) nodes[index];
    }

    private boolean assertInRange(int index)
    {
        return true;
    }

    public T last()
    {
        return get(size() - 1);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T set(int index, Node node)
    {
        incModCount();
        T oldValue = (T) nodes[index];
        update((T) nodes[index], (T) node);
        nodes[index] = node;
        return oldValue;
    }

    public void initialize(int index, Node node)
    {
        incModCount();
        nodes[index] = node;
    }

    void copy(NodeList<? extends Node> other)
    {
        self.incModCount();
        incModCount();
        Node[] newNodes = new Node[other.size];
        System.arraycopy(other.nodes, 0, newNodes, 0, newNodes.length);
        nodes = newNodes;
        size = other.size;
    }

    public boolean equals(NodeList<T> other)
    {
        if (size != other.size)
        {
            return false;
        }
        for (int i = 0; i < size; i++)
        {
            if (nodes[i] != other.nodes[i])
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
        for (int i = 0; i < size; i++)
        {
            update((T) nodes[i], null);
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
    public boolean remove(Object node)
    {
        self.incModCount();
        int i = 0;
        incModCount();
        while (i < size && nodes[i] != node)
        {
            i++;
        }
        if (i < size)
        {
            T oldValue = (T) nodes[i];
            i++;
            while (i < size)
            {
                nodes[i - 1] = nodes[i];
                i++;
            }
            nodes[--size] = null;
            update(oldValue, null);
            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public T remove(int index)
    {
        self.incModCount();
        T oldValue = (T) nodes[index];
        int i = index + 1;
        incModCount();
        while (i < size)
        {
            nodes[i - 1] = nodes[i];
            i++;
        }
        nodes[--size] = null;
        update(oldValue, null);
        return oldValue;
    }

    boolean replaceFirst(Node node, Node other)
    {
        for (int i = 0; i < size; i++)
        {
            if (nodes[i] == node)
            {
                nodes[i] = other;
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
    public boolean contains(T other)
    {
        for (int i = 0; i < size; i++)
        {
            if (nodes[i] == other)
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
    public void snapshotTo(Collection<? super T> to)
    {
        for (int i = 0; i < size; i++)
        {
            to.add(get(i));
        }
    }

    @SuppressWarnings("unchecked")
    public void setAll(NodeList<T> values)
    {
        self.incModCount();
        incModCount();
        for (int i = 0; i < size(); i++)
        {
            update((T) nodes[i], null);
        }
        nodes = Arrays.copyOf(values.nodes, values.size());
        size = values.size();

        for (int i = 0; i < size(); i++)
        {
            update(null, (T) nodes[i]);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A> A[] toArray(A[] a)
    {
        if (a.length >= size)
        {
            System.arraycopy(nodes, 0, a, 0, size);
            return a;
        }
        return (A[]) Arrays.copyOf(nodes, size, a.getClass());
    }

    @Override
    public Object[] toArray()
    {
        return Arrays.copyOf(nodes, size);
    }

    protected void replace(T node, T other)
    {
        incModCount();
        for (int i = 0; i < size(); i++)
        {
            if (nodes[i] == node)
            {
                nodes[i] = other;
                update(node, other);
            }
        }
    }

    @Override
    public int indexOf(Object node)
    {
        for (int i = 0; i < size; i++)
        {
            if (nodes[i] == node)
            {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean contains(Object o)
    {
        return indexOf(o) != -1;
    }

    @Override
    public boolean containsAll(Collection<?> c)
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean addAll(Collection<? extends T> c)
    {
        for (T e : c)
        {
            add(e);
        }
        return true;
    }

    public boolean addAll(T[] c)
    {
        for (T e : c)
        {
            add(e);
        }
        return true;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < size; i++)
        {
            if (i != 0)
            {
                sb.append(", ");
            }
            sb.append(nodes[i]);
        }
        sb.append(']');
        return sb.toString();
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

    public SubList<T> subList(int startIndex)
    {
        return new SubList<>(this, startIndex);
    }

    public static final class SubList<R extends Node> extends AbstractList<R> implements NodeIterable<R>, RandomAccess
    {
        private final NodeList<R> list;
        private final int offset;

        private SubList(NodeList<R> list, int offset)
        {
            this.list = list;
            this.offset = offset;
        }

        @Override
        public R get(int index)
        {
            return list.get(offset + index);
        }

        @Override
        public int size()
        {
            return list.size() - offset;
        }

        public SubList<R> subList(int startIndex)
        {
            return new SubList<>(this.list, startIndex + offset);
        }

        @Override
        public Iterator<R> iterator()
        {
            return new NodeListIterator<>(list, offset);
        }
    }

    private static final class NodeListIterator<R extends Node> implements Iterator<R>
    {
        private final NodeList<R> list;
        private final int expectedModCount;
        private int index;

        private NodeListIterator(NodeList<R> list, int startIndex)
        {
            this.list = list;
            this.expectedModCount = list.modCount;
            this.index = startIndex;
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
