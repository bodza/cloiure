package cloiure.lang;

public interface ILookup
{
    Object valAt(Object key);

    Object valAt(Object key, Object notFound);
}
