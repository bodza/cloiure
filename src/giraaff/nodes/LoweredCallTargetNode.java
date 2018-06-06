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
    protected final JavaType[] ___signature;
    // @field
    protected final CallingConvention.Type ___callType;

    // @cons LoweredCallTargetNode
    protected LoweredCallTargetNode(NodeClass<? extends LoweredCallTargetNode> __c, ValueNode[] __arguments, StampPair __returnStamp, JavaType[] __signature, ResolvedJavaMethod __target, CallingConvention.Type __callType, CallTargetNode.InvokeKind __invokeKind)
    {
        super(__c, __arguments, __target, __invokeKind, __returnStamp);
        this.___signature = __signature;
        this.___callType = __callType;
    }

    public JavaType[] signature()
    {
        return this.___signature;
    }

    public CallingConvention.Type callType()
    {
        return this.___callType;
    }
}
