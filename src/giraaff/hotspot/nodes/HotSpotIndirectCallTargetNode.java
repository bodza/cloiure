package giraaff.hotspot.nodes;

import jdk.vm.ci.code.CallingConvention.Type;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.core.common.type.StampPair;
import giraaff.graph.NodeClass;
import giraaff.nodes.IndirectCallTargetNode;
import giraaff.nodes.ValueNode;

// @class HotSpotIndirectCallTargetNode
public final class HotSpotIndirectCallTargetNode extends IndirectCallTargetNode
{
    public static final NodeClass<HotSpotIndirectCallTargetNode> TYPE = NodeClass.create(HotSpotIndirectCallTargetNode.class);

    @Input ValueNode metaspaceMethod;

    // @cons
    public HotSpotIndirectCallTargetNode(ValueNode metaspaceMethod, ValueNode computedAddress, ValueNode[] arguments, StampPair returnStamp, JavaType[] signature, ResolvedJavaMethod target, Type callType, InvokeKind invokeKind)
    {
        super(TYPE, computedAddress, arguments, returnStamp, signature, target, callType, invokeKind);
        this.metaspaceMethod = metaspaceMethod;
    }

    public ValueNode metaspaceMethod()
    {
        return metaspaceMethod;
    }
}
