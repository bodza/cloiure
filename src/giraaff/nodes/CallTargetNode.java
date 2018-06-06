package giraaff.nodes;

import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.StampPair;
import giraaff.core.common.type.TypeReference;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.NodeInputList;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @NodeInfo.allowedUsageTypes "InputType.Extension"
// @class CallTargetNode
public abstract class CallTargetNode extends ValueNode implements LIRLowerable
{
    // @def
    public static final NodeClass<CallTargetNode> TYPE = NodeClass.create(CallTargetNode.class);

    // @enum CallTargetNode.InvokeKind
    public enum InvokeKind
    {
        Interface(false),
        Special(true),
        Static(true),
        Virtual(false);

        // @cons CallTargetNode.InvokeKind
        InvokeKind(boolean __direct)
        {
            this.___direct = __direct;
        }

        // @field
        private final boolean ___direct;

        public boolean hasReceiver()
        {
            return this != Static;
        }

        public boolean isDirect()
        {
            return this.___direct;
        }

        public boolean isIndirect()
        {
            return !this.___direct;
        }

        public boolean isInterface()
        {
            return this == CallTargetNode.InvokeKind.Interface;
        }
    }

    @Node.Input
    // @field
    protected NodeInputList<ValueNode> ___arguments;
    // @field
    protected ResolvedJavaMethod ___targetMethod;
    // @field
    protected CallTargetNode.InvokeKind ___invokeKind;
    // @field
    protected final StampPair ___returnStamp;

    // @cons CallTargetNode
    protected CallTargetNode(NodeClass<? extends CallTargetNode> __c, ValueNode[] __arguments, ResolvedJavaMethod __targetMethod, CallTargetNode.InvokeKind __invokeKind, StampPair __returnStamp)
    {
        super(__c, StampFactory.forVoid());
        this.___targetMethod = __targetMethod;
        this.___invokeKind = __invokeKind;
        this.___arguments = new NodeInputList<>(this, __arguments);
        this.___returnStamp = __returnStamp;
    }

    public NodeInputList<ValueNode> arguments()
    {
        return this.___arguments;
    }

    public static Stamp createReturnStamp(Assumptions __assumptions, JavaType __returnType)
    {
        JavaKind __kind = __returnType.getJavaKind();
        if (__kind == JavaKind.Object && __returnType instanceof ResolvedJavaType)
        {
            return StampFactory.object(TypeReference.create(__assumptions, (ResolvedJavaType) __returnType));
        }
        else
        {
            return StampFactory.forKind(__kind);
        }
    }

    public StampPair returnStamp()
    {
        return this.___returnStamp;
    }

    ///
    // A human-readable representation of the target, used for debug printing only.
    ///
    public abstract String targetName();

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        // nop
    }

    public void setTargetMethod(ResolvedJavaMethod __method)
    {
        this.___targetMethod = __method;
    }

    ///
    // Gets the target method for this invocation instruction.
    //
    // @return the target method
    ///
    public ResolvedJavaMethod targetMethod()
    {
        return this.___targetMethod;
    }

    public CallTargetNode.InvokeKind invokeKind()
    {
        return this.___invokeKind;
    }

    public void setInvokeKind(CallTargetNode.InvokeKind __kind)
    {
        this.___invokeKind = __kind;
    }
}
