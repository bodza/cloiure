package giraaff.core.common.util;

import giraaff.util.UnsafeAccess;

/**
 * Provides low-level read access from a byte[] array for signed and unsigned values of size 1, 2,
 * 4, and 8 bytes.
 *
 * The class can either be instantiated for sequential access to the byte[] array; or static methods
 * can be used to read values without the overhead of creating an instance.
 *
 * The flag {@code supportsUnalignedMemoryAccess} must be set according to the capabilities of the
 * hardware architecture: the value {@code true} allows more efficient memory access on
 * architectures that support unaligned memory accesses; the value {@code false} is the safe
 * fallback that works on every hardware.
 */
// @class UnsafeArrayTypeReader
public abstract class UnsafeArrayTypeReader implements TypeReader
{
    public static int getS1(byte[] data, long byteIndex)
    {
        return UnsafeAccess.UNSAFE.getByte(data, readOffset(data, byteIndex, Byte.BYTES));
    }

    public static int getU1(byte[] data, long byteIndex)
    {
        return UnsafeAccess.UNSAFE.getByte(data, readOffset(data, byteIndex, Byte.BYTES)) & 0xFF;
    }

    public static int getS2(byte[] data, long byteIndex, boolean supportsUnalignedMemoryAccess)
    {
        if (supportsUnalignedMemoryAccess)
        {
            return UnalignedUnsafeArrayTypeReader.getS2(data, byteIndex);
        }
        else
        {
            return AlignedUnsafeArrayTypeReader.getS2(data, byteIndex);
        }
    }

    public static int getU2(byte[] data, long byteIndex, boolean supportsUnalignedMemoryAccess)
    {
        return getS2(data, byteIndex, supportsUnalignedMemoryAccess) & 0xFFFF;
    }

    public static int getS4(byte[] data, long byteIndex, boolean supportsUnalignedMemoryAccess)
    {
        if (supportsUnalignedMemoryAccess)
        {
            return UnalignedUnsafeArrayTypeReader.getS4(data, byteIndex);
        }
        else
        {
            return AlignedUnsafeArrayTypeReader.getS4(data, byteIndex);
        }
    }

    public static long getU4(byte[] data, long byteIndex, boolean supportsUnalignedMemoryAccess)
    {
        return getS4(data, byteIndex, supportsUnalignedMemoryAccess) & 0xFFFFFFFFL;
    }

    public static long getS8(byte[] data, long byteIndex, boolean supportsUnalignedMemoryAccess)
    {
        if (supportsUnalignedMemoryAccess)
        {
            return UnalignedUnsafeArrayTypeReader.getS8(data, byteIndex);
        }
        else
        {
            return AlignedUnsafeArrayTypeReader.getS8(data, byteIndex);
        }
    }

    protected static long readOffset(byte[] data, long byteIndex, int numBytes)
    {
        return byteIndex + UnsafeAccess.UNSAFE.ARRAY_BYTE_BASE_OFFSET;
    }

    public static UnsafeArrayTypeReader create(byte[] data, long byteIndex, boolean supportsUnalignedMemoryAccess)
    {
        if (supportsUnalignedMemoryAccess)
        {
            return new UnalignedUnsafeArrayTypeReader(data, byteIndex);
        }
        else
        {
            return new AlignedUnsafeArrayTypeReader(data, byteIndex);
        }
    }

    protected final byte[] data;
    protected long byteIndex;

    // @cons
    protected UnsafeArrayTypeReader(byte[] data, long byteIndex)
    {
        super();
        this.data = data;
        this.byteIndex = byteIndex;
    }

    @Override
    public long getByteIndex()
    {
        return byteIndex;
    }

    @Override
    public void setByteIndex(long byteIndex)
    {
        this.byteIndex = byteIndex;
    }

    @Override
    public final int getS1()
    {
        int result = getS1(data, byteIndex);
        byteIndex += Byte.BYTES;
        return result;
    }

    @Override
    public final int getU1()
    {
        int result = getU1(data, byteIndex);
        byteIndex += Byte.BYTES;
        return result;
    }

    @Override
    public final int getU2()
    {
        return getS2() & 0xFFFF;
    }

