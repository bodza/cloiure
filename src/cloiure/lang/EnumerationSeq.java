package cloiure.lang;

import java.io.IOException;
import java.io.NotSerializableException;
import java.util.Enumeration;

public class EnumerationSeq extends ASeq
{
    final Enumeration iter;
    final State state;

    static class State
    {
        volatile Object val;
        volatile Object _rest;
    }

    public static EnumerationSeq create(Enumeration iter)
    {
        if (iter.hasMoreElements())
        {
            return new EnumerationSeq(iter);
        }
        return null;
    }

    EnumerationSeq(Enumeration iter)
    {
        this.iter = iter;
        state = new State();
        this.state.val = state;
        this.state._rest = state;
    }

    EnumerationSeq(IPersistentMap meta, Enumeration iter, State state)
    {
        super(meta);
        this.iter = iter;
        this.state = state;
    }

    public Object first()
    {
        if (state.val == state)
        {
            synchronized (state)
            {
                if (state.val == state)
                {
                    state.val = iter.nextElement();
                }
            }
        }
        return state.val;
    }

    public ISeq next()
    {
        if (state._rest == state)
        {
            synchronized (state)
            {
                if (state._rest == state)
                {
                    first();
                    state._rest = create(iter);
                }
            }
        }
        return (ISeq) state._rest;
    }

    public EnumerationSeq withMeta(IPersistentMap meta)
    {
        return new EnumerationSeq(meta, iter, state);
    }

    private void writeObject (java.io.ObjectOutputStream out) throws IOException
    {
        throw new NotSerializableException(getClass().getName());
    }
}
