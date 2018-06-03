package giraaff.graph;

import giraaff.nodeinfo.InputType;

/**
 * Describes an edge slot for a {@link NodeClass}.
 */
// @class Position
public final class Position
{
    /**
     * The edges in which this position lies.
     */
    // @field
    private final Edges edges;

    /**
     * Index of the {@link Node} or {@link NodeList} field denoted by this position.
     */
    // @field
    private final int index;

    /**
     * Index within a {@link NodeList} if {@link #index} denotes a {@link NodeList} field otherwise
     * {@link Node#NOT_ITERABLE}.
     */
    // @field
    private final int subIndex;

    // @cons
    public Position(Edges __edges, int __index, int __subIndex)
    {
        super();
        this.edges = __edges;
        this.index = __index;
        this.subIndex = __subIndex;
    }

    public Node get(Node __node)
    {
        if (index < edges.getDirectCount())
        {
            return Edges.getNode(__node, edges.getOffsets(), index);
        }
        else
        {
            return Edges.getNodeList(__node, edges.getOffsets(), index).get(subIndex);
        }
    }

    public InputType getInputType()
    {
        return ((InputEdges) edges).getInputType(index);
    }

    public String getName()
    {
        return edges.getName(index);
    }

    public boolean isInputOptional()
    {
        return ((InputEdges) edges).isOptional(index);
    }

    public void set(Node __node, Node __value)
    {
        if (index < edges.getDirectCount())
        {
            edges.setNode(__node, index, __value);
        }
        else
        {
            Edges.getNodeList(__node, edges.getOffsets(), index).set(subIndex, __value);
        }
    }

    public void initialize(Node __node, Node __value)
    {
        if (index < edges.getDirectCount())
        {
            edges.initializeNode(__node, index, __value);
        }
        else
        {
            Edges.getNodeList(__node, edges.getOffsets(), index).initialize(subIndex, __value);
        }
    }

    @Override
    public int hashCode()
    {
        final int __prime = 31;
        int __result = 1;
        __result = __prime * __result + index;
        __result = __prime * __result + edges.hashCode();
        __result = __prime * __result + subIndex;
        return __result;
    }

    @Override
    public boolean equals(Object __obj)
    {
        if (this == __obj)
        {
            return true;
        }
        if (__obj == null)
        {
            return false;
        }
        if (getClass() != __obj.getClass())
        {
            return false;
        }
        Position __other = (Position) __obj;
        if (index != __other.index)
        {
            return false;
        }
        if (edges != __other.edges)
        {
            return false;
        }
        if (subIndex != __other.subIndex)
        {
            return false;
        }
        return true;
    }

    /**
     * Gets the index within a {@link NodeList} if {@link #getIndex()} denotes a {@link NodeList}
     * field otherwise {@link Node#NOT_ITERABLE}.
     */
    public int getSubIndex()
    {
        return subIndex;
    }

    /**
     * Gets the index of the {@link Node} or {@link NodeList} field denoted by this position.
     */
    public int getIndex()
    {
        return index;
    }
}
