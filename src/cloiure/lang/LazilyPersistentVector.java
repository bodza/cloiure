package cloiure.lang;

import java.util.Collection;
import java.util.RandomAccess;

public class LazilyPersistentVector
{
    static public IPersistentVector createOwning(Object... items)
    {
     // if (items.length <= Tuple.MAX_SIZE)
     //     return Tuple.createFromArray(items);
     // else
        if (items.length <= 32)
        {
            return new PersistentVector(items.length, 5, PersistentVector.EMPTY_NODE, items);
        }
        return PersistentVector.create(items);
    }

    static int fcount(Object c)
    {
        if (c instanceof Counted)
        {
            return ((Counted) c).count();
        }
        return ((Collection)c).size();
    }

    static public IPersistentVector create(Object obj)
    {
     // if ((obj instanceof Counted || obj instanceof RandomAccess) && fcount(obj) <= Tuple.MAX_SIZE)
     //     return Tuple.createFromColl(obj);
     // else
        if (obj instanceof IReduceInit)
        {
            return PersistentVector.create((IReduceInit) obj);
        }
        else if (obj instanceof ISeq)
        {
            return PersistentVector.create(RT.seq(obj));
        }
        else if (obj instanceof Iterable)
        {
            return PersistentVector.create((Iterable)obj);
        }
        else
        {
            return createOwning(RT.toArray(obj));
        }
    }
}
