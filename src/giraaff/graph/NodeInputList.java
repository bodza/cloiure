package giraaff.graph;

import java.util.Collection;
import java.util.List;

import giraaff.graph.Edges;

// @class NodeInputList
public final class NodeInputList<T extends Node> extends NodeList<T>
{
    // @cons NodeInputList
    public NodeInputList(Node __self, int __initialSize)
    {
        super(__self, __initialSize);
    }

    // @cons NodeInputList
    public NodeInputList(Node __self)
    {
        super(__self);
    }

    // @cons NodeInputList
    public NodeInputList(Node __self, T[] __elements)
    {
        super(__self, __elements);
    }

    // @cons NodeInputList
    public NodeInputList(Node __self, List<? extends T> __elements)
    {
        super(__self, __elements);
    }

    // @cons NodeInputList
    public NodeInputList(Node __self, Collection<? extends NodeInterface> __elements)
    {
        super(__self, __elements);
    }

    @Override
    protected void update(T __oldNode, T __newNode)
    {
        this.___self.updateUsages(__oldNode, __newNode);
    }

    @Override
    public Edges.EdgesType getEdgesType()
    {
        return Edges.EdgesType.Inputs;
    }
}
