package giraaff.lir.asm;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import giraaff.core.common.type.DataPointerConstant;

///
// Class for chunks of data that go into the data section.
///
// @class ArrayDataPointerConstant
public final class ArrayDataPointerConstant extends DataPointerConstant
{
    // @field
    private final byte[] ___data;

    // @cons ArrayDataPointerConstant
    public ArrayDataPointerConstant(byte[] __array, int __alignment)
    {
        super(__alignment);
        this.___data = __array.clone();
    }

    // @cons ArrayDataPointerConstant
    public ArrayDataPointerConstant(short[] __array, int __alignment)
    {
        super(__alignment);
        ByteBuffer __byteBuffer = ByteBuffer.allocate(__array.length * 2);
        __byteBuffer.order(ByteOrder.nativeOrder());
        __byteBuffer.asShortBuffer().put(__array);
        this.___data = __byteBuffer.array();
    }

    // @cons ArrayDataPointerConstant
    public ArrayDataPointerConstant(int[] __array, int __alignment)
    {
        super(__alignment);
        ByteBuffer __byteBuffer = ByteBuffer.allocate(__array.length * 4);
        __byteBuffer.order(ByteOrder.nativeOrder());
        __byteBuffer.asIntBuffer().put(__array);
        this.___data = __byteBuffer.array();
    }

    // @cons ArrayDataPointerConstant
    public ArrayDataPointerConstant(float[] __array, int __alignment)
    {
        super(__alignment);
        ByteBuffer __byteBuffer = ByteBuffer.allocate(__array.length * 4);
        __byteBuffer.order(ByteOrder.nativeOrder());
        __byteBuffer.asFloatBuffer().put(__array);
        this.___data = __byteBuffer.array();
    }

    // @cons ArrayDataPointerConstant
    public ArrayDataPointerConstant(double[] __array, int __alignment)
    {
        super(__alignment);
        ByteBuffer __byteBuffer = ByteBuffer.allocate(__array.length * 8);
        __byteBuffer.order(ByteOrder.nativeOrder());
        __byteBuffer.asDoubleBuffer().put(__array);
        this.___data = __byteBuffer.array();
    }

    // @cons ArrayDataPointerConstant
    public ArrayDataPointerConstant(long[] __array, int __alignment)
    {
        super(__alignment);
        ByteBuffer __byteBuffer = ByteBuffer.allocate(__array.length * 8);
        __byteBuffer.order(ByteOrder.nativeOrder());
        __byteBuffer.asLongBuffer().put(__array);
        this.___data = __byteBuffer.array();
    }

    @Override
    public boolean isDefaultForKind()
    {
        return false;
    }

    @Override
    public void serialize(ByteBuffer __buffer)
    {
        __buffer.put(this.___data);
    }

    @Override
    public int getSerializedSize()
    {
        return this.___data.length;
    }

    @Override
    public String toValueString()
    {
        return "ArrayDataPointerConstant" + Arrays.toString(this.___data);
    }
}
