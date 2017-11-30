package cloiure.lang;

public interface IRef extends IDeref
{
    void setValidator(IFn vf);

    IFn getValidator();

    IPersistentMap getWatches();

    IRef addWatch(Object key, IFn callback);

    IRef removeWatch(Object key);
}
