package giraaff.core.common.util;

import giraaff.core.common.util.TypeConversion;
import giraaff.util.UnsafeAccess;

///
// Provides low-level sequential write access to a byte[] array for signed and unsigned values of
// size 1, 2, 4, and 8 bytes. To avoid copying an array when the buffer size is no longer
// sufficient, the buffer is split into chunks of a fixed size.
//
// The flag {@code supportsUnalignedMemoryAccess} must be set according to the capabilities of the
// hardware architecture: the value {@code true} allows more efficient memory access on
// architectures that support unaligned memory accesses; the value {@code false} is the safe
// fallback that works on every hardware.
///
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
        protected final byte[] ___data;
        // @field
        protected int ___size;
        // @field
        protected UnsafeArrayTypeWriter.Chunk ___next;

        // @cons UnsafeArrayTypeWriter.Chunk
        protected Chunk(int __arrayLength)
        {
            super();
            this.___data = new byte[__arrayLength];
        }
    }

    // @field
    protected final UnsafeArrayTypeWriter.Chunk ___firstChunk;
    // @field
    protected UnsafeArrayTypeWriter.Chunk ___writeChunk;
    // @field
    protected int ___totalSize;

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

    // @cons UnsafeArrayTypeWriter
    protected UnsafeArrayTypeWriter()
    {
        super();
        this.___firstChunk = new UnsafeArrayTypeWriter.Chunk(MIN_CHUNK_LENGTH);
        this.___writeChunk = this.___firstChunk;
    }

    @Override
    public final long getBytesWritten()
    {
        return this.___totalSize;
    }

    ///
    // Copies the buffer into the provided byte[] array of length {@link #getBytesWritten()}.
    ///
    public final byte[] toArray(byte[] __result)
    {
        int __resultIdx = 0;
        for (UnsafeArrayTypeWriter.Chunk __cur = this.___firstChunk; __cur != null; __cur = __cur.___next)
        {
            System.arraycopy(__cur.___data, 0, __result, __resultIdx, __cur.___size);
            __resultIdx += __cur.___size;
        }
        return __result;
    }

    @Override
    public final void putS1(long __value)
    {
        long __offset = writeOffset(Byte.BYTES);
        UnsafeAccess.UNSAFE.putByte(this.___writeChunk.___data, __offset, TypeConversion.asS1(__value));
    }

    @Override
    public final void putU1(long __value)
    {
        long __offset = writeOffset(Byte.BYTES);
        UnsafeAccess.UNSAFE.putByte(this.___writeChunk.___data, __offset, TypeConversion.asU1(__value));
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
        if (this.___writeChunk.___size + __writeBytes >= this.___writeChunk.___data.length)
        {
            UnsafeArrayTypeWriter.Chunk __newChunk = new UnsafeArrayTypeWriter.Chunk(Math.min(this.___writeChunk.___data.length * 2, MAX_CHUNK_LENGTH));
            this.___writeChunk.___next = __newChunk;
            this.___writeChunk = __newChunk;
        }

        long __result = this.___writeChunk.___size + UnsafeAccess.UNSAFE.ARRAY_BYTE_BASE_OFFSET;

        this.___totalSize += __writeBytes;
        this.___writeChunk.___size += __writeBytes;

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
        UnsafeAccess.UNSAFE.putShort(this.___writeChunk.___data, __offset, TypeConversion.asS2(__value));
    }

    @Override
    public void putS4(long __value)
    {
        long __offset = writeOffset(Integer.BYTES);
        UnsafeAccess.UNSAFE.putInt(this.___writeChunk.___data, __offset, TypeConversion.asS4(__value));
    }

    @Override
    public void putS8(long __value)
    {
        long __offset = writeOffset(Long.BYTES);
        UnsafeAccess.UNSAFE.putLong(this.___writeChunk.___data, __offset, __value);
    }
}

// @class AlignedUnsafeArrayTypeWriter
final class AlignedUnsafeArrayTypeWriter extends UnsafeArrayTypeWriter
{
    @Override
    public void putS2(long __value)
    {
        long __offset = writeOffset(Short.BYTES);
        UnsafeAccess.UNSAFE.putByte(this.___writeChunk.___data, __offset + 0, (byte) (__value >> 0));
        UnsafeAccess.UNSAFE.putByte(this.___writeChunk.___data, __offset + 1, (byte) (__value >> 8));
    }

    @Override
    public void putS4(long __value)
    {
        long __offset = writeOffset(Integer.BYTES);
        UnsafeAccess.UNSAFE.putByte(this.___writeChunk.___data, __offset + 0, (byte) (__value >> 0));
        UnsafeAccess.UNSAFE.putByte(this.___writeChunk.___data, __offset + 1, (byte) (__value >> 8));
        UnsafeAccess.UNSAFE.putByte(this.___writeChunk.___data, __offset + 2, (byte) (__value >> 16));
        UnsafeAccess.UNSAFE.putByte(this.___writeChunk.___data, __offset + 3, (byte) (__value >> 24));
    }

    @Override
    public void putS8(long __value)
    {
        long __offset = writeOffset(Long.BYTES);
        UnsafeAccess.UNSAFE.putByte(this.___writeChunk.___data, __offset + 0, (byte) (__value >> 0));
        UnsafeAccess.UNSAFE.putByte(this.___writeChunk.___data, __offset + 1, (byte) (__value >> 8));
        UnsafeAccess.UNSAFE.putByte(this.___writeChunk.___data, __offset + 2, (byte) (__value >> 16));
        UnsafeAccess.UNSAFE.putByte(this.___writeChunk.___data, __offset + 3, (byte) (__value >> 24));
        UnsafeAccess.UNSAFE.putByte(this.___writeChunk.___data, __offset + 4, (byte) (__value >> 32));
        UnsafeAccess.UNSAFE.putByte(this.___writeChunk.___data, __offset + 5, (byte) (__value >> 40));
        UnsafeAccess.UNSAFE.putByte(this.___writeChunk.___data, __offset + 6, (byte) (__value >> 48));
        UnsafeAccess.UNSAFE.putByte(this.___writeChunk.___data, __offset + 7, (byte) (__value >> 56));
    }
}
