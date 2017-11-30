package cloiure.lang;

public interface ITransientCollection
{
    ITransientCollection conj(Object val);

    IPersistentCollection persistent();
}
