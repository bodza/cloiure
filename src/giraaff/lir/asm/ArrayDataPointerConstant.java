package giraaff.lir.asm;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import giraaff.core.common.type.DataPointerConstant;

/**
 * Class for chunks of data that go into the data section.
 */
// @class ArrayDataPointerConstant
public final class ArrayDataPointerConstant extends DataPointerConstant
{
    private final byte[] data;

    // @cons
    public ArrayDataPointerConstant(byte[] array, int alignment)
    {
        super(alignment);
        data = array.clone();
    }

    // @cons
    public ArrayDataPointerConstant(short[] array, int alignment)
    {
        super(alignment);
        ByteBuffer byteBuffer = ByteBuffer.allocate(array.length * 2);
        byteBuffer.order(ByteOrder.nativeOrder());
        byteBuffer.asShortBuffer().put(array);
        data = byteBuffer.array();
    }

    // @cons
    public ArrayDataPointerConstant(int[] array, int alignment)
    {
        super(alignment);
        ByteBuffer byteBuffer = ByteBuffer.allocate(array.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        byteBuffer.asIntBuffer().put(array);
        data = byteBuffer.array();
    }

    // @cons
    public ArrayDataPointerConstant(float[] array, int alignment)
    {
        super(alignment);
        ByteBuffer byteBuffer = ByteBuffer.allocate(array.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        byteBuffer.asFloatBuffer().put(array);
        data = byteBuffer.array();
    }

    // @cons
    public ArrayDataPointerConstant(double[] array, int alignment)
    {
        super(alignment);
        ByteBuffer byteBuffer = ByteBuffer.allocate(array.length * 8);
        byteBuffer.order(ByteOrder.nativeOrder());
        byteBuffer.asDoubleBuffer().put(array);
        data = byteBuffer.array();
    }

    // @cons
    public ArrayDataPointerConstant(long[] array, int alignment)
    {
        super(alignment);
        ByteBuffer byteBuffer = ByteBuffer.allocate(array.length * 8);
        byteBuffer.order(ByteOrder.nativeOrder());
        byteBuffer.asLongBuffer().put(array);
        data = byteBuffer.array();
    }

    @Override
    public boolean isDefaultForKind()
    {
        return false;
    }

    @Override
    public void serialize(ByteBuffer buffer)
    {
        buffer.put(data);
    }

    @Override
    public int getSerializedSize()
    {
        return data.length;
    }

    @Override
    public String toValueString()
    {
        return "ArrayDataPointerConstant" + Arrays.toString(data);
    }
}
