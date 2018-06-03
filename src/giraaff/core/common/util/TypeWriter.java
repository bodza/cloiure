package giraaff.core.common.util;

/**
 * Provides low-level sequential write access for signed and unsigned values of size 1, 2, 4, and 8 bytes.
 */
// @iface TypeWriter
public interface TypeWriter
{
    /**
     * Returns the number of bytes that have been written, i.e., the byte index of the next byte to be written.
     */
    long getBytesWritten();

    /** Writes a signed 1 byte value. */
    void putS1(long value);

    /** Writes an unsigned 1 byte value. */
    void putU1(long value);

    /** Writes a signed 2 byte value. */
    void putS2(long value);

    /** Writes an unsigned 2 byte value. */
    void putU2(long value);

    /** Writes a signed 4 byte value. */
    void putS4(long value);

    /** Writes an unsigned 4 byte value. */
    void putU4(long value);

    /** Writes a signed 8 byte value. */
    void putS8(long value);

    /**
     * Writes a signed value in a variable byte size encoding.
     */
    default void putSV(long value)
    {
        long __cur = value;
        while (true)
        {
            if (__cur >= -64 && __cur < 64)
            {
                putU1(__cur & 0x7f);
                return;
            }
            putU1(0x80 | (__cur & 0x7f));
            __cur = __cur >> 7;
        }
    }

    /**
     * Writes an unsigned value in a variable byte size encoding.
     */
    default void putUV(long value)
    {
        long __cur = value;
        while (true)
        {
            if (__cur < 128)
            {
                putU1(__cur & 0x7f);
                return;
            }
            putU1(0x80 | (__cur & 0x7f));
            __cur = __cur >> 7;
        }
    }
}
