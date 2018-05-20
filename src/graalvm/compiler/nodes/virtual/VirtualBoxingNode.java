package graalvm.compiler.nodes.virtual;

import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.FixedNode;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.extended.BoxNode;

import graalvm.compiler.nodes.spi.VirtualizerTool;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaType;

@NodeInfo
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
