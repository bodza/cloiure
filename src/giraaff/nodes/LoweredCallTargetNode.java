package giraaff.nodes;

import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.core.common.type.StampPair;
import giraaff.graph.NodeClass;

// @class LoweredCallTargetNode
public abstract class LoweredCallTargetNode extends CallTargetNode
{
    public static final NodeClass<LoweredCallTargetNode> TYPE = NodeClass.create(LoweredCallTargetNode.class);

    protected final JavaType[] signature;
    protected final CallingConvention.Type callType;

    // @cons
    protected LoweredCallTargetNode(NodeClass<? extends LoweredCallTargetNode> c, ValueNode[] arguments, StampPair returnStamp, JavaType[] signature, ResolvedJavaMethod target, CallingConvention.Type callType, InvokeKind invokeKind)
    {
        super(c, arguments, target, invokeKind, returnStamp);
        this.signature = signature;
        this.callType = callType;
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
