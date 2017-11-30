package cloiure.lang;

/**
 * A persistent, functional, sequence interface
 * <p/>
 * ISeqs are immutable values, i.e. neither first(), nor rest() changes
 * or invalidates the ISeq
 */
public interface ISeq extends IPersistentCollection
{
    Object first();

    ISeq next();

    ISeq more();

    ISeq cons(Object o);
}
