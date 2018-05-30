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
        private int alignment;
        private final int size;

        private DataSectionReference ref;

        // @cons
        protected Data(int alignment, int size)
        {
            super();
            this.alignment = alignment;
            this.size = size;

            // initialized in DataSection.insertData(Data)
            ref = null;
        }

        protected abstract void emit(ByteBuffer buffer, Patches patches);

        public void updateAlignment(int newAlignment)
        {
            if (newAlignment == alignment)
            {
                return;
            }
            alignment = lcm(alignment, newAlignment);
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
        public boolean equals(Object obj)
        {
            if (obj == this)
            {
                return true;
            }
            if (obj instanceof Data)
            {
                Data that = (Data) obj;
                if (this.alignment == that.alignment && this.size == that.size && this.ref.equals(that.ref))
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
        private final byte[] data;

        // @cons
        public RawData(byte[] data, int alignment)
        {
            super(alignment, data.length);
            this.data = data;
        }

        @Override
        protected void emit(ByteBuffer buffer, Patches patches)
        {
            buffer.put(data);
        }
    }

    // @class DataSection.SerializableData
    public static final class SerializableData extends Data
    {
        private final SerializableConstant constant;

        // @cons
        public SerializableData(SerializableConstant constant)
        {
            this(constant, 1);
        }

        // @cons
        public SerializableData(SerializableConstant constant, int alignment)
        {
            super(alignment, constant.getSerializedSize());
            this.constant = constant;
        }

        @Override
        protected void emit(ByteBuffer buffer, Patches patches)
        {
            int position = buffer.position();
            constant.serialize(buffer);
        }
    }

    // @class DataSection.ZeroData
    public static class ZeroData extends Data
    {
        // @cons
        protected ZeroData(int alignment, int size)
        {
            super(alignment, size);
        }

        public static ZeroData create(int alignment, int size)
        {
            switch (size)
            {
                case 1:
                    return new ZeroData(alignment, size)
                    {
                        @Override
                        protected void emit(ByteBuffer buffer, Patches patches)
                        {
                            buffer.put((byte) 0);
                        }
                    };

                case 2:
                    return new ZeroData(alignment, size)
                    {
                        @Override
                        protected void emit(ByteBuffer buffer, Patches patches)
                        {
                            buffer.putShort((short) 0);
                        }
                    };

                case 4:
                    return new ZeroData(alignment, size)
                    {
                        @Override
                        protected void emit(ByteBuffer buffer, Patches patches)
                        {
                            buffer.putInt(0);
                        }
                    };

                case 8:
                    return new ZeroData(alignment, size)
                    {
                        @Override
                        protected void emit(ByteBuffer buffer, Patches patches)
                        {
                            buffer.putLong(0);
                        }
                    };

                default:
                    return new ZeroData(alignment, size);
            }
        }

        @Override
        protected void emit(ByteBuffer buffer, Patches patches)
        {
            int rest = getSize();
            while (rest > 8)
            {
                buffer.putLong(0L);
                rest -= 8;
            }
            while (rest > 0)
            {
                buffer.put((byte) 0);
                rest--;
            }
        }
    }

    // @class DataSection.PackedData
    public static final class PackedData extends Data
    {
        private final Data[] nested;

        // @cons
        private PackedData(int alignment, int size, Data[] nested)
        {
            super(alignment, size);
            this.nested = nested;
        }

        public static PackedData create(Data[] nested)
        {
            int size = 0;
            int alignment = 1;
            for (int i = 0; i < nested.length; i++)
            {
                alignment = DataSection.lcm(alignment, nested[i].getAlignment());
                size += nested[i].getSize();
            }
            return new PackedData(alignment, size, nested);
        }

        @Override
        protected void emit(ByteBuffer buffer, Patches patches)
        {
            for (Data data : nested)
            {
                data.emit(buffer, patches);
            }
        }
    }

    private final ArrayList<Data> dataItems = new ArrayList<>();

    private boolean closed;
    private int sectionAlignment;
    private int sectionSize;

    @Override
    public int hashCode()
    {
        // DataSection instances should not be used as hash map keys
        throw new UnsupportedOperationException("hashCode");
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj instanceof DataSection)
        {
            DataSection that = (DataSection) obj;
            if (this.closed == that.closed && this.sectionAlignment == that.sectionAlignment && this.sectionSize == that.sectionSize && Objects.equals(this.dataItems, that.dataItems))
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
    public DataSectionReference insertData(Data data)
    {
        checkOpen();
        synchronized (data)
        {
            if (data.ref == null)
            {
                data.ref = new DataSectionReference();
                dataItems.add(data);
            }
            return data.ref;
        }
    }

    /**
     * Transfers all {@link Data} from the provided other {@link DataSection} to this
     * {@link DataSection}, and empties the other section.
     */
    public void addAll(DataSection other)
    {
        checkOpen();
        other.checkOpen();

        for (Data data : other.dataItems)
        {
            dataItems.add(data);
        }
        other.dataItems.clear();
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
        dataItems.sort((a, b) -> a.alignment - b.alignment);

        int position = 0;
        int alignment = 1;
        for (Data d : dataItems)
        {
            alignment = lcm(alignment, d.alignment);
            position = align(position, d.alignment);

            d.ref.setOffset(position);
            position += d.size;
        }

        sectionAlignment = alignment;
        sectionSize = position;
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
    public void buildDataSection(ByteBuffer buffer, Patches patch)
    {
        buildDataSection(buffer, patch, (r, s) -> {});
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
    public void buildDataSection(ByteBuffer buffer, Patches patch, BiConsumer<DataSectionReference, Integer> onEmit)
    {
        checkClosed();
        int start = buffer.position();
        for (Data d : dataItems)
        {
            buffer.position(start + d.ref.getOffset());
            onEmit.accept(d.ref, d.getSize());
            d.emit(buffer, patch);
        }
        buffer.position(start + sectionSize);
    }

    public Data findData(DataSectionReference ref)
    {
        for (Data d : dataItems)
        {
            if (d.ref == ref)
            {
                return d;
            }
        }
        return null;
    }

    public static void emit(ByteBuffer buffer, Data data, Patches patch)
    {
        data.emit(buffer, patch);
    }

    @Override
    public Iterator<Data> iterator()
    {
        return dataItems.iterator();
    }

    private static int lcm(int x, int y)
    {
        if (x == 0)
        {
            return y;
        }
        else if (y == 0)
        {
            return x;
        }

        int a = Math.max(x, y);
        int b = Math.min(x, y);
        while (b > 0)
        {
            int tmp = a % b;
            a = b;
            b = tmp;
        }

        int gcd = a;
        return x * y / gcd;
    }

    private static int align(int position, int alignment)
    {
        return ((position + alignment - 1) / alignment) * alignment;
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
