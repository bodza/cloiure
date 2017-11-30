package cloiure.lang;

import java.util.Iterator;

/**
 * Indicate a map can provide more efficient key and val iterators.
 */
public interface IMapIterable
{
    Iterator keyIterator();

    Iterator valIterator();
}
