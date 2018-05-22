package graalvm.compiler.graph;

import java.util.List;

import graalvm.compiler.graph.Edges.Type;

public final class NodeSuccessorList<T extends Node> extends NodeList<T>
{
    public NodeSuccessorList(Node self, int initialSize)
    {
        super(self, initialSize);
    }

    protected NodeSuccessorList(Node self)
    {
        super(self);
    }

    public NodeSuccessorList(Node self, T[] elements)
    {
        super(self, elements);
    }

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
