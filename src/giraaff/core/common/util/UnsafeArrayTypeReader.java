package giraaff.core.common.util;

import giraaff.util.UnsafeAccess;

///
// Provides low-level read access from a byte[] array for signed and unsigned values of size 1, 2,
// 4, and 8 bytes.
//
// The class can either be instantiated for sequential access to the byte[] array; or static methods
// can be used to read values without the overhead of creating an instance.
//
// The flag {@code supportsUnalignedMemoryAccess} must be set according to the capabilities of the
// hardware architecture: the value {@code true} allows more efficient memory access on
// architectures that support unaligned memory accesses; the value {@code false} is the safe
// fallback that works on every hardware.
///
// @class UnsafeArrayTypeReader
public abstract class UnsafeArrayTypeReader implements TypeReader
{
    public static int getS1(byte[] __data, long __byteIndex)
    {
        return UnsafeAccess.UNSAFE.getByte(__data, readOffset(__data, __byteIndex, Byte.BYTES));
    }

    public static int getU1(byte[] __data, long __byteIndex)
    {
        return UnsafeAccess.UNSAFE.getByte(__data, readOffset(__data, __byteIndex, Byte.BYTES)) & 0xFF;
    }

    public static int getS2(byte[] __data, long __byteIndex, boolean __supportsUnalignedMemoryAccess)
    {
        if (__supportsUnalignedMemoryAccess)
        {
            return UnalignedUnsafeArrayTypeReader.getS2(__data, __byteIndex);
        }
        else
        {
            return AlignedUnsafeArrayTypeReader.getS2(__data, __byteIndex);
        }
    }

    public static int getU2(byte[] __data, long __byteIndex, boolean __supportsUnalignedMemoryAccess)
    {
        return getS2(__data, __byteIndex, __supportsUnalignedMemoryAccess) & 0xFFFF;
    }

    public static int getS4(byte[] __data, long __byteIndex, boolean __supportsUnalignedMemoryAccess)
    {
        if (__supportsUnalignedMemoryAccess)
        {
            return UnalignedUnsafeArrayTypeReader.getS4(__data, __byteIndex);
        }
        else
        {
            return AlignedUnsafeArrayTypeReader.getS4(__data, __byteIndex);
        }
    }

    public static long getU4(byte[] __data, long __byteIndex, boolean __supportsUnalignedMemoryAccess)
    {
        return getS4(__data, __byteIndex, __supportsUnalignedMemoryAccess) & 0xFFFFFFFFL;
    }

    public static long getS8(byte[] __data, long __byteIndex, boolean __supportsUnalignedMemoryAccess)
    {
        if (__supportsUnalignedMemoryAccess)
        {
            return UnalignedUnsafeArrayTypeReader.getS8(__data, __byteIndex);
        }
        else
        {
            return AlignedUnsafeArrayTypeReader.getS8(__data, __byteIndex);
        }
    }

    protected static long readOffset(byte[] __data, long __byteIndex, int __numBytes)
    {
        return __byteIndex + UnsafeAccess.UNSAFE.ARRAY_BYTE_BASE_OFFSET;
    }

    public static UnsafeArrayTypeReader create(byte[] __data, long __byteIndex, boolean __supportsUnalignedMemoryAccess)
    {
        if (__supportsUnalignedMemoryAccess)
        {
            return new UnalignedUnsafeArrayTypeReader(__data, __byteIndex);
        }
        else
        {
            return new AlignedUnsafeArrayTypeReader(__data, __byteIndex);
        }
    }

    // @field
    protected final byte[] ___data;
    // @field
    protected long ___byteIndex;

    // @cons UnsafeArrayTypeReader
    protected UnsafeArrayTypeReader(byte[] __data, long __byteIndex)
    {
        super();
        this.___data = __data;
        this.___byteIndex = __byteIndex;
    }

    @Override
    public long getByteIndex()
    {
        return this.___byteIndex;
    }

    @Override
    public void setByteIndex(long __byteIndex)
    {
        this.___byteIndex = __byteIndex;
    }

    @Override
    public final int getS1()
    {
        int __result = getS1(this.___data, this.___byteIndex);
        this.___byteIndex += Byte.BYTES;
        return __result;
    }

