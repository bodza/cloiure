package cloiure.lang;

public interface IReference extends IMeta
{
    IPersistentMap alterMeta(IFn alter, ISeq args);
    IPersistentMap resetMeta(IPersistentMap m);
}
