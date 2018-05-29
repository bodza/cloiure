package giraaff.graph;

import java.util.List;

import giraaff.graph.Edges.Type;

// @class NodeSuccessorList
public final class NodeSuccessorList<T extends Node> extends NodeList<T>
{
    // @cons
    public NodeSuccessorList(Node self, int initialSize)
    {
        super(self, initialSize);
    }

    // @cons
    protected NodeSuccessorList(Node self)
    {
        super(self);
    }

    // @cons
    public NodeSuccessorList(Node self, T[] elements)
    {
        super(self, elements);
    }

    // @cons
    public NodeSuccessorList(Node self, List<? extends T> elements)
    {
        super(self, elements);
    }

    @Override
    protected void update(T oldNode, T newNode)
    {
        self.updatePredecessor(oldNode, newNode);
    }

    @Override
    public Type getEdgesType()
    {
        return Type.Successors;
    }
}
