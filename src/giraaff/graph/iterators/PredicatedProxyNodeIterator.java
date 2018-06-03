package giraaff.graph.iterators;

import java.util.Iterator;

import giraaff.graph.Node;

// @class PredicatedProxyNodeIterator
public final class PredicatedProxyNodeIterator<T extends Node> extends NodeIterator<T>
{
    // @field
    private final Iterator<T> iterator;
    // @field
    private final NodePredicate predicate;

    // @cons
    public PredicatedProxyNodeIterator(Iterator<T> __iterator, NodePredicate __predicate)
    {
        super();
        this.iterator = __iterator;
        this.predicate = __predicate;
    }

    @Override
    protected void forward()
    {
        while ((current == null || !current.isAlive() || !predicate.apply(current)) && iterator.hasNext())
        {
            current = iterator.next();
        }
        if (current != null && (!current.isAlive() || !predicate.apply(current)))
        {
            current = null;
        }
    }
}
