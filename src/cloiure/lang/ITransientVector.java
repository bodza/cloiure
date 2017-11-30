package cloiure.lang;

public interface ITransientVector extends ITransientAssociative, Indexed
{
    ITransientVector assocN(int i, Object val);

    ITransientVector pop();
}
