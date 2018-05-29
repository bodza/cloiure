package giraaff.graph;

import java.util.Collection;
import java.util.List;

import giraaff.graph.Edges.Type;

// @class NodeInputList
public final class NodeInputList<T extends Node> extends NodeList<T>
{
    // @cons
    public NodeInputList(Node self, int initialSize)
    {
        super(self, initialSize);
    }

    // @cons
    public NodeInputList(Node self)
    {
        super(self);
    }

    // @cons
    public NodeInputList(Node self, T[] elements)
    {
        super(self, elements);
    }

    // @cons
    public NodeInputList(Node self, List<? extends T> elements)
    {
        super(self, elements);
    }

    // @cons
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
        return Type.Inputs;
    }
}
