package giraaff.graph;

import giraaff.nodeinfo.InputType;

///
// Describes an edge slot for a {@link NodeClass}.
///
// @class Position
public final class Position
{
    ///
    // The edges in which this position lies.
    ///
    // @field
    private final Edges ___edges;

    ///
    // Index of the {@link Node} or {@link NodeList} field denoted by this position.
    ///
    // @field
    private final int ___index;

    ///
    // Index within a {@link NodeList} if {@link #index} denotes a {@link NodeList} field otherwise
    // {@link Node#NOT_ITERABLE}.
    ///
    // @field
    private final int ___subIndex;

    // @cons Position
    public Position(Edges __edges, int __index, int __subIndex)
    {
        super();
        this.___edges = __edges;
        this.___index = __index;
        this.___subIndex = __subIndex;
    }

    public Node get(Node __node)
    {
        if (this.___index < this.___edges.getDirectCount())
        {
            return Edges.getNode(__node, this.___edges.getOffsets(), this.___index);
        }
        else
        {
            return Edges.getNodeList(__node, this.___edges.getOffsets(), this.___index).get(this.___subIndex);
        }
    }

    public InputType getInputType()
    {
        return ((InputEdges) this.___edges).getInputType(this.___index);
    }

    public String getName()
    {
        return this.___edges.getName(this.___index);
    }

    public boolean isInputOptional()
    {
        return ((InputEdges) this.___edges).isOptional(this.___index);
    }

    public void set(Node __node, Node __value)
    {
        if (this.___index < this.___edges.getDirectCount())
        {
            this.___edges.setNode(__node, this.___index, __value);
        }
        else
        {
            Edges.getNodeList(__node, this.___edges.getOffsets(), this.___index).set(this.___subIndex, __value);
        }
    }

    public void initialize(Node __node, Node __value)
    {
        if (this.___index < this.___edges.getDirectCount())
        {
            this.___edges.initializeNode(__node, this.___index, __value);
        }
        else
        {
            Edges.getNodeList(__node, this.___edges.getOffsets(), this.___index).initialize(this.___subIndex, __value);
        }
    }

    @Override
    public int hashCode()
    {
        final int __prime = 31;
        int __result = 1;
        __result = __prime * __result + this.___index;
        __result = __prime * __result + this.___edges.hashCode();
        __result = __prime * __result + this.___subIndex;
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
        if (this.___index != __other.___index)
        {
            return false;
        }
        if (this.___edges != __other.___edges)
        {
            return false;
        }
        if (this.___subIndex != __other.___subIndex)
        {
            return false;
        }
        return true;
    }

    ///
    // Gets the index within a {@link NodeList} if {@link #getIndex()} denotes a {@link NodeList}
    // field otherwise {@link Node#NOT_ITERABLE}.
    ///
    public int getSubIndex()
    {
        return this.___subIndex;
    }

    ///
    // Gets the index of the {@link Node} or {@link NodeList} field denoted by this position.
    ///
    public int getIndex()
    {
        return this.___index;
    }
}
