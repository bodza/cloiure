package giraaff.graph.iterators;

import java.util.Iterator;

import giraaff.graph.Node;

// @class FilteredNodeIterable
public final class FilteredNodeIterable<T extends Node> implements NodeIterable<T>
{
    // @field
    protected final NodeIterable<T> ___nodeIterable;
    // @field
    protected NodePredicate ___predicate = NodePredicates.alwaysTrue();

    // @cons FilteredNodeIterable
    public FilteredNodeIterable(NodeIterable<T> __nodeIterable)
    {
        super();
        this.___nodeIterable = __nodeIterable;
    }

    public FilteredNodeIterable<T> and(NodePredicate __nodePredicate)
    {
        this.___predicate = this.___predicate.and(__nodePredicate);
        return this;
    }

    @Override
    public Iterator<T> iterator()
    {
        return new PredicatedProxyNodeIterator<>(this.___nodeIterable.iterator(), this.___predicate);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <F extends T> FilteredNodeIterable<F> filter(Class<F> __clazz)
    {
        return (FilteredNodeIterable<F>) this.and(NodePredicates.isA(__clazz));
    }

    @Override
    public FilteredNodeIterable<T> filter(NodePredicate __p)
    {
        return this.and(__p);
    }
}
