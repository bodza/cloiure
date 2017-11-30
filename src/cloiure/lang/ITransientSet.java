package cloiure.lang;

public interface ITransientSet extends ITransientCollection, Counted
{
    public ITransientSet disjoin(Object key);
    public boolean contains(Object key);
    public Object get(Object key);
}
