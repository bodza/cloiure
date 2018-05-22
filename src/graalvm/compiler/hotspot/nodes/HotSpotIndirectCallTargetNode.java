package graalvm.compiler.hotspot.nodes;

import jdk.vm.ci.code.CallingConvention.Type;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import graalvm.compiler.core.common.type.StampPair;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodes.IndirectCallTargetNode;
import graalvm.compiler.nodes.ValueNode;

public final class HotSpotIndirectCallTargetNode extends IndirectCallTargetNode
{
    public static final NodeClass<HotSpotIndirectCallTargetNode> TYPE = NodeClass.create(HotSpotIndirectCallTargetNode.class);

    @Input ValueNode metaspaceMethod;

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
