package giraaff.graph;

import java.util.Iterator;
import java.util.NoSuchElementException;

// @class TypedGraphNodeIterator
final class TypedGraphNodeIterator<T extends IterableNodeType> implements Iterator<T>
{
    // @field
    private final Graph graph;
    // @field
    private final int[] ids;
    // @field
    private final Node[] current;

    // @field
    private int currentIdIndex;
    // @field
    private boolean needsForward;

    // @cons
    TypedGraphNodeIterator(NodeClass<?> __clazz, Graph __graph)
    {
        super();
        this.graph = __graph;
        ids = __clazz.iterableIds();
        currentIdIndex = 0;
        current = new Node[ids.length];
        needsForward = true;
    }

    private Node findNext()
    {
        if (needsForward)
        {
            forward();
        }
        else
        {
            Node __c = current();
            Node __afterDeleted = graph.getIterableNodeNext(__c);
            if (__afterDeleted == null)
            {
                needsForward = true;
            }
            else if (__c != __afterDeleted)
            {
                setCurrent(__afterDeleted);
            }
        }
        if (needsForward)
        {
            return null;
        }
        return current();
    }

    private void forward()
    {
        needsForward = false;
        int __startIdx = currentIdIndex;
        while (true)
        {
            Node __next;
            if (current() == null)
            {
                __next = graph.getIterableNodeStart(ids[currentIdIndex]);
            }
            else
            {
                __next = graph.getIterableNodeNext(current().typeCacheNext);
            }
            if (__next == null)
            {
                currentIdIndex++;
                if (currentIdIndex >= ids.length)
                {
                    currentIdIndex = 0;
                }
                if (currentIdIndex == __startIdx)
                {
                    needsForward = true;
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
        return current[currentIdIndex];
    }

    private void setCurrent(Node __n)
    {
        current[currentIdIndex] = __n;
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
        needsForward = true;
        return (T) __result;
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}
