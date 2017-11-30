package cloiure.lang;

import java.util.Comparator;

public interface Sorted
{
    Comparator comparator();

    Object entryKey(Object entry);

    ISeq seq(boolean ascending);

    ISeq seqFrom(Object key, boolean ascending);
}