    @Override
    public final int getU1()
    {
        int __result = getU1(this.___data, this.___byteIndex);
        this.___byteIndex += Byte.BYTES;
        return __result;
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
    protected static int getS2(byte[] __data, long __byteIndex)
    {
        return UnsafeAccess.UNSAFE.getShort(__data, readOffset(__data, __byteIndex, Short.BYTES));
    }

    protected static int getS4(byte[] __data, long __byteIndex)
    {
        return UnsafeAccess.UNSAFE.getInt(__data, readOffset(__data, __byteIndex, Integer.BYTES));
    }

    protected static long getS8(byte[] __data, long __byteIndex)
    {
        return UnsafeAccess.UNSAFE.getLong(__data, readOffset(__data, __byteIndex, Long.BYTES));
    }

    // @cons UnalignedUnsafeArrayTypeReader
    protected UnalignedUnsafeArrayTypeReader(byte[] __data, long __byteIndex)
    {
        super(__data, __byteIndex);
    }

    @Override
    public int getS2()
    {
        int __result = getS2(this.___data, this.___byteIndex);
        this.___byteIndex += Short.BYTES;
        return __result;
    }

    @Override
    public int getS4()
    {
        int __result = getS4(this.___data, this.___byteIndex);
        this.___byteIndex += Integer.BYTES;
        return __result;
    }

    @Override
    public long getS8()
    {
        long __result = getS8(this.___data, this.___byteIndex);
        this.___byteIndex += Long.BYTES;
        return __result;
    }
}

// @class AlignedUnsafeArrayTypeReader
final class AlignedUnsafeArrayTypeReader extends UnsafeArrayTypeReader
{
    protected static int getS2(byte[] __data, long __byteIndex)
    {
        long __offset = readOffset(__data, __byteIndex, Short.BYTES);
        return ((UnsafeAccess.UNSAFE.getByte(__data, __offset + 0) & 0xFF) << 0) |
                (UnsafeAccess.UNSAFE.getByte(__data, __offset + 1) << 8);
    }

    protected static int getS4(byte[] __data, long __byteIndex)
    {
        long __offset = readOffset(__data, __byteIndex, Integer.BYTES);
        return ((UnsafeAccess.UNSAFE.getByte(__data, __offset + 0) & 0xFF) << 0) |
               ((UnsafeAccess.UNSAFE.getByte(__data, __offset + 1) & 0xFF) << 8) |
               ((UnsafeAccess.UNSAFE.getByte(__data, __offset + 2) & 0xFF) << 16) |
                (UnsafeAccess.UNSAFE.getByte(__data, __offset + 3) << 24);
    }

    protected static long getS8(byte[] __data, long __byteIndex)
    {
        long __offset = readOffset(__data, __byteIndex, Long.BYTES);
        return ((long) ((UnsafeAccess.UNSAFE.getByte(__data, __offset + 0) & 0xFF)) << 0) |
               ((long) ((UnsafeAccess.UNSAFE.getByte(__data, __offset + 1) & 0xFF)) << 8) |
               ((long) ((UnsafeAccess.UNSAFE.getByte(__data, __offset + 2) & 0xFF)) << 16) |
               ((long) ((UnsafeAccess.UNSAFE.getByte(__data, __offset + 3) & 0xFF)) << 24) |
               ((long) ((UnsafeAccess.UNSAFE.getByte(__data, __offset + 4) & 0xFF)) << 32) |
               ((long) ((UnsafeAccess.UNSAFE.getByte(__data, __offset + 5) & 0xFF)) << 40) |
               ((long) ((UnsafeAccess.UNSAFE.getByte(__data, __offset + 6) & 0xFF)) << 48) |
                ((long) (UnsafeAccess.UNSAFE.getByte(__data, __offset + 7)) << 56);
    }

    // @cons AlignedUnsafeArrayTypeReader
    protected AlignedUnsafeArrayTypeReader(byte[] __data, long __byteIndex)
    {
        super(__data, __byteIndex);
    }

    @Override
    public int getS2()
    {
        int __result = getS2(this.___data, this.___byteIndex);
        this.___byteIndex += Short.BYTES;
        return __result;
    }

    @Override
    public int getS4()
    {
        int __result = getS4(this.___data, this.___byteIndex);
        this.___byteIndex += Integer.BYTES;
        return __result;
    }

    @Override
    public long getS8()
    {
        long __result = getS8(this.___data, this.___byteIndex);
        this.___byteIndex += Long.BYTES;
        return __result;
    }
}
