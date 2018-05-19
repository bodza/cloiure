package graalvm.compiler.graph;

import static graalvm.compiler.graph.Edges.Type.Inputs;

import java.util.Collection;
import java.util.List;

import graalvm.compiler.graph.Edges.Type;

public final class NodeInputList<T extends Node> extends NodeList<T>
{
    public NodeInputList(Node self, int initialSize)
    {
        super(self, initialSize);
    }

    public NodeInputList(Node self)
    {
        super(self);
    }

    public NodeInputList(Node self, T[] elements)
    {
        super(self, elements);
    }

    public NodeInputList(Node self, List<? extends T> elements)
    {
        super(self, elements);
    }

    public NodeInputList(Node self, Collection<? extends NodeInterface> elements)
    {
        super(self, elements);
    }

    @Override
    protected void update(T oldNode, T newNode)
    {
        self.updateUsages(oldNode, newNode);
    }

    @Override
    public Type getEdgesType()
    {
        return Inputs;
    }
}
