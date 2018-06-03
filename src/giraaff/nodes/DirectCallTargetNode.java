package giraaff.nodes;

import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.core.common.type.StampPair;
import giraaff.graph.NodeClass;

// @class DirectCallTargetNode
public class DirectCallTargetNode extends LoweredCallTargetNode
{
    // @def
    public static final NodeClass<DirectCallTargetNode> TYPE = NodeClass.create(DirectCallTargetNode.class);

    // @cons
    public DirectCallTargetNode(ValueNode[] __arguments, StampPair __returnStamp, JavaType[] __signature, ResolvedJavaMethod __target, CallingConvention.Type __callType, InvokeKind __invokeKind)
    {
        this(TYPE, __arguments, __returnStamp, __signature, __target, __callType, __invokeKind);
    }

    // @cons
    protected DirectCallTargetNode(NodeClass<? extends DirectCallTargetNode> __c, ValueNode[] __arguments, StampPair __returnStamp, JavaType[] __signature, ResolvedJavaMethod __target, CallingConvention.Type __callType, InvokeKind __invokeKind)
    {
        super(__c, __arguments, __returnStamp, __signature, __target, __callType, __invokeKind);
    }

    @Override
    public String targetName()
    {
        return targetMethod().format("Direct#%h.%n");
    }
}
