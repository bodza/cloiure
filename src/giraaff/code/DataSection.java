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

import giraaff.code.DataSection;

// @class DataSection
public final class DataSection implements Iterable<DataSection.Data>
{
    // @iface DataSection.Patches
    public interface Patches
    {
        void registerPatch(int __position, VMConstant __c);
    }

    // @class DataSection.Data
    public abstract static class Data
    {
        // @field
        private int ___alignment;
        // @field
        private final int ___size;

        // @field
        private DataSectionReference ___ref;

        // @cons DataSection.Data
        protected Data(int __alignment, int __size)
        {
            super();
            this.___alignment = __alignment;
            this.___size = __size;

            // initialized in DataSection.insertData(DataSection.Data)
            this.___ref = null;
        }

        protected abstract void emit(ByteBuffer __buffer, DataSection.Patches __patches);

        public void updateAlignment(int __newAlignment)
        {
            if (__newAlignment == this.___alignment)
            {
                return;
            }
            this.___alignment = lcm(this.___alignment, __newAlignment);
        }

        public int getAlignment()
        {
            return this.___alignment;
        }

        public int getSize()
        {
            return this.___size;
        }

        @Override
        public int hashCode()
        {
            // DataSection.Data instances should not be used as hash map keys
            throw new UnsupportedOperationException("hashCode");
        }

        @Override
        public boolean equals(Object __obj)
        {
            if (__obj == this)
            {
                return true;
            }
            if (__obj instanceof DataSection.Data)
            {
                DataSection.Data __that = (DataSection.Data) __obj;
                if (this.___alignment == __that.___alignment && this.___size == __that.___size && this.___ref.equals(__that.___ref))
                {
                    return true;
                }
            }
            return false;
        }
    }

    // @class DataSection.RawData
    public static final class RawData extends DataSection.Data
    {
        // @field
        private final byte[] ___data;

        // @cons DataSection.RawData
        public RawData(byte[] __data, int __alignment)
        {
            super(__alignment, __data.length);
            this.___data = __data;
        }

        @Override
        protected void emit(ByteBuffer __buffer, DataSection.Patches __patches)
        {
            __buffer.put(this.___data);
        }
    }

    // @class DataSection.SerializableData
    public static final class SerializableData extends DataSection.Data
    {
        // @field
        private final SerializableConstant ___constant;

        // @cons DataSection.SerializableData
        public SerializableData(SerializableConstant __constant)
        {
            this(__constant, 1);
        }

        // @cons DataSection.SerializableData
        public SerializableData(SerializableConstant __constant, int __alignment)
        {
            super(__alignment, __constant.getSerializedSize());
            this.___constant = __constant;
        }

        @Override
        protected void emit(ByteBuffer __buffer, DataSection.Patches __patches)
        {
            int __position = __buffer.position();
            this.___constant.serialize(__buffer);
        }
    }

    // @class DataSection.ZeroData
    public static class ZeroData extends DataSection.Data
    {
        // @cons DataSection.ZeroData
        protected ZeroData(int __alignment, int __size)
        {
            super(__alignment, __size);
        }

        public static DataSection.ZeroData create(int __alignment, int __size)
        {
            switch (__size)
            {
                case 1:
                    // @closure
                    return new DataSection.ZeroData(__alignment, __size)
                    {
                        @Override
                        protected void emit(ByteBuffer __buffer, DataSection.Patches __patches)
                        {
                            __buffer.put((byte) 0);
                        }
                    };

                case 2:
                    // @closure
                    return new DataSection.ZeroData(__alignment, __size)
                    {
                        @Override
                        protected void emit(ByteBuffer __buffer, DataSection.Patches __patches)
                        {
                            __buffer.putShort((short) 0);
                        }
                    };

                case 4:
                    // @closure
                    return new DataSection.ZeroData(__alignment, __size)
                    {
                        @Override
                        protected void emit(ByteBuffer __buffer, DataSection.Patches __patches)
                        {
                            __buffer.putInt(0);
                        }
                    };

                case 8:
                    // @closure
                    return new DataSection.ZeroData(__alignment, __size)
                    {
                        @Override
                        protected void emit(ByteBuffer __buffer, DataSection.Patches __patches)
                        {
                            __buffer.putLong(0);
                        }
                    };

                default:
                    return new DataSection.ZeroData(__alignment, __size);
            }
        }

        @Override
        protected void emit(ByteBuffer __buffer, DataSection.Patches __patches)
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
    public static final class PackedData extends DataSection.Data
    {
        // @field
        private final DataSection.Data[] ___nested;

        // @cons DataSection.PackedData
        private PackedData(int __alignment, int __size, DataSection.Data[] __nested)
        {
            super(__alignment, __size);
            this.___nested = __nested;
        }

        public static DataSection.PackedData create(DataSection.Data[] __nested)
        {
            int __size = 0;
            int __alignment = 1;
            for (int __i = 0; __i < __nested.length; __i++)
            {
                __alignment = DataSection.lcm(__alignment, __nested[__i].getAlignment());
                __size += __nested[__i].getSize();
            }
            return new DataSection.PackedData(__alignment, __size, __nested);
        }

        @Override
        protected void emit(ByteBuffer __buffer, DataSection.Patches __patches)
        {
            for (DataSection.Data __data : this.___nested)
            {
                __data.emit(__buffer, __patches);
            }
        }
    }

