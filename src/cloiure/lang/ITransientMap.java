package cloiure.lang;

public interface ITransientMap extends ITransientAssociative, Counted
{
    ITransientMap assoc(Object key, Object val);

    ITransientMap without(Object key);

    IPersistentMap persistent();
}
