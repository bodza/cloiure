package giraaff.nodes.virtual;

import java.nio.ByteOrder;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.spi.ArrayOffsetProvider;
import giraaff.graph.NodeClass;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.FixedNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.ArrayLengthProvider;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @class VirtualArrayNode
public final class VirtualArrayNode extends VirtualObjectNode implements ArrayLengthProvider
{
    // @def
    public static final NodeClass<VirtualArrayNode> TYPE = NodeClass.create(VirtualArrayNode.class);

    // @field
    protected final ResolvedJavaType ___componentType;
    // @field
    protected final int ___length;

    // @cons
    public VirtualArrayNode(ResolvedJavaType __componentType, int __length)
    {
        this(TYPE, __componentType, __length);
    }

    // @cons
    protected VirtualArrayNode(NodeClass<? extends VirtualObjectNode> __c, ResolvedJavaType __componentType, int __length)
    {
        super(__c, __componentType.getArrayClass(), true);
        this.___componentType = __componentType;
        this.___length = __length;
    }

    @Override
    public ResolvedJavaType type()
    {
        return this.___componentType.getArrayClass();
    }

    public ResolvedJavaType componentType()
    {
        return this.___componentType;
    }

    @Override
    public int entryCount()
    {
        return this.___length;
    }

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        // nothing to do...
    }

    @Override
    public String entryName(int __index)
    {
        return "[" + __index + "]";
    }

    @Override
    public int entryIndexForOffset(ArrayOffsetProvider __arrayOffsetProvider, long __constantOffset, JavaKind __expectedEntryKind)
    {
        return entryIndexForOffset(__arrayOffsetProvider, __constantOffset, __expectedEntryKind, this.___componentType, this.___length);
    }

    public static int entryIndexForOffset(ArrayOffsetProvider __arrayOffsetProvider, long __constantOffset, JavaKind __expectedEntryKind, ResolvedJavaType __componentType, int __length)
    {
        int __baseOffset = __arrayOffsetProvider.arrayBaseOffset(__componentType.getJavaKind());
        int __indexScale = __arrayOffsetProvider.arrayScalingFactor(__componentType.getJavaKind());

        long __offset;
        if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN && __componentType.isPrimitive())
        {
            // on big endian, we expect the value to be correctly aligned in memory
            int __componentByteCount = __componentType.getJavaKind().getByteCount();
            __offset = __constantOffset - (__componentByteCount - Math.min(__componentByteCount, 4 + __expectedEntryKind.getByteCount()));
        }
        else
        {
            __offset = __constantOffset;
        }
        long __index = __offset - __baseOffset;
        if (__index % __indexScale != 0)
        {
            return -1;
        }
        long __elementIndex = __index / __indexScale;
        if (__elementIndex < 0 || __elementIndex >= __length)
        {
            return -1;
        }
        return (int) __elementIndex;
    }

    @Override
    public JavaKind entryKind(int __index)
    {
        return this.___componentType.getJavaKind();
    }

    @Override
    public VirtualArrayNode duplicate()
    {
        return new VirtualArrayNode(this.___componentType, this.___length);
    }

    @Override
    public ValueNode getMaterializedRepresentation(FixedNode __fixed, ValueNode[] __entries, LockState __locks)
    {
        return new AllocatedObjectNode(this);
    }

    @Override
    public ValueNode length()
    {
        return ConstantNode.forInt(this.___length);
    }
}
