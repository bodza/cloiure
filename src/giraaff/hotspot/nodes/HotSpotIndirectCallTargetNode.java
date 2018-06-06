package giraaff.hotspot.nodes;

import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.core.common.type.StampPair;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.nodes.CallTargetNode;
import giraaff.nodes.IndirectCallTargetNode;
import giraaff.nodes.ValueNode;

// @class HotSpotIndirectCallTargetNode
public final class HotSpotIndirectCallTargetNode extends IndirectCallTargetNode
{
    // @def
    public static final NodeClass<HotSpotIndirectCallTargetNode> TYPE = NodeClass.create(HotSpotIndirectCallTargetNode.class);

    @Node.Input
    // @field
    ValueNode ___metaspaceMethod;

    // @cons HotSpotIndirectCallTargetNode
    public HotSpotIndirectCallTargetNode(ValueNode __metaspaceMethod, ValueNode __computedAddress, ValueNode[] __arguments, StampPair __returnStamp, JavaType[] __signature, ResolvedJavaMethod __target, CallingConvention.Type __callType, CallTargetNode.InvokeKind __invokeKind)
    {
        super(TYPE, __computedAddress, __arguments, __returnStamp, __signature, __target, __callType, __invokeKind);
        this.___metaspaceMethod = __metaspaceMethod;
    }

    public ValueNode metaspaceMethod()
    {
        return this.___metaspaceMethod;
    }
}
