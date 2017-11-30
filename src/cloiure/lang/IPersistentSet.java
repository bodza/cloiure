package cloiure.lang;

public interface IPersistentSet extends IPersistentCollection, Counted
{
    public IPersistentSet disjoin(Object key);
    public boolean contains(Object key);
    public Object get(Object key);
}
