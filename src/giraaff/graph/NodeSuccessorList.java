package giraaff.graph;

import java.util.List;

import giraaff.graph.Edges;

// @class NodeSuccessorList
public final class NodeSuccessorList<T extends Node> extends NodeList<T>
{
    // @cons NodeSuccessorList
    public NodeSuccessorList(Node __self, int __initialSize)
    {
        super(__self, __initialSize);
    }

    // @cons NodeSuccessorList
    protected NodeSuccessorList(Node __self)
    {
        super(__self);
    }

    // @cons NodeSuccessorList
    public NodeSuccessorList(Node __self, T[] __elements)
    {
        super(__self, __elements);
    }

    // @cons NodeSuccessorList
    public NodeSuccessorList(Node __self, List<? extends T> __elements)
    {
        super(__self, __elements);
    }

    @Override
    protected void update(T __oldNode, T __newNode)
    {
        this.___self.updatePredecessor(__oldNode, __newNode);
    }

    @Override
    public Edges.EdgesType getEdgesType()
    {
        return Edges.EdgesType.Successors;
    }
}
