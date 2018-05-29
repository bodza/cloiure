package giraaff.graph;

// @class NodeStack
public final class NodeStack
{
    private static final int DEFAULT_INITIAL_SIZE = 8;

    protected Node[] values;
    public int tos;

    // @cons
    public NodeStack()
    {
        this(DEFAULT_INITIAL_SIZE);
    }

    // @cons
    public NodeStack(int initialSize)
    {
        super();
        values = new Node[initialSize];
    }

    public int size()
    {
        return tos;
    }

    public void push(Node n)
    {
        int newIndex = tos++;
        int valuesLength = values.length;
        if (newIndex >= valuesLength)
        {
            grow();
        }
        values[newIndex] = n;
    }

    private void grow()
    {
        int valuesLength = values.length;
        Node[] newValues = new Node[valuesLength << 1];
        System.arraycopy(values, 0, newValues, 0, valuesLength);
        values = newValues;
    }

    public Node get(int index)
    {
        return values[index];
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

    @Override
    public String toString()
    {
        if (tos == 0)
        {
            return "NodeStack: []";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tos; i++)
        {
            sb.append(", ");
            sb.append(values[i]);
        }
        return "NodeStack: [" + sb.substring(2) + "]";
    }
}
