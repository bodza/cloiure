package cloiure.lang;

public interface IPersistentCollection extends Seqable
{
    int count();

    IPersistentCollection cons(Object o);

    IPersistentCollection empty();

    boolean equiv(Object o);
}
