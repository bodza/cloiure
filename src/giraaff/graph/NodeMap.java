package giraaff.graph;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.BiFunction;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.MapCursor;

// @class NodeMap
public final class NodeMap<T> extends NodeIdAccessor implements EconomicMap<Node, T>
{
    private static final int MIN_REALLOC_SIZE = 16;

    protected Object[] values;

    // @cons
    public NodeMap(Graph graph)
    {
        super(graph);
        this.values = new Object[graph.nodeIdCount()];
    }

    // @cons
    public NodeMap(NodeMap<T> copyFrom)
    {
        super(copyFrom.graph);
        this.values = Arrays.copyOf(copyFrom.values, copyFrom.values.length);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get(Node node)
    {
        return (T) values[getNodeId(node)];
    }

    @SuppressWarnings("unchecked")
    public T getAndGrow(Node node)
    {
        checkAndGrow(node);
        return (T) values[getNodeId(node)];
    }

    private void checkAndGrow(Node node)
    {
        if (isNew(node))
        {
            this.values = Arrays.copyOf(values, Math.max(MIN_REALLOC_SIZE, graph.nodeIdCount() * 3 / 2));
        }
    }

    @Override
    public boolean isEmpty()
    {
        throw new UnsupportedOperationException("isEmpty() is not supported for performance reasons");
    }

    @Override
    public boolean containsKey(Node node)
    {
        if (node.graph() == graph())
        {
            return get(node) != null;
        }
        return false;
    }

    public boolean containsValue(Object value)
    {
        for (Object o : values)
        {
            if (o == value)
            {
                return true;
            }
        }
        return false;
    }

    public Graph graph()
    {
        return graph;
    }

    public void set(Node node, T value)
    {
        values[getNodeId(node)] = value;
    }

    public void setAndGrow(Node node, T value)
    {
        checkAndGrow(node);
        set(node, value);
    }

    /**
     * @return Return the key for the entry at index {@code i}
     */
    protected Node getKey(int i)
    {
        return graph.getNode(i);
    }

    @Override
    public int size()
    {
        throw new UnsupportedOperationException("size() is not supported for performance reasons");
    }

    public int capacity()
    {
        return values.length;
    }

    public boolean isNew(Node node)
    {
        return getNodeId(node) >= capacity();
    }

    @Override
    public void clear()
    {
        Arrays.fill(values, null);
    }

    @Override
    public Iterable<Node> getKeys()
    {
        // @closure
        return new Iterable<Node>()
        {
            @Override
            public Iterator<Node> iterator()
            {
                // @closure
                return new Iterator<Node>()
                {
                    int i = 0;

                    @Override
                    public boolean hasNext()
                    {
                        forward();
                        return i < NodeMap.this.values.length;
                    }

                    @Override
                    public Node next()
                    {
                        final int pos = i;
                        final Node key = NodeMap.this.getKey(pos);
                        i++;
                        forward();
                        return key;
                    }

                    @Override
                    public void remove()
                    {
                        throw new UnsupportedOperationException();
                    }

                    private void forward()
                    {
                        while (i < NodeMap.this.values.length && (NodeMap.this.getKey(i) == null || NodeMap.this.values[i] == null))
                        {
                            i++;
                        }
                    }
                };
            }
        };
    }

    @Override
    public MapCursor<Node, T> getEntries()
    {
        // @closure
        return new MapCursor<Node, T>()
        {
            int current = -1;

            @Override
            public boolean advance()
            {
                current++;
                while (current < NodeMap.this.values.length && (NodeMap.this.values[current] == null || NodeMap.this.getKey(current) == null))
                {
                    current++;
                }
                return current < NodeMap.this.values.length;
            }

            @Override
            public Node getKey()
            {
                return NodeMap.this.getKey(current);
            }

            @SuppressWarnings("unchecked")
            @Override
            public T getValue()
            {
                return (T) NodeMap.this.values[current];
            }

            @Override
            public void remove()
            {
                NodeMap.this.values[current] = null;
            }
        };
    }

    @Override
    public Iterable<T> getValues()
    {
        // @closure
        return new Iterable<T>()
        {
            @Override
            public Iterator<T> iterator()
            {
                // @closure
                return new Iterator<T>()
                {
                    int i = 0;

                    @Override
                    public boolean hasNext()
                    {
                        forward();
                        return i < NodeMap.this.values.length;
                    }

                    @SuppressWarnings("unchecked")
                    @Override
                    public T next()
                    {
                        final int pos = i;
                        final T value = (T) NodeMap.this.values[pos];
                        i++;
                        forward();
                        return value;
                    }

                    @Override
                    public void remove()
                    {
                        throw new UnsupportedOperationException();
                    }

                    private void forward()
                    {
                        while (i < NodeMap.this.values.length && (NodeMap.this.getKey(i) == null || NodeMap.this.values[i] == null))
                        {
                            i++;
                        }
                    }
                };
            }
        };
    }

    @Override
    public T put(Node key, T value)
    {
        T result = get(key);
        set(key, value);
        return result;
    }

    @Override
    public T removeKey(Node key)
    {
        return put(key, null);
    }

    @Override
    public void replaceAll(BiFunction<? super Node, ? super T, ? extends T> function)
    {
        for (Node n : getKeys())
        {
            put(n, function.apply(n, get(n)));
        }
    }
}
