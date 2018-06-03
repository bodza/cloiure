package giraaff.graph;

import java.util.List;

import giraaff.graph.Edges.Type;

// @class NodeSuccessorList
public final class NodeSuccessorList<T extends Node> extends NodeList<T>
{
    // @cons
    public NodeSuccessorList(Node __self, int __initialSize)
    {
        super(__self, __initialSize);
    }

    // @cons
    protected NodeSuccessorList(Node __self)
    {
        super(__self);
    }

    // @cons
    public NodeSuccessorList(Node __self, T[] __elements)
    {
        super(__self, __elements);
    }

    // @cons
    public NodeSuccessorList(Node __self, List<? extends T> __elements)
    {
        super(__self, __elements);
    }

    @Override
    protected void update(T __oldNode, T __newNode)
    {
        self.updatePredecessor(__oldNode, __newNode);
    }

    @Override
    public Type getEdgesType()
    {
        return Type.Successors;
    }
}
