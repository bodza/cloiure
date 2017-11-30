package cloiure.lang;

import java.util.Enumeration;

public class SeqEnumeration implements Enumeration
{
    ISeq seq;

    public SeqEnumeration(ISeq seq)
    {
        this.seq = seq;
    }

    public boolean hasMoreElements()
    {
        return (seq != null);
    }

    public Object nextElement()
    {
        Object ret = RT.first(seq);
        seq = RT.next(seq);
        return ret;
    }
}
