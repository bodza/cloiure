package giraaff.graph.iterators;

import java.util.Iterator;

import giraaff.graph.Node;

// @class FilteredNodeIterable
public final class FilteredNodeIterable<T extends Node> implements NodeIterable<T>
{
    // @field
    protected final NodeIterable<T> nodeIterable;
    // @field
    protected NodePredicate predicate = NodePredicates.alwaysTrue();

    // @cons
    public FilteredNodeIterable(NodeIterable<T> __nodeIterable)
    {
        super();
        this.nodeIterable = __nodeIterable;
    }

    public FilteredNodeIterable<T> and(NodePredicate __nodePredicate)
    {
        this.predicate = this.predicate.and(__nodePredicate);
        return this;
    }

    @Override
    public Iterator<T> iterator()
    {
        return new PredicatedProxyNodeIterator<>(nodeIterable.iterator(), predicate);
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
