package giraaff.graph;

import java.util.Iterator;
import java.util.NoSuchElementException;

// @class TypedGraphNodeIterator
final class TypedGraphNodeIterator<T extends IterableNodeType> implements Iterator<T>
{
    // @field
    private final Graph ___graph;
    // @field
    private final int[] ___ids;
    // @field
    private final Node[] ___current;

    // @field
    private int ___currentIdIndex;
    // @field
    private boolean ___needsForward;

    // @cons
    TypedGraphNodeIterator(NodeClass<?> __clazz, Graph __graph)
    {
        super();
        this.___graph = __graph;
        this.___ids = __clazz.iterableIds();
        this.___currentIdIndex = 0;
        this.___current = new Node[this.___ids.length];
        this.___needsForward = true;
    }

    private Node findNext()
    {
        if (this.___needsForward)
        {
            forward();
        }
        else
        {
            Node __c = current();
            Node __afterDeleted = this.___graph.getIterableNodeNext(__c);
            if (__afterDeleted == null)
            {
                this.___needsForward = true;
            }
            else if (__c != __afterDeleted)
            {
                setCurrent(__afterDeleted);
            }
        }
        if (this.___needsForward)
        {
            return null;
        }
        return current();
    }

    private void forward()
    {
        this.___needsForward = false;
        int __startIdx = this.___currentIdIndex;
        while (true)
        {
            Node __next;
            if (current() == null)
            {
                __next = this.___graph.getIterableNodeStart(this.___ids[this.___currentIdIndex]);
            }
            else
            {
                __next = this.___graph.getIterableNodeNext(current().___typeCacheNext);
            }
            if (__next == null)
            {
                this.___currentIdIndex++;
                if (this.___currentIdIndex >= this.___ids.length)
                {
                    this.___currentIdIndex = 0;
                }
                if (this.___currentIdIndex == __startIdx)
                {
                    this.___needsForward = true;
                    return;
                }
            }
            else
            {
                setCurrent(__next);
                break;
            }
        }
    }

    private Node current()
    {
        return this.___current[this.___currentIdIndex];
    }

    private void setCurrent(Node __n)
    {
        this.___current[this.___currentIdIndex] = __n;
    }

    @Override
    public boolean hasNext()
    {
        return findNext() != null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T next()
    {
        Node __result = findNext();
        if (__result == null)
        {
            throw new NoSuchElementException();
        }
        this.___needsForward = true;
        return (T) __result;
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}
