package giraaff.graph.iterators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import giraaff.graph.Node;

// @iface NodeIterable
public interface NodeIterable<T extends Node> extends Iterable<T>
{
    @SuppressWarnings("unchecked")
    default <F extends T> NodeIterable<F> filter(Class<F> clazz)
    {
        return (NodeIterable<F>) new FilteredNodeIterable<>(this).and(NodePredicates.isA(clazz));
    }

    default FilteredNodeIterable<T> filter(NodePredicate predicate)
    {
        return new FilteredNodeIterable<>(this).and(predicate);
    }

    default List<T> snapshot()
    {
        ArrayList<T> __list = new ArrayList<>();
        snapshotTo(__list);
        return __list;
    }

    default void snapshotTo(Collection<? super T> to)
    {
        for (T __n : this)
        {
            to.add(__n);
        }
    }

    default T first()
    {
        Iterator<T> __iterator = iterator();
        if (__iterator.hasNext())
        {
            return __iterator.next();
        }
        return null;
    }

    default int count()
    {
        int __count = 0;
        Iterator<T> __iterator = iterator();
        while (__iterator.hasNext())
        {
            __iterator.next();
            __count++;
        }
        return __count;
    }

    default boolean isEmpty()
    {
        return !iterator().hasNext();
    }

    default boolean isNotEmpty()
    {
        return iterator().hasNext();
    }

    default boolean contains(T node)
    {
        for (T __next : this)
        {
            if (__next == node)
            {
                return true;
            }
        }
        return false;
    }
}
