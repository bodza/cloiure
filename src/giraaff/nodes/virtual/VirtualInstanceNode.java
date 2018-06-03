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
    // @def
    public static final NodeClass<VirtualInstanceNode> TYPE = NodeClass.create(VirtualInstanceNode.class);

    // @field
    protected final ResolvedJavaType ___type;
    // @field
    protected final ResolvedJavaField[] ___fields;

    // @cons
    public VirtualInstanceNode(ResolvedJavaType __type, boolean __hasIdentity)
    {
        this(__type, __type.getInstanceFields(true), __hasIdentity);
    }

    // @cons
    public VirtualInstanceNode(ResolvedJavaType __type, ResolvedJavaField[] __fields, boolean __hasIdentity)
    {
        this(TYPE, __type, __fields, __hasIdentity);
    }

    // @cons
    protected VirtualInstanceNode(NodeClass<? extends VirtualInstanceNode> __c, ResolvedJavaType __type, boolean __hasIdentity)
    {
        this(__c, __type, __type.getInstanceFields(true), __hasIdentity);
    }

    // @cons
    protected VirtualInstanceNode(NodeClass<? extends VirtualInstanceNode> __c, ResolvedJavaType __type, ResolvedJavaField[] __fields, boolean __hasIdentity)
    {
        super(__c, __type, __hasIdentity);
        this.___type = __type;
        this.___fields = __fields;
    }

    @Override
    public ResolvedJavaType type()
    {
        return this.___type;
    }

    @Override
    public int entryCount()
    {
        return this.___fields.length;
    }

    public ResolvedJavaField field(int __index)
    {
        return this.___fields[__index];
    }

    public ResolvedJavaField[] getFields()
    {
        return this.___fields;
    }

    @Override
    public String entryName(int __index)
    {
        return this.___fields[__index].getName();
    }

    public int fieldIndex(ResolvedJavaField __field)
    {
        // on average fields.length == ~6, so a linear search is fast enough
        for (int __i = 0; __i < this.___fields.length; __i++)
        {
            if (this.___fields[__i].equals(__field))
            {
                return __i;
            }
        }
        return -1;
    }

    @Override
    public int entryIndexForOffset(ArrayOffsetProvider __arrayOffsetProvider, long __constantOffset, JavaKind __expectedEntryKind)
    {
        return fieldIndex(this.___type.findInstanceFieldWithOffset(__constantOffset, __expectedEntryKind));
    }

    @Override
    public JavaKind entryKind(int __index)
    {
        return this.___fields[__index].getJavaKind();
    }

    @Override
    public VirtualInstanceNode duplicate()
    {
        return new VirtualInstanceNode(this.___type, this.___fields, super.hasIdentity());
    }

    @Override
    public ValueNode getMaterializedRepresentation(FixedNode __fixed, ValueNode[] __entries, LockState __locks)
    {
        return new AllocatedObjectNode(this);
    }
}
