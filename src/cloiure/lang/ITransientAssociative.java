package cloiure.lang;

public interface ITransientAssociative extends ITransientCollection, ILookup
{
    ITransientAssociative assoc(Object key, Object val);
}
