package giraaff.nodes.virtual;

import java.nio.ByteOrder;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.spi.ArrayOffsetProvider;
import giraaff.graph.NodeClass;
import giraaff.nodeinfo.Verbosity;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.ArrayLengthProvider;
import giraaff.nodes.spi.NodeLIRBuilderTool;

public class VirtualArrayNode extends VirtualObjectNode implements ArrayLengthProvider
{
    public static final NodeClass<VirtualArrayNode> TYPE = NodeClass.create(VirtualArrayNode.class);
    protected final ResolvedJavaType componentType;
    protected final int length;

    public VirtualArrayNode(ResolvedJavaType componentType, int length)
    {
        this(TYPE, componentType, length);
    }

    protected VirtualArrayNode(NodeClass<? extends VirtualObjectNode> c, ResolvedJavaType componentType, int length)
    {
        super(c, componentType.getArrayClass(), true);
        this.componentType = componentType;
        this.length = length;
    }

    @Override
    public ResolvedJavaType type()
    {
        return componentType.getArrayClass();
    }

    public ResolvedJavaType componentType()
    {
        return componentType;
    }

    @Override
    public int entryCount()
    {
        return length;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen)
    {
        // nothing to do...
    }

    @Override
    public String toString(Verbosity verbosity)
    {
        if (verbosity == Verbosity.Name)
        {
            return super.toString(Verbosity.Name) + "(" + getObjectId() + ") " + componentType.getName() + "[" + length + "]";
        }
        else
        {
            return super.toString(verbosity);
        }
    }

    @Override
    public String entryName(int index)
    {
        return "[" + index + "]";
    }

    @Override
    public int entryIndexForOffset(ArrayOffsetProvider arrayOffsetProvider, long constantOffset, JavaKind expectedEntryKind)
    {
        return entryIndexForOffset(arrayOffsetProvider, constantOffset, expectedEntryKind, componentType, length);
    }

    public static int entryIndexForOffset(ArrayOffsetProvider arrayOffsetProvider, long constantOffset, JavaKind expectedEntryKind, ResolvedJavaType componentType, int length)
    {
        int baseOffset = arrayOffsetProvider.arrayBaseOffset(componentType.getJavaKind());
        int indexScale = arrayOffsetProvider.arrayScalingFactor(componentType.getJavaKind());

        long offset;
        if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN && componentType.isPrimitive())
        {
            // On big endian, we expect the value to be correctly aligned in memory
            int componentByteCount = componentType.getJavaKind().getByteCount();
            offset = constantOffset - (componentByteCount - Math.min(componentByteCount, 4 + expectedEntryKind.getByteCount()));
        }
        else
        {
            offset = constantOffset;
        }
        long index = offset - baseOffset;
        if (index % indexScale != 0)
        {
            return -1;
        }
        long elementIndex = index / indexScale;
        if (elementIndex < 0 || elementIndex >= length)
        {
            return -1;
        }
        return (int) elementIndex;
    }

    @Override
    public JavaKind entryKind(int index)
    {
        return componentType.getJavaKind();
    }

    @Override
    public VirtualArrayNode duplicate()
    {
        return new VirtualArrayNode(componentType, length);
    }

    @Override
    public ValueNode getMaterializedRepresentation(FixedNode fixed, ValueNode[] entries, LockState locks)
    {
        return new AllocatedObjectNode(this);
    }

    @Override
    public ValueNode length()
    {
        return ConstantNode.forInt(length);
    }
}