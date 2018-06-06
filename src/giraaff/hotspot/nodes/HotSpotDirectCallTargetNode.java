package giraaff.hotspot.nodes;

import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.core.common.type.StampPair;
import giraaff.graph.NodeClass;
import giraaff.nodes.CallTargetNode;
import giraaff.nodes.DirectCallTargetNode;
import giraaff.nodes.ValueNode;

// @class HotSpotDirectCallTargetNode
public final class HotSpotDirectCallTargetNode extends DirectCallTargetNode
{
    // @def
    public static final NodeClass<HotSpotDirectCallTargetNode> TYPE = NodeClass.create(HotSpotDirectCallTargetNode.class);

    // @cons HotSpotDirectCallTargetNode
    public HotSpotDirectCallTargetNode(ValueNode[] __arguments, StampPair __returnStamp, JavaType[] __signature, ResolvedJavaMethod __target, CallingConvention.Type __callType, CallTargetNode.InvokeKind __invokeKind)
    {
        super(TYPE, __arguments, __returnStamp, __signature, __target, __callType, __invokeKind);
    }
}
