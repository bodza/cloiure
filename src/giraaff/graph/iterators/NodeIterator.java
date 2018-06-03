package giraaff.graph.iterators;

import java.util.Iterator;
import java.util.NoSuchElementException;

import giraaff.graph.Node;

// @class NodeIterator
public abstract class NodeIterator<T extends Node> implements Iterator<T>
{
    // @field
    protected T ___current;

    protected abstract void forward();

    @Override
    public boolean hasNext()
    {
        forward();
        return this.___current != null;
    }

    @Override
    public T next()
    {
        forward();
        T __ret = this.___current;
        if (this.___current == null)
        {
            throw new NoSuchElementException();
        }
        this.___current = null;
        return __ret;
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}
