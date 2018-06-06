package giraaff.nodes;

import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.core.common.type.StampPair;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;

// @class IndirectCallTargetNode
public class IndirectCallTargetNode extends LoweredCallTargetNode
{
    // @def
    public static final NodeClass<IndirectCallTargetNode> TYPE = NodeClass.create(IndirectCallTargetNode.class);

    @Node.Input
    // @field
    protected ValueNode ___computedAddress;

    // @cons IndirectCallTargetNode
    public IndirectCallTargetNode(ValueNode __computedAddress, ValueNode[] __arguments, StampPair __returnStamp, JavaType[] __signature, ResolvedJavaMethod __target, CallingConvention.Type __callType, CallTargetNode.InvokeKind __invokeKind)
    {
        this(TYPE, __computedAddress, __arguments, __returnStamp, __signature, __target, __callType, __invokeKind);
    }

    // @cons IndirectCallTargetNode
    protected IndirectCallTargetNode(NodeClass<? extends IndirectCallTargetNode> __c, ValueNode __computedAddress, ValueNode[] __arguments, StampPair __returnStamp, JavaType[] __signature, ResolvedJavaMethod __target, CallingConvention.Type __callType, CallTargetNode.InvokeKind __invokeKind)
    {
        super(__c, __arguments, __returnStamp, __signature, __target, __callType, __invokeKind);
        this.___computedAddress = __computedAddress;
    }

    public ValueNode computedAddress()
    {
        return this.___computedAddress;
    }

    @Override
    public String targetName()
    {
        if (targetMethod() == null)
        {
            return "[unknown]";
        }
        return targetMethod().format("Indirect#%h.%n");
    }
}
