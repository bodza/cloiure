package graalvm.compiler.nodes;

import graalvm.compiler.core.common.type.StampPair;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.nodeinfo.NodeInfo;

import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.ResolvedJavaMethod;

@NodeInfo
public class DirectCallTargetNode extends LoweredCallTargetNode
{
    public static final NodeClass<DirectCallTargetNode> TYPE = NodeClass.create(DirectCallTargetNode.class);

    public DirectCallTargetNode(ValueNode[] arguments, StampPair returnStamp, JavaType[] signature, ResolvedJavaMethod target, CallingConvention.Type callType, InvokeKind invokeKind)
    {
        this(TYPE, arguments, returnStamp, signature, target, callType, invokeKind);
    }

    protected DirectCallTargetNode(NodeClass<? extends DirectCallTargetNode> c, ValueNode[] arguments, StampPair returnStamp, JavaType[] signature, ResolvedJavaMethod target, CallingConvention.Type callType, InvokeKind invokeKind)
    {
        super(c, arguments, returnStamp, signature, target, callType, invokeKind);
    }

    @Override
    public String targetName()
    {
        return targetMethod().format("Direct#%h.%n");
    }
}
