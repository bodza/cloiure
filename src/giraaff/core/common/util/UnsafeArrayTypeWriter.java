package giraaff.core.common.util;

import giraaff.core.common.util.TypeConversion;
import giraaff.util.UnsafeAccess;

/**
 * Provides low-level sequential write access to a byte[] array for signed and unsigned values of
 * size 1, 2, 4, and 8 bytes. To avoid copying an array when the buffer size is no longer
 * sufficient, the buffer is split into chunks of a fixed size.
 *
 * The flag {@code supportsUnalignedMemoryAccess} must be set according to the capabilities of the
 * hardware architecture: the value {@code true} allows more efficient memory access on
 * architectures that support unaligned memory accesses; the value {@code false} is the safe
 * fallback that works on every hardware.
 */
// @class UnsafeArrayTypeWriter
public abstract class UnsafeArrayTypeWriter implements TypeWriter
{
    // @def
    private static final int MIN_CHUNK_LENGTH = 200;
    // @def
    private static final int MAX_CHUNK_LENGTH = 16000;

    // @class UnsafeArrayTypeWriter.Chunk
    static final class Chunk
    {
        // @field
        protected final byte[] data;
        // @field
        protected int size;
        // @field
        protected Chunk next;

        // @cons
        protected Chunk(int __arrayLength)
        {
            super();
            data = new byte[__arrayLength];
        }
    }

    // @field
    protected final Chunk firstChunk;
    // @field
    protected Chunk writeChunk;
    // @field
    protected int totalSize;

    public static UnsafeArrayTypeWriter create(boolean __supportsUnalignedMemoryAccess)
    {
        if (__supportsUnalignedMemoryAccess)
        {
            return new UnalignedUnsafeArrayTypeWriter();
        }
        else
        {
            return new AlignedUnsafeArrayTypeWriter();
        }
    }

    // @cons
    protected UnsafeArrayTypeWriter()
    {
        super();
        firstChunk = new Chunk(MIN_CHUNK_LENGTH);
        writeChunk = firstChunk;
    }

    @Override
    public final long getBytesWritten()
    {
        return totalSize;
    }

    /**
     * Copies the buffer into the provided byte[] array of length {@link #getBytesWritten()}.
     */
    public final byte[] toArray(byte[] __result)
    {
        int __resultIdx = 0;
        for (Chunk __cur = firstChunk; __cur != null; __cur = __cur.next)
        {
            System.arraycopy(__cur.data, 0, __result, __resultIdx, __cur.size);
            __resultIdx += __cur.size;
        }
        return __result;
    }

    @Override
    public final void putS1(long __value)
    {
        long __offset = writeOffset(Byte.BYTES);
        UnsafeAccess.UNSAFE.putByte(writeChunk.data, __offset, TypeConversion.asS1(__value));
    }

    @Override
    public final void putU1(long __value)
    {
        long __offset = writeOffset(Byte.BYTES);
        UnsafeAccess.UNSAFE.putByte(writeChunk.data, __offset, TypeConversion.asU1(__value));
    }

    @Override
    public final void putU2(long __value)
    {
        putS2(TypeConversion.asU2(__value));
    }

    @Override
    public final void putU4(long __value)
    {
        putS4(TypeConversion.asU4(__value));
    }

    protected long writeOffset(int __writeBytes)
    {
        if (writeChunk.size + __writeBytes >= writeChunk.data.length)
        {
            Chunk __newChunk = new Chunk(Math.min(writeChunk.data.length * 2, MAX_CHUNK_LENGTH));
            writeChunk.next = __newChunk;
            writeChunk = __newChunk;
        }

        long __result = writeChunk.size + UnsafeAccess.UNSAFE.ARRAY_BYTE_BASE_OFFSET;

        totalSize += __writeBytes;
        writeChunk.size += __writeBytes;

        return __result;
    }
}

// @class UnalignedUnsafeArrayTypeWriter
final class UnalignedUnsafeArrayTypeWriter extends UnsafeArrayTypeWriter
{
    @Override
    public void putS2(long __value)
    {
        long __offset = writeOffset(Short.BYTES);
        UnsafeAccess.UNSAFE.putShort(writeChunk.data, __offset, TypeConversion.asS2(__value));
    }

    @Override
    public void putS4(long __value)
    {
        long __offset = writeOffset(Integer.BYTES);
        UnsafeAccess.UNSAFE.putInt(writeChunk.data, __offset, TypeConversion.asS4(__value));
    }

    @Override
    public void putS8(long __value)
    {
        long __offset = writeOffset(Long.BYTES);
        UnsafeAccess.UNSAFE.putLong(writeChunk.data, __offset, __value);
    }
}

// @class AlignedUnsafeArrayTypeWriter
final class AlignedUnsafeArrayTypeWriter extends UnsafeArrayTypeWriter
{
    @Override
    public void putS2(long __value)
    {
        long __offset = writeOffset(Short.BYTES);
        UnsafeAccess.UNSAFE.putByte(writeChunk.data, __offset + 0, (byte) (__value >> 0));
        UnsafeAccess.UNSAFE.putByte(writeChunk.data, __offset + 1, (byte) (__value >> 8));
    }

    @Override
    public void putS4(long __value)
    {
        long __offset = writeOffset(Integer.BYTES);
        UnsafeAccess.UNSAFE.putByte(writeChunk.data, __offset + 0, (byte) (__value >> 0));
        UnsafeAccess.UNSAFE.putByte(writeChunk.data, __offset + 1, (byte) (__value >> 8));
        UnsafeAccess.UNSAFE.putByte(writeChunk.data, __offset + 2, (byte) (__value >> 16));
        UnsafeAccess.UNSAFE.putByte(writeChunk.data, __offset + 3, (byte) (__value >> 24));
    }

    @Override
    public void putS8(long __value)
    {
        long __offset = writeOffset(Long.BYTES);
        UnsafeAccess.UNSAFE.putByte(writeChunk.data, __offset + 0, (byte) (__value >> 0));
        UnsafeAccess.UNSAFE.putByte(writeChunk.data, __offset + 1, (byte) (__value >> 8));
        UnsafeAccess.UNSAFE.putByte(writeChunk.data, __offset + 2, (byte) (__value >> 16));
        UnsafeAccess.UNSAFE.putByte(writeChunk.data, __offset + 3, (byte) (__value >> 24));
        UnsafeAccess.UNSAFE.putByte(writeChunk.data, __offset + 4, (byte) (__value >> 32));
        UnsafeAccess.UNSAFE.putByte(writeChunk.data, __offset + 5, (byte) (__value >> 40));
        UnsafeAccess.UNSAFE.putByte(writeChunk.data, __offset + 6, (byte) (__value >> 48));
        UnsafeAccess.UNSAFE.putByte(writeChunk.data, __offset + 7, (byte) (__value >> 56));
    }
}
