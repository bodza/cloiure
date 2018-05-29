package giraaff.graph.iterators;

import java.util.Iterator;

import giraaff.graph.Node;

// @class PredicatedProxyNodeIterator
public final class PredicatedProxyNodeIterator<T extends Node> extends NodeIterator<T>
{
    private final Iterator<T> iterator;
    private final NodePredicate predicate;

    // @cons
    public PredicatedProxyNodeIterator(Iterator<T> iterator, NodePredicate predicate)
    {
        super();
        this.iterator = iterator;
        this.predicate = predicate;
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
