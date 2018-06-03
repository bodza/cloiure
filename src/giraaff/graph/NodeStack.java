package giraaff.graph;

// @class NodeStack
public final class NodeStack
{
    // @def
    private static final int DEFAULT_INITIAL_SIZE = 8;

    // @field
    protected Node[] ___values;
    // @field
    public int ___tos;

    // @cons
    public NodeStack()
    {
        this(DEFAULT_INITIAL_SIZE);
    }

    // @cons
    public NodeStack(int __initialSize)
    {
        super();
        this.___values = new Node[__initialSize];
    }

    public int size()
    {
        return this.___tos;
    }

    public void push(Node __n)
    {
        int __newIndex = this.___tos++;
        int __valuesLength = this.___values.length;
        if (__newIndex >= __valuesLength)
        {
            grow();
        }
        this.___values[__newIndex] = __n;
    }

    private void grow()
    {
        int __valuesLength = this.___values.length;
        Node[] __newValues = new Node[__valuesLength << 1];
        System.arraycopy(this.___values, 0, __newValues, 0, __valuesLength);
        this.___values = __newValues;
    }

    public Node get(int __index)
    {
        return this.___values[__index];
    }

    public Node pop()
    {
        return this.___values[--this.___tos];
    }

    public Node peek()
    {
        return this.___values[this.___tos - 1];
    }

    public boolean isEmpty()
    {
        return this.___tos == 0;
    }

    public void clear()
    {
        this.___tos = 0;
    }
}
