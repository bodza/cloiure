package giraaff.graph;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.BiFunction;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.MapCursor;

// @class NodeMap
public final class NodeMap<T> extends NodeIdAccessor implements EconomicMap<Node, T>
{
    // @def
    private static final int MIN_REALLOC_SIZE = 16;

    // @field
    protected Object[] ___values;

    // @cons
    public NodeMap(Graph __graph)
    {
        super(__graph);
        this.___values = new Object[__graph.nodeIdCount()];
    }

    // @cons
    public NodeMap(NodeMap<T> __copyFrom)
    {
        super(__copyFrom.___graph);
        this.___values = Arrays.copyOf(__copyFrom.___values, __copyFrom.___values.length);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get(Node __node)
    {
        return (T) this.___values[getNodeId(__node)];
    }

    @SuppressWarnings("unchecked")
    public T getAndGrow(Node __node)
    {
        checkAndGrow(__node);
        return (T) this.___values[getNodeId(__node)];
    }

    private void checkAndGrow(Node __node)
    {
        if (isNew(__node))
        {
            this.___values = Arrays.copyOf(this.___values, Math.max(MIN_REALLOC_SIZE, this.___graph.nodeIdCount() * 3 / 2));
        }
    }

    @Override
    public boolean isEmpty()
    {
        throw new UnsupportedOperationException("isEmpty() is not supported for performance reasons");
    }

    @Override
    public boolean containsKey(Node __node)
    {
        if (__node.graph() == graph())
        {
            return get(__node) != null;
        }
        return false;
    }

    public boolean containsValue(Object __value)
    {
        for (Object __o : this.___values)
        {
            if (__o == __value)
            {
                return true;
            }
        }
        return false;
    }

    public Graph graph()
    {
        return this.___graph;
    }

    public void set(Node __node, T __value)
    {
        this.___values[getNodeId(__node)] = __value;
    }

    public void setAndGrow(Node __node, T __value)
    {
        checkAndGrow(__node);
        set(__node, __value);
    }

    ///
    // @return Return the key for the entry at index {@code i}
    ///
    protected Node getKey(int __i)
    {
        return this.___graph.getNode(__i);
    }

    @Override
    public int size()
    {
        throw new UnsupportedOperationException("size() is not supported for performance reasons");
    }

    public int capacity()
    {
        return this.___values.length;
    }

    public boolean isNew(Node __node)
    {
        return getNodeId(__node) >= capacity();
    }

    @Override
    public void clear()
    {
        Arrays.fill(this.___values, null);
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
                    int __i = 0;

                    @Override
                    public boolean hasNext()
                    {
                        forward();
                        return __i < NodeMap.this.___values.length;
                    }

                    @Override
                    public Node next()
                    {
                        final int __pos = __i;
                        final Node __key = NodeMap.this.getKey(__pos);
                        __i++;
                        forward();
                        return __key;
                    }

                    @Override
                    public void remove()
                    {
                        throw new UnsupportedOperationException();
                    }

                    private void forward()
                    {
                        while (__i < NodeMap.this.___values.length && (NodeMap.this.getKey(__i) == null || NodeMap.this.___values[__i] == null))
                        {
                            __i++;
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
            int __current = -1;

            @Override
            public boolean advance()
            {
                __current++;
                while (__current < NodeMap.this.___values.length && (NodeMap.this.___values[__current] == null || NodeMap.this.getKey(__current) == null))
                {
                    __current++;
                }
                return __current < NodeMap.this.___values.length;
            }

            @Override
            public Node getKey()
            {
                return NodeMap.this.getKey(__current);
            }

            @SuppressWarnings("unchecked")
            @Override
            public T getValue()
            {
                return (T) NodeMap.this.___values[__current];
            }

            @Override
            public void remove()
            {
                NodeMap.this.___values[__current] = null;
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
                    int __i = 0;

                    @Override
                    public boolean hasNext()
                    {
                        forward();
                        return __i < NodeMap.this.___values.length;
                    }

                    @SuppressWarnings("unchecked")
                    @Override
                    public T next()
                    {
                        final int __pos = __i;
                        final T __value = (T) NodeMap.this.___values[__pos];
                        __i++;
                        forward();
                        return __value;
                    }

                    @Override
                    public void remove()
                    {
                        throw new UnsupportedOperationException();
                    }

                    private void forward()
                    {
                        while (__i < NodeMap.this.___values.length && (NodeMap.this.getKey(__i) == null || NodeMap.this.___values[__i] == null))
                        {
                            __i++;
                        }
                    }
                };
            }
        };
    }

    @Override
    public T put(Node __key, T __value)
    {
        T __result = get(__key);
        set(__key, __value);
        return __result;
    }

    @Override
    public T removeKey(Node __key)
    {
        return put(__key, null);
    }

    @Override
    public void replaceAll(BiFunction<? super Node, ? super T, ? extends T> __function)
    {
        for (Node __n : getKeys())
        {
            put(__n, __function.apply(__n, get(__n)));
        }
    }
}
