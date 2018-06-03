package giraaff.code;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.BiConsumer;

import jdk.vm.ci.code.site.DataSectionReference;
import jdk.vm.ci.meta.MetaUtil;
import jdk.vm.ci.meta.SerializableConstant;
import jdk.vm.ci.meta.VMConstant;

import giraaff.code.DataSection.Data;

// @class DataSection
public final class DataSection implements Iterable<Data>
{
    // @iface DataSection.Patches
    public interface Patches
    {
        void registerPatch(int position, VMConstant c);
    }

    // @class DataSection.Data
    public abstract static class Data
    {
        // @field
        private int alignment;
        // @field
        private final int size;

        // @field
        private DataSectionReference ref;

        // @cons
        protected Data(int __alignment, int __size)
        {
            super();
            this.alignment = __alignment;
            this.size = __size;

            // initialized in DataSection.insertData(Data)
            ref = null;
        }

        protected abstract void emit(ByteBuffer buffer, Patches patches);

        public void updateAlignment(int __newAlignment)
        {
            if (__newAlignment == alignment)
            {
                return;
            }
            alignment = lcm(alignment, __newAlignment);
        }

        public int getAlignment()
        {
            return alignment;
        }

        public int getSize()
        {
            return size;
        }

        @Override
        public int hashCode()
        {
            // Data instances should not be used as hash map keys
            throw new UnsupportedOperationException("hashCode");
        }

        @Override
        public boolean equals(Object __obj)
        {
            if (__obj == this)
            {
                return true;
            }
            if (__obj instanceof Data)
            {
                Data __that = (Data) __obj;
                if (this.alignment == __that.alignment && this.size == __that.size && this.ref.equals(__that.ref))
                {
                    return true;
                }
            }
            return false;
        }
    }

    // @class DataSection.RawData
    public static final class RawData extends Data
    {
        // @field
        private final byte[] data;

        // @cons
        public RawData(byte[] __data, int __alignment)
        {
            super(__alignment, __data.length);
            this.data = __data;
        }

        @Override
        protected void emit(ByteBuffer __buffer, Patches __patches)
        {
            __buffer.put(data);
        }
    }

    // @class DataSection.SerializableData
    public static final class SerializableData extends Data
    {
        // @field
        private final SerializableConstant constant;

        // @cons
        public SerializableData(SerializableConstant __constant)
        {
            this(__constant, 1);
        }

        // @cons
        public SerializableData(SerializableConstant __constant, int __alignment)
        {
            super(__alignment, __constant.getSerializedSize());
            this.constant = __constant;
        }

        @Override
        protected void emit(ByteBuffer __buffer, Patches __patches)
        {
            int __position = __buffer.position();
            constant.serialize(__buffer);
        }
    }

    // @class DataSection.ZeroData
    public static class ZeroData extends Data
    {
        // @cons
        protected ZeroData(int __alignment, int __size)
        {
            super(__alignment, __size);
        }

        public static ZeroData create(int __alignment, int __size)
        {
            switch (__size)
            {
                case 1:
                    // @closure
                    return new ZeroData(__alignment, __size)
                    {
                        @Override
                        protected void emit(ByteBuffer __buffer, Patches __patches)
                        {
                            __buffer.put((byte) 0);
                        }
                    };

                case 2:
                    // @closure
                    return new ZeroData(__alignment, __size)
                    {
                        @Override
                        protected void emit(ByteBuffer __buffer, Patches __patches)
                        {
                            __buffer.putShort((short) 0);
                        }
                    };

                case 4:
                    // @closure
                    return new ZeroData(__alignment, __size)
                    {
                        @Override
                        protected void emit(ByteBuffer __buffer, Patches __patches)
                        {
                            __buffer.putInt(0);
                        }
                    };

                case 8:
                    // @closure
                    return new ZeroData(__alignment, __size)
                    {
                        @Override
                        protected void emit(ByteBuffer __buffer, Patches __patches)
                        {
                            __buffer.putLong(0);
                        }
                    };

                default:
                    return new ZeroData(__alignment, __size);
            }
        }

        @Override
        protected void emit(ByteBuffer __buffer, Patches __patches)
        {
            int __rest = getSize();
            while (__rest > 8)
            {
                __buffer.putLong(0L);
                __rest -= 8;
            }
            while (__rest > 0)
            {
                __buffer.put((byte) 0);
                __rest--;
            }
        }
    }

    // @class DataSection.PackedData
    public static final class PackedData extends Data
    {
        // @field
        private final Data[] nested;

        // @cons
        private PackedData(int __alignment, int __size, Data[] __nested)
        {
            super(__alignment, __size);
            this.nested = __nested;
        }

        public static PackedData create(Data[] __nested)
        {
            int __size = 0;
            int __alignment = 1;
            for (int __i = 0; __i < __nested.length; __i++)
            {
                __alignment = DataSection.lcm(__alignment, __nested[__i].getAlignment());
                __size += __nested[__i].getSize();
            }
            return new PackedData(__alignment, __size, __nested);
        }

        @Override
        protected void emit(ByteBuffer __buffer, Patches __patches)
        {
            for (Data __data : nested)
            {
                __data.emit(__buffer, __patches);
            }
        }
    }

    // @field
    private final ArrayList<Data> dataItems = new ArrayList<>();

    // @field
    private boolean closed;
    // @field
    private int sectionAlignment;
    // @field
    private int sectionSize;

    @Override
    public int hashCode()
    {
        // DataSection instances should not be used as hash map keys
        throw new UnsupportedOperationException("hashCode");
    }