    @Override
    public final long getU4()
    {
        return getS4() & 0xFFFFFFFFL;
    }
}

// @class UnalignedUnsafeArrayTypeReader
final class UnalignedUnsafeArrayTypeReader extends UnsafeArrayTypeReader
{
    protected static int getS2(byte[] data, long byteIndex)
    {
        return UnsafeAccess.UNSAFE.getShort(data, readOffset(data, byteIndex, Short.BYTES));
    }

    protected static int getS4(byte[] data, long byteIndex)
    {
        return UnsafeAccess.UNSAFE.getInt(data, readOffset(data, byteIndex, Integer.BYTES));
    }

    protected static long getS8(byte[] data, long byteIndex)
    {
        return UnsafeAccess.UNSAFE.getLong(data, readOffset(data, byteIndex, Long.BYTES));
    }

    // @cons
    protected UnalignedUnsafeArrayTypeReader(byte[] data, long byteIndex)
    {
        super(data, byteIndex);
    }

    @Override
    public int getS2()
    {
        int result = getS2(data, byteIndex);
        byteIndex += Short.BYTES;
        return result;
    }

    @Override
    public int getS4()
    {
        int result = getS4(data, byteIndex);
        byteIndex += Integer.BYTES;
        return result;
    }

    @Override
    public long getS8()
    {
        long result = getS8(data, byteIndex);
        byteIndex += Long.BYTES;
        return result;
    }
}

// @class AlignedUnsafeArrayTypeReader
final class AlignedUnsafeArrayTypeReader extends UnsafeArrayTypeReader
{
    protected static int getS2(byte[] data, long byteIndex)
    {
        long offset = readOffset(data, byteIndex, Short.BYTES);
        return ((UnsafeAccess.UNSAFE.getByte(data, offset + 0) & 0xFF) << 0) |
                (UnsafeAccess.UNSAFE.getByte(data, offset + 1) << 8);
    }

    protected static int getS4(byte[] data, long byteIndex)
    {
        long offset = readOffset(data, byteIndex, Integer.BYTES);
        return ((UnsafeAccess.UNSAFE.getByte(data, offset + 0) & 0xFF) << 0) |
               ((UnsafeAccess.UNSAFE.getByte(data, offset + 1) & 0xFF) << 8) |
               ((UnsafeAccess.UNSAFE.getByte(data, offset + 2) & 0xFF) << 16) |
                (UnsafeAccess.UNSAFE.getByte(data, offset + 3) << 24);
    }

    protected static long getS8(byte[] data, long byteIndex)
    {
        long offset = readOffset(data, byteIndex, Long.BYTES);
        return ((long) ((UnsafeAccess.UNSAFE.getByte(data, offset + 0) & 0xFF)) << 0) |
               ((long) ((UnsafeAccess.UNSAFE.getByte(data, offset + 1) & 0xFF)) << 8) |
               ((long) ((UnsafeAccess.UNSAFE.getByte(data, offset + 2) & 0xFF)) << 16) |
               ((long) ((UnsafeAccess.UNSAFE.getByte(data, offset + 3) & 0xFF)) << 24) |
               ((long) ((UnsafeAccess.UNSAFE.getByte(data, offset + 4) & 0xFF)) << 32) |
               ((long) ((UnsafeAccess.UNSAFE.getByte(data, offset + 5) & 0xFF)) << 40) |
               ((long) ((UnsafeAccess.UNSAFE.getByte(data, offset + 6) & 0xFF)) << 48) |
                ((long) (UnsafeAccess.UNSAFE.getByte(data, offset + 7)) << 56);
    }

    // @cons
    protected AlignedUnsafeArrayTypeReader(byte[] data, long byteIndex)
    {
        super(data, byteIndex);
    }

    @Override
    public int getS2()
    {
        int result = getS2(data, byteIndex);
        byteIndex += Short.BYTES;
        return result;
    }

    @Override
    public int getS4()
    {
        int result = getS4(data, byteIndex);
        byteIndex += Integer.BYTES;
        return result;
    }

    @Override
    public long getS8()
    {
        long result = getS8(data, byteIndex);
        byteIndex += Long.BYTES;
        return result;
    }
}
