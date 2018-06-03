package giraaff.nodes.virtual;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.graph.NodeClass;
import giraaff.nodes.FixedNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.extended.BoxNode;
import giraaff.nodes.spi.VirtualizerTool;

// @class VirtualBoxingNode
public final class VirtualBoxingNode extends VirtualInstanceNode
{
    // @def
    public static final NodeClass<VirtualBoxingNode> TYPE = NodeClass.create(VirtualBoxingNode.class);

    // @field
    protected final JavaKind ___boxingKind;

    // @cons
    public VirtualBoxingNode(ResolvedJavaType __type, JavaKind __boxingKind)
    {
        this(TYPE, __type, __boxingKind);
    }

    // @cons
    public VirtualBoxingNode(NodeClass<? extends VirtualBoxingNode> __c, ResolvedJavaType __type, JavaKind __boxingKind)
    {
        super(__c, __type, false);
        this.___boxingKind = __boxingKind;
    }

    public JavaKind getBoxingKind()
    {
        return this.___boxingKind;
    }

    @Override
    public VirtualBoxingNode duplicate()
    {
        return new VirtualBoxingNode(type(), this.___boxingKind);
    }

    @Override
    public ValueNode getMaterializedRepresentation(FixedNode __fixed, ValueNode[] __entries, LockState __locks)
    {
        return new BoxNode(__entries[0], type(), this.___boxingKind);
    }

    public ValueNode getBoxedValue(VirtualizerTool __tool)
    {
        return __tool.getEntry(this, 0);
    }
}