    @Override
    public boolean equals(Object __obj)
    {
        if (this == __obj)
        {
            return true;
        }
        if (__obj instanceof DataSection)
        {
            DataSection __that = (DataSection) __obj;
            if (this.closed == __that.closed && this.sectionAlignment == __that.sectionAlignment && this.sectionSize == __that.sectionSize && Objects.equals(this.dataItems, __that.dataItems))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Inserts a {@link Data} item into the data section. If the item is already in the data
     * section, the same {@link DataSectionReference} is returned.
     *
     * @param data the {@link Data} item to be inserted
     * @return a unique {@link DataSectionReference} identifying the {@link Data} item
     */
    public DataSectionReference insertData(Data __data)
    {
        checkOpen();
        synchronized (__data)
        {
            if (__data.ref == null)
            {
                __data.ref = new DataSectionReference();
                dataItems.add(__data);
            }
            return __data.ref;
        }
    }

    /**
     * Transfers all {@link Data} from the provided other {@link DataSection} to this
     * {@link DataSection}, and empties the other section.
     */
    public void addAll(DataSection __other)
    {
        checkOpen();
        __other.checkOpen();

        for (Data __data : __other.dataItems)
        {
            dataItems.add(__data);
        }
        __other.dataItems.clear();
    }

    /**
     * Determines if this object has been {@link #close() closed}.
     */
    public boolean closed()
    {
        return closed;
    }

    /**
     * Computes the layout of the data section and closes this object to further updates.
     *
     * This must be called exactly once.
     */
    public void close()
    {
        checkOpen();
        closed = true;

        // simple heuristic: put items with larger alignment requirement first
        dataItems.sort((__a, __b) -> __a.alignment - __b.alignment);

        int __position = 0;
        int __alignment = 1;
        for (Data __d : dataItems)
        {
            __alignment = lcm(__alignment, __d.alignment);
            __position = align(__position, __d.alignment);

            __d.ref.setOffset(__position);
            __position += __d.size;
        }

        sectionAlignment = __alignment;
        sectionSize = __position;
    }

    /**
     * Gets the size of the data section.
     *
     * This must only be called once this object has been {@linkplain #closed() closed}.
     */
    public int getSectionSize()
    {
        checkClosed();
        return sectionSize;
    }

    /**
     * Gets the minimum alignment requirement of the data section.
     *
     * This must only be called once this object has been {@linkplain #closed() closed}.
     */
    public int getSectionAlignment()
    {
        checkClosed();
        return sectionAlignment;
    }

    /**
     * Builds the data section into a given buffer.
     *
     * This must only be called once this object has been {@linkplain #closed() closed}.
     *
     * @param buffer the {@link ByteBuffer} where the data section should be built. The buffer must
     *            hold at least {@link #getSectionSize()} bytes.
     * @param patch a {@link Patches} instance to receive {@link VMConstant constants} for
     *            relocations in the data section
     */
    public void buildDataSection(ByteBuffer __buffer, Patches __patch)
    {
        buildDataSection(__buffer, __patch, (__r, __s) -> {});
    }

    /**
     * Builds the data section into a given buffer.
     *
     * This must only be called once this object has been {@linkplain #closed() closed}. When this
     * method returns, the buffers' position is just after the last data item.
     *
     * @param buffer the {@link ByteBuffer} where the data section should be built. The buffer must
     *            hold at least {@link #getSectionSize()} bytes.
     * @param patch a {@link Patches} instance to receive {@link VMConstant constants} for
     * @param onEmit a function that is called before emitting each data item with the
     *            {@link DataSectionReference} and the size of the data.
     */
    public void buildDataSection(ByteBuffer __buffer, Patches __patch, BiConsumer<DataSectionReference, Integer> __onEmit)
    {
        checkClosed();
        int __start = __buffer.position();
        for (Data __d : dataItems)
        {
            __buffer.position(__start + __d.ref.getOffset());
            __onEmit.accept(__d.ref, __d.getSize());
            __d.emit(__buffer, __patch);
        }
        __buffer.position(__start + sectionSize);
    }

    public Data findData(DataSectionReference __ref)
    {
        for (Data __d : dataItems)
        {
            if (__d.ref == __ref)
            {
                return __d;
            }
        }
        return null;
    }

    public static void emit(ByteBuffer __buffer, Data __data, Patches __patch)
    {
        __data.emit(__buffer, __patch);
    }

    @Override
    public Iterator<Data> iterator()
    {
        return dataItems.iterator();
    }

    private static int lcm(int __x, int __y)
    {
        if (__x == 0)
        {
            return __y;
        }
        else if (__y == 0)
        {
            return __x;
        }

        int __a = Math.max(__x, __y);
        int __b = Math.min(__x, __y);
        while (__b > 0)
        {
            int __tmp = __a % __b;
            __a = __b;
            __b = __tmp;
        }

        int __gcd = __a;
        return __x * __y / __gcd;
    }

    private static int align(int __position, int __alignment)
    {
        return ((__position + __alignment - 1) / __alignment) * __alignment;
    }

    private void checkClosed()
    {
        if (!closed)
        {
            throw new IllegalStateException();
        }
    }

    private void checkOpen()
    {
        if (closed)
        {
            throw new IllegalStateException();
        }
    }

    public void clear()
    {
        checkOpen();
        this.dataItems.clear();
        this.sectionAlignment = 0;
        this.sectionSize = 0;
    }
}
