package giraaff.core.common.util;

/**
 * Provides low-level read access for signed and unsigned values of size 1, 2, 4, and 8 bytes.
 */
// @iface TypeReader
public interface TypeReader
{
    /** Returns the next byte index to be read. */
    long getByteIndex();

    /** Sets the next byte index to be read. */
    void setByteIndex(long byteIndex);

    /** Reads a signed 1 byte value. */
    int getS1();

    /** Reads an unsigned 1 byte value. */
    int getU1();

    /** Reads a signed 2 byte value. */
    int getS2();

    /** Reads an unsigned 2 byte value. */
    int getU2();

    /** Reads a signed 4 byte value. */
    int getS4();

    /** Reads an unsigned 4 byte value. */
    long getU4();

    /** Reads a signed 4 byte value. */
    long getS8();

    /**
     * Reads a signed value that has been written using {@link TypeWriter#putSV variable byte size encoding}.
     */
    default long getSV()
    {
        long __result = 0;
        int __shift = 0;
        long __b;
        do
        {
            __b = getU1();
            __result |= (__b & 0x7f) << __shift;
            __shift += 7;
        } while ((__b & 0x80) != 0);

        if ((__b & 0x40) != 0 && __shift < 64)
        {
            __result |= -1L << __shift;
        }
        return __result;
    }

    /**
     * Reads a signed variable byte size encoded value that is known to fit into the range of int.
     */
    default int getSVInt()
    {
        return TypeConversion.asS4(getSV());
    }

    /**
     * Reads an unsigned value that has been written using {@link TypeWriter#putSV variable byte
     * size encoding}.
     */
    default long getUV()
    {
        long __result = 0;
        int __shift = 0;
        long __b;
        do
        {
            __b = getU1();
            __result |= (__b & 0x7f) << __shift;
            __shift += 7;
        } while ((__b & 0x80) != 0);

        return __result;
    }

    /**
     * Reads an unsigned variable byte size encoded value that is known to fit into the range of int.
     */
    default int getUVInt()
    {
        return TypeConversion.asS4(getUV());
    }
}
