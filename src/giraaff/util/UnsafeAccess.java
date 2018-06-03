package giraaff.util;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

/**
 * Public access to the {@link Unsafe} capability.
 */
// @class UnsafeAccess
public final class UnsafeAccess
{
    // @def
    public static final Unsafe UNSAFE = initUnsafe();

    private static Unsafe initUnsafe()
    {
        try
        {
            // Fast path when we are trusted.
            return Unsafe.getUnsafe();
        }
        catch (SecurityException __se)
        {
            // Slow path when we are not trusted.
            try
            {
                Field __theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                __theUnsafe.setAccessible(true);
                return (Unsafe) __theUnsafe.get(Unsafe.class);
            }
            catch (Exception __e)
            {
                throw new RuntimeException("exception while trying to get Unsafe", __e);
            }
        }
    }
}
