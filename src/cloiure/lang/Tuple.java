package cloiure.lang;

import java.util.Collection;
import java.util.RandomAccess;

public class Tuple
{
    static final int MAX_SIZE = 6;

    public static IPersistentVector create()
    {
        return PersistentVector.EMPTY;
    }

    public static IPersistentVector create(Object v0)
    {
        return RT.vector(v0);
    }

    public static IPersistentVector create(Object v0, Object v1)
    {
        return RT.vector(v0, v1);
    }

    public static IPersistentVector create(Object v0, Object v1, Object v2)
    {
        return RT.vector(v0, v1, v2);
    }

    public static IPersistentVector create(Object v0, Object v1, Object v2, Object v3)
    {
        return RT.vector(v0, v1, v2, v3);
    }

    public static IPersistentVector create(Object v0, Object v1, Object v2, Object v3, Object v4)
    {
        return RT.vector(v0, v1, v2, v3, v4);
    }

    public static IPersistentVector create(Object v0, Object v1, Object v2, Object v3, Object v4, Object v5)
    {
        return RT.vector(v0, v1, v2, v3, v4, v5);
    }
}