    // @field
    private final ArrayList<DataSection.Data> ___dataItems = new ArrayList<>();

    // @field
    private boolean ___closed;
    // @field
    private int ___sectionAlignment;
    // @field
    private int ___sectionSize;

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
            if (this.___closed == __that.___closed && this.___sectionAlignment == __that.___sectionAlignment && this.___sectionSize == __that.___sectionSize && Objects.equals(this.___dataItems, __that.___dataItems))
            {
                return true;
            }
        }
        return false;
    }

    ///
    // Inserts a {@link DataSection.Data} item into the data section. If the item is already in the
    // data section, the same {@link DataSectionReference} is returned.
    //
    // @param data the {@link DataSection.Data} item to be inserted
    // @return a unique {@link DataSectionReference} identifying the {@link DataSection.Data} item
    ///
    public DataSectionReference insertData(DataSection.Data __data)
    {
        checkOpen();
        synchronized (__data)
        {
            if (__data.___ref == null)
            {
                __data.___ref = new DataSectionReference();
                this.___dataItems.add(__data);
            }
            return __data.___ref;
        }
    }

    ///
    // Transfers all {@link DataSection.Data} from the provided other {@link DataSection} to this
    // {@link DataSection}, and empties the other section.
    ///
    public void addAll(DataSection __other)
    {
        checkOpen();
        __other.checkOpen();

        for (DataSection.Data __data : __other.___dataItems)
        {
            this.___dataItems.add(__data);
        }
        __other.___dataItems.clear();
    }

    ///
    // Determines if this object has been {@link #close() closed}.
    ///
    public boolean closed()
    {
        return this.___closed;
    }

    ///
    // Computes the layout of the data section and closes this object to further updates.
    //
    // This must be called exactly once.
    ///
    public void close()
    {
        checkOpen();
        this.___closed = true;

        // simple heuristic: put items with larger alignment requirement first
        this.___dataItems.sort((__a, __b) -> __a.___alignment - __b.___alignment);

        int __position = 0;
        int __alignment = 1;
        for (DataSection.Data __d : this.___dataItems)
        {
            __alignment = lcm(__alignment, __d.___alignment);
            __position = align(__position, __d.___alignment);

            __d.___ref.setOffset(__position);
            __position += __d.___size;
        }

        this.___sectionAlignment = __alignment;
        this.___sectionSize = __position;
    }

    ///
    // Gets the size of the data section.
    //
    // This must only be called once this object has been {@linkplain #closed() closed}.
    ///
    public int getSectionSize()
    {
        checkClosed();
        return this.___sectionSize;
    }

    ///
    // Gets the minimum alignment requirement of the data section.
    //
    // This must only be called once this object has been {@linkplain #closed() closed}.
    ///
    public int getSectionAlignment()
    {
        checkClosed();
        return this.___sectionAlignment;
    }

    ///
    // Builds the data section into a given buffer.
    //
    // This must only be called once this object has been {@linkplain #closed() closed}.
    //
    // @param buffer the {@link ByteBuffer} where the data section should be built. The buffer must
    //            hold at least {@link #getSectionSize()} bytes.
    // @param patch a {@link DataSection.Patches} instance to receive {@link VMConstant constants} for
    //            relocations in the data section
    ///
    public void buildDataSection(ByteBuffer __buffer, DataSection.Patches __patch)
    {
        buildDataSection(__buffer, __patch, (__r, __s) -> {});
    }

    ///
    // Builds the data section into a given buffer.
    //
    // This must only be called once this object has been {@linkplain #closed() closed}. When this
    // method returns, the buffers' position is just after the last data item.
    //
    // @param buffer the {@link ByteBuffer} where the data section should be built. The buffer must
    //            hold at least {@link #getSectionSize()} bytes.
    // @param patch a {@link DataSection.Patches} instance to receive {@link VMConstant constants} for
    // @param onEmit a function that is called before emitting each data item with the
    //            {@link DataSectionReference} and the size of the data.
    ///
    public void buildDataSection(ByteBuffer __buffer, DataSection.Patches __patch, BiConsumer<DataSectionReference, Integer> __onEmit)
    {
        checkClosed();
        int __start = __buffer.position();
        for (DataSection.Data __d : this.___dataItems)
        {
            __buffer.position(__start + __d.___ref.getOffset());
            __onEmit.accept(__d.___ref, __d.getSize());
            __d.emit(__buffer, __patch);
        }
        __buffer.position(__start + this.___sectionSize);
    }

    public DataSection.Data findData(DataSectionReference __ref)
    {
        for (DataSection.Data __d : this.___dataItems)
        {
            if (__d.___ref == __ref)
            {
                return __d;
            }
        }
        return null;
    }

    public static void emit(ByteBuffer __buffer, DataSection.Data __data, DataSection.Patches __patch)
    {
        __data.emit(__buffer, __patch);
    }

    @Override
    public Iterator<DataSection.Data> iterator()
    {
        return this.___dataItems.iterator();
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
        if (!this.___closed)
        {
            throw new IllegalStateException();
        }
    }

    private void checkOpen()
    {
        if (this.___closed)
        {
            throw new IllegalStateException();
        }
    }

    public void clear()
    {
        checkOpen();
        this.___dataItems.clear();
        this.___sectionAlignment = 0;
        this.___sectionSize = 0;
    }
}
