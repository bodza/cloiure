package graalvm.compiler.hotspot.nodes;

import graalvm.compiler.core.common.type.StampPair;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.DirectCallTargetNode;
import graalvm.compiler.nodes.ValueNode;

import jdk.vm.ci.code.CallingConvention.Type;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.ResolvedJavaMethod;

@NodeInfo
public final class HotSpotDirectCallTargetNode extends DirectCallTargetNode
{
    public static final NodeClass<HotSpotDirectCallTargetNode> TYPE = NodeClass.create(HotSpotDirectCallTargetNode.class);

    public HotSpotDirectCallTargetNode(ValueNode[] arguments, StampPair returnStamp, JavaType[] signature, ResolvedJavaMethod target, Type callType, InvokeKind invokeKind)
    {
        super(TYPE, arguments, returnStamp, signature, target, callType, invokeKind);
    }
}
