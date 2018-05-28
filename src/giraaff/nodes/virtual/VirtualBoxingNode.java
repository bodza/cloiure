package giraaff.nodes.virtual;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.graph.NodeClass;
import giraaff.nodes.FixedNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.extended.BoxNode;
import giraaff.nodes.spi.VirtualizerTool;

public class VirtualBoxingNode extends VirtualInstanceNode
{
    public static final NodeClass<VirtualBoxingNode> TYPE = NodeClass.create(VirtualBoxingNode.class);

    protected final JavaKind boxingKind;

    public VirtualBoxingNode(ResolvedJavaType type, JavaKind boxingKind)
    {
        this(TYPE, type, boxingKind);
    }

    public VirtualBoxingNode(NodeClass<? extends VirtualBoxingNode> c, ResolvedJavaType type, JavaKind boxingKind)
    {
        super(c, type, false);
        this.boxingKind = boxingKind;
    }

    public JavaKind getBoxingKind()
    {
        return boxingKind;
    }

    @Override
    public VirtualBoxingNode duplicate()
    {
        return new VirtualBoxingNode(type(), boxingKind);
    }

    @Override
    public ValueNode getMaterializedRepresentation(FixedNode fixed, ValueNode[] entries, LockState locks)
    {
        return new BoxNode(entries[0], type(), boxingKind);
    }

    public ValueNode getBoxedValue(VirtualizerTool tool)
    {
        return tool.getEntry(this, 0);
    }
}
