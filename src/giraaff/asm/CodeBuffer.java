package giraaff.asm;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Code buffer management for the assembler.
 */
// @class CodeBuffer
final class CodeBuffer
{
    // @field
    protected ByteBuffer data;

    // @cons
    CodeBuffer(ByteOrder __order)
    {
        super();
        data = ByteBuffer.allocate(AsmOptions.InitialCodeBufferSize);
        data.order(__order);
    }

    public int position()
    {
        return data.position();
    }

    public void setPosition(int __position)
    {
        data.position(__position);
    }

    /**
     * Closes this buffer.
     * Any further operations on a closed buffer will result in a {@link NullPointerException}.
     *
     * @param trimmedCopy if {@code true}, then a copy of the underlying byte array up to (but
     *            not including) {@code position()} is returned
     * @return the data in this buffer or a trimmed copy if {@code trimmedCopy} is {@code true}
     */
    public byte[] close(boolean __trimmedCopy)
    {
        byte[] __result = data.array();
        if (__trimmedCopy)
        {
            // Make a copy even if result.length == data.position(),
            // since the API for trimmedCopy states a copy is always made.
            __result = Arrays.copyOf(__result, data.position());
        }
        data = null;
        return __result;
    }

    public byte[] copyData(int __start, int __end)
    {
        if (data == null)
        {
            return null;
        }
        return Arrays.copyOfRange(data.array(), __start, __end);
    }

    /**
     * Copies the data from this buffer into a given array.
     *
     * @param dst the destination array
     * @param off starting position in {@code dst}
     * @param len number of bytes to copy
     */
    public void copyInto(byte[] __dst, int __off, int __len)
    {
        System.arraycopy(data.array(), 0, __dst, __off, __len);
    }

    protected void ensureSize(int __length)
    {
        if (__length >= data.limit())
        {
            byte[] __newBuf = Arrays.copyOf(data.array(), __length * 4);
            ByteBuffer __newData = ByteBuffer.wrap(__newBuf);
            __newData.order(data.order());
            __newData.position(data.position());
            data = __newData;
        }
    }

    public void emitBytes(byte[] __arr, int __off, int __len)
    {
        ensureSize(data.position() + __len);
        data.put(__arr, __off, __len);
    }

    public void emitByte(int __b)
    {
        ensureSize(data.position() + 1);
        data.put((byte) (__b & 0xFF));
    }

    public void emitShort(int __b)
    {
        ensureSize(data.position() + 2);
        data.putShort((short) __b);
    }

    public void emitInt(int __b)
    {
        ensureSize(data.position() + 4);
        data.putInt(__b);
    }

    public void emitLong(long __b)
    {
        ensureSize(data.position() + 8);
        data.putLong(__b);
    }

    public void emitBytes(byte[] __arr, int __pos)
    {
        final int __len = __arr.length;
        ensureSize(__pos + __len);
        // Write directly into the underlying array so as to not change the ByteBuffer's position.
        System.arraycopy(__arr, 0, data.array(), __pos, __len);
    }

    public void emitByte(int __b, int __pos)
    {
        ensureSize(__pos + 1);
        data.put(__pos, (byte) (__b & 0xFF));
    }

    public void emitShort(int __b, int __pos)
    {
        ensureSize(__pos + 2);
        data.putShort(__pos, (short) __b).position();
    }

    public void emitInt(int __b, int __pos)
    {
        ensureSize(__pos + 4);
        data.putInt(__pos, __b).position();
    }

    public void emitLong(long __b, int __pos)
    {
        ensureSize(__pos + 8);
        data.putLong(__pos, __b).position();
    }

    public int getByte(int __pos)
    {
        int __b = data.get(__pos);
        return __b & 0xff;
    }

    public int getShort(int __pos)
    {
        short __s = data.getShort(__pos);
        return __s & 0xffff;
    }

    public int getInt(int __pos)
    {
        return data.getInt(__pos);
    }

    public void reset()
    {
        data.clear();
    }
}
