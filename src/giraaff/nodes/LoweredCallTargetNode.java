package giraaff.nodes;

import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.core.common.type.StampPair;
import giraaff.graph.NodeClass;

// @class LoweredCallTargetNode
public abstract class LoweredCallTargetNode extends CallTargetNode
{
    // @def
    public static final NodeClass<LoweredCallTargetNode> TYPE = NodeClass.create(LoweredCallTargetNode.class);

    // @field
    protected final JavaType[] signature;
    // @field
    protected final CallingConvention.Type callType;

    // @cons
    protected LoweredCallTargetNode(NodeClass<? extends LoweredCallTargetNode> __c, ValueNode[] __arguments, StampPair __returnStamp, JavaType[] __signature, ResolvedJavaMethod __target, CallingConvention.Type __callType, InvokeKind __invokeKind)
    {
        super(__c, __arguments, __target, __invokeKind, __returnStamp);
        this.signature = __signature;
        this.callType = __callType;
    }

    public JavaType[] signature()
    {
        return signature;
    }

    public CallingConvention.Type callType()
    {
        return callType;
    }
}
