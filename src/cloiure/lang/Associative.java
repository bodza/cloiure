package cloiure.lang;

public interface Associative extends IPersistentCollection, ILookup
{
    boolean containsKey(Object key);

    IMapEntry entryAt(Object key);

    Associative assoc(Object key, Object val);
}
