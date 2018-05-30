package giraaff.nodes.virtual;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.spi.ArrayOffsetProvider;
import giraaff.graph.NodeClass;
import giraaff.nodes.FixedNode;
import giraaff.nodes.ValueNode;

// @class VirtualInstanceNode
public class VirtualInstanceNode extends VirtualObjectNode
{
    public static final NodeClass<VirtualInstanceNode> TYPE = NodeClass.create(VirtualInstanceNode.class);

    protected final ResolvedJavaType type;
    protected final ResolvedJavaField[] fields;

    // @cons
    public VirtualInstanceNode(ResolvedJavaType type, boolean hasIdentity)
    {
        this(type, type.getInstanceFields(true), hasIdentity);
    }

    // @cons
    public VirtualInstanceNode(ResolvedJavaType type, ResolvedJavaField[] fields, boolean hasIdentity)
    {
        this(TYPE, type, fields, hasIdentity);
    }

    // @cons
    protected VirtualInstanceNode(NodeClass<? extends VirtualInstanceNode> c, ResolvedJavaType type, boolean hasIdentity)
    {
        this(c, type, type.getInstanceFields(true), hasIdentity);
    }

    // @cons
    protected VirtualInstanceNode(NodeClass<? extends VirtualInstanceNode> c, ResolvedJavaType type, ResolvedJavaField[] fields, boolean hasIdentity)
    {
        super(c, type, hasIdentity);
        this.type = type;
        this.fields = fields;
    }

    @Override
    public ResolvedJavaType type()
    {
        return type;
    }

    @Override
    public int entryCount()
    {
        return fields.length;
    }

    public ResolvedJavaField field(int index)
    {
        return fields[index];
    }

    public ResolvedJavaField[] getFields()
    {
        return fields;
    }

    @Override
    public String entryName(int index)
    {
        return fields[index].getName();
    }

    public int fieldIndex(ResolvedJavaField field)
    {
        // on average fields.length == ~6, so a linear search is fast enough
        for (int i = 0; i < fields.length; i++)
        {
            if (fields[i].equals(field))
            {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int entryIndexForOffset(ArrayOffsetProvider arrayOffsetProvider, long constantOffset, JavaKind expectedEntryKind)
    {
        return fieldIndex(type.findInstanceFieldWithOffset(constantOffset, expectedEntryKind));
    }

    @Override
    public JavaKind entryKind(int index)
    {
        return fields[index].getJavaKind();
    }

    @Override
    public VirtualInstanceNode duplicate()
    {
        return new VirtualInstanceNode(type, fields, super.hasIdentity());
    }

    @Override
    public ValueNode getMaterializedRepresentation(FixedNode fixed, ValueNode[] entries, LockState locks)
    {
        return new AllocatedObjectNode(this);
    }
}
