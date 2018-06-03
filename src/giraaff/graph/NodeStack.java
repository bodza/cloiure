package giraaff.graph;

// @class NodeStack
public final class NodeStack
{
    // @def
    private static final int DEFAULT_INITIAL_SIZE = 8;

    // @field
    protected Node[] values;
    // @field
    public int tos;

    // @cons
    public NodeStack()
    {
        this(DEFAULT_INITIAL_SIZE);
    }

    // @cons
    public NodeStack(int __initialSize)
    {
        super();
        values = new Node[__initialSize];
    }

    public int size()
    {
        return tos;
    }

    public void push(Node __n)
    {
        int __newIndex = tos++;
        int __valuesLength = values.length;
        if (__newIndex >= __valuesLength)
        {
            grow();
        }
        values[__newIndex] = __n;
    }

    private void grow()
    {
        int __valuesLength = values.length;
        Node[] __newValues = new Node[__valuesLength << 1];
        System.arraycopy(values, 0, __newValues, 0, __valuesLength);
        values = __newValues;
    }

    public Node get(int __index)
    {
        return values[__index];
    }

    public Node pop()
    {
        return values[--tos];
    }

    public Node peek()
    {
        return values[tos - 1];
    }

    public boolean isEmpty()
    {
        return tos == 0;
    }

    public void clear()
    {
        tos = 0;
    }
}
