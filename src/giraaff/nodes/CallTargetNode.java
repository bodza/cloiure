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
import giraaff.graph.NodeClass;
import giraaff.graph.NodeInputList;
import giraaff.nodes.spi.LIRLowerable;
import giraaff.nodes.spi.NodeLIRBuilderTool;

// @NodeInfo.allowedUsageTypes "Extension"
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

        InvokeKind(boolean __direct)
        {
            this.direct = __direct;
        }

        // @field
        private final boolean direct;

        public boolean hasReceiver()
        {
            return this != Static;
        }

        public boolean isDirect()
        {
            return direct;
        }

        public boolean isIndirect()
        {
            return !direct;
        }

        public boolean isInterface()
        {
            return this == InvokeKind.Interface;
        }
    }

    @Input
    // @field
    protected NodeInputList<ValueNode> arguments;
    // @field
    protected ResolvedJavaMethod targetMethod;
    // @field
    protected InvokeKind invokeKind;
    // @field
    protected final StampPair returnStamp;

    // @cons
    protected CallTargetNode(NodeClass<? extends CallTargetNode> __c, ValueNode[] __arguments, ResolvedJavaMethod __targetMethod, InvokeKind __invokeKind, StampPair __returnStamp)
    {
        super(__c, StampFactory.forVoid());
        this.targetMethod = __targetMethod;
        this.invokeKind = __invokeKind;
        this.arguments = new NodeInputList<>(this, __arguments);
        this.returnStamp = __returnStamp;
    }

    public NodeInputList<ValueNode> arguments()
    {
        return arguments;
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
        return this.returnStamp;
    }

    /**
     * A human-readable representation of the target, used for debug printing only.
     */
    public abstract String targetName();

    @Override
    public void generate(NodeLIRBuilderTool __gen)
    {
        // nop
    }

    public void setTargetMethod(ResolvedJavaMethod __method)
    {
        targetMethod = __method;
    }

    /**
     * Gets the target method for this invocation instruction.
     *
     * @return the target method
     */
    public ResolvedJavaMethod targetMethod()
    {
        return targetMethod;
    }

    public InvokeKind invokeKind()
    {
        return invokeKind;
    }

    public void setInvokeKind(InvokeKind __kind)
    {
        this.invokeKind = __kind;
    }
}
