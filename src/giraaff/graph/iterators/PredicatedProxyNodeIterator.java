package giraaff.graph.iterators;

import java.util.Iterator;

import giraaff.graph.Node;

// @class PredicatedProxyNodeIterator
public final class PredicatedProxyNodeIterator<T extends Node> extends NodeIterator<T>
{
    // @field
    private final Iterator<T> ___iterator;
    // @field
    private final NodePredicate ___predicate;

    // @cons
    public PredicatedProxyNodeIterator(Iterator<T> __iterator, NodePredicate __predicate)
    {
        super();
        this.___iterator = __iterator;
        this.___predicate = __predicate;
    }

    @Override
    protected void forward()
    {
        while ((this.___current == null || !this.___current.isAlive() || !this.___predicate.apply(this.___current)) && this.___iterator.hasNext())
        {
            this.___current = this.___iterator.next();
        }
        if (this.___current != null && (!this.___current.isAlive() || !this.___predicate.apply(this.___current)))
        {
            this.___current = null;
        }
    }
}
