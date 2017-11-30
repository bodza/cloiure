package cloiure.lang;

public interface IPersistentMap extends Iterable, Associative, Counted
{
    IPersistentMap assoc(Object key, Object val);

    IPersistentMap assocEx(Object key, Object val);

    IPersistentMap without(Object key);
}
