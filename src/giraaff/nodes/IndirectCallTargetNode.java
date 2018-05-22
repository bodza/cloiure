package giraaff.nodes;

import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.core.common.type.StampPair;
import giraaff.graph.NodeClass;

public class IndirectCallTargetNode extends LoweredCallTargetNode
{
    public static final NodeClass<IndirectCallTargetNode> TYPE = NodeClass.create(IndirectCallTargetNode.class);

    @Input protected ValueNode computedAddress;

    public IndirectCallTargetNode(ValueNode computedAddress, ValueNode[] arguments, StampPair returnStamp, JavaType[] signature, ResolvedJavaMethod target, CallingConvention.Type callType, InvokeKind invokeKind)
    {
        this(TYPE, computedAddress, arguments, returnStamp, signature, target, callType, invokeKind);
    }

    protected IndirectCallTargetNode(NodeClass<? extends IndirectCallTargetNode> c, ValueNode computedAddress, ValueNode[] arguments, StampPair returnStamp, JavaType[] signature, ResolvedJavaMethod target, CallingConvention.Type callType, InvokeKind invokeKind)
    {
        super(c, arguments, returnStamp, signature, target, callType, invokeKind);
        this.computedAddress = computedAddress;
    }

    public ValueNode computedAddress()
    {
        return computedAddress;
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
