package cloiure.util.concurrent.atomic;

import java.lang.reflect.Field;
// import java.security.AccessController;
// import java.security.PrivilegedAction;

import sun.misc.Unsafe;

/**
 * An object reference that may be updated atomically.
 *
 * @param <V> the type of object referred to by this reference
 */
public class AtomicReference<V>
{
    private static final Unsafe unsafe/* = Unsafe.getUnsafe()*/;
    static
    {
     // unsafe = (Unsafe)AccessController.doPrivileged(new PrivilegedAction()
     // {
     //     public Object run()
     //     {
     //         return Unsafe.getUnsafe();
     //     }
     // });

        try
        {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe)f.get(null);
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    private static final long offset;
    static
    {
        try
        {
            offset = unsafe.objectFieldOffset(AtomicReference.class.getDeclaredField("value"));
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    private volatile V value;

    /**
     * Creates a new AtomicReference with the given initial value.
     *
     * @param v the initial value
     */
    public AtomicReference(V v)
    {
        value = v;
    }

    /**
     * Gets the current value.
     *
     * @return the current value
     */
    public final V get()
    {
        return value;
    }

    /**
     * Sets to the given value.
     *
     * @param v the new value
     */
    public final void set(V v)
    {
        value = v;
    }

    /**
     * Atomically sets the value to the given updated value if the current value {@code ==} the expected value.
     *
     * @param expect the expected value
     * @param update the new value
     * @return false when the actual value was not equal to the expected value, true otherwise
     */
    public final boolean compareAndSet(V expect, V update)
    {
        return unsafe.compareAndSwapObject(this, offset, expect, update);
    }

    /**
     * Returns the String representation of the current value.
     *
     * @return the String representation of the current value
     */
    public String toString()
    {
        return String.valueOf(value);
    }
}
