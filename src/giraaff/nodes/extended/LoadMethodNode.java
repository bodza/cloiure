package giraaff.nodes.extended;

import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.Assumptions.AssumptionResult;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.TypeReference;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.FixedWithNextNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;
import giraaff.nodes.type.StampTool;
import giraaff.util.GraalError;

/**
 * Loads a method from the virtual method table of a given hub.
 */
// @class LoadMethodNode
public final class LoadMethodNode extends FixedWithNextNode implements Lowerable, Canonicalizable
{
    // @def
    public static final NodeClass<LoadMethodNode> TYPE = NodeClass.create(LoadMethodNode.class);

    @Input
    // @field
    ValueNode hub;
    // @field
    protected final ResolvedJavaMethod method;
    // @field
    protected final ResolvedJavaType receiverType;

    /**
     * The caller or context type used to perform access checks when resolving {@link #method}.
     */
    // @field
    protected final ResolvedJavaType callerType;

    public ValueNode getHub()
    {
        return hub;
    }

    // @cons
    public LoadMethodNode(@InjectedNodeParameter Stamp __stamp, ResolvedJavaMethod __method, ResolvedJavaType __receiverType, ResolvedJavaType __callerType, ValueNode __hub)
    {
        super(TYPE, __stamp);
        this.receiverType = __receiverType;
        this.callerType = __callerType;
        this.hub = __hub;
        this.method = __method;
        if (!__method.isInVirtualMethodTable(__receiverType))
        {
            throw new GraalError("%s does not have a vtable entry in type %s", __method, __receiverType);
        }
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }

    @Override
    public Node canonical(CanonicalizerTool __tool)
    {
        if (hub instanceof LoadHubNode)
        {
            ValueNode __object = ((LoadHubNode) hub).getValue();
            TypeReference __type = StampTool.typeReferenceOrNull(__object);
            if (__type != null)
            {
                if (__type.isExact())
                {
                    return resolveExactMethod(__tool, __type.getType());
                }
                Assumptions __assumptions = graph().getAssumptions();
                AssumptionResult<ResolvedJavaMethod> __resolvedMethod = __type.getType().findUniqueConcreteMethod(method);
                if (__resolvedMethod != null && __resolvedMethod.canRecordTo(__assumptions) && !__type.getType().isInterface() && method.getDeclaringClass().isAssignableFrom(__type.getType()))
                {
                    NodeView __view = NodeView.from(__tool);
                    __resolvedMethod.recordTo(__assumptions);
                    return ConstantNode.forConstant(stamp(__view), __resolvedMethod.getResult().getEncoding(), __tool.getMetaAccess());
                }
            }
        }
        if (hub.isConstant())
        {
            return resolveExactMethod(__tool, __tool.getConstantReflection().asJavaType(hub.asConstant()));
        }

        return this;
    }

    /**
     * Find the method which would be loaded.
     *
     * @param type the exact type of object being loaded from
     * @return the method which would be invoked for {@code type} or null if it doesn't implement the method
     */
    private Node resolveExactMethod(CanonicalizerTool __tool, ResolvedJavaType __type)
    {
        ResolvedJavaMethod __newMethod = __type.resolveConcreteMethod(method, callerType);
        if (__newMethod == null)
        {
            /*
             * This really represent a misuse of LoadMethod since we're loading from a class which
             * isn't known to implement the original method but for now at least fold it away.
             */
            return ConstantNode.forConstant(stamp(NodeView.DEFAULT), JavaConstant.NULL_POINTER, null);
        }
        else
        {
            return ConstantNode.forConstant(stamp(NodeView.DEFAULT), __newMethod.getEncoding(), __tool.getMetaAccess());
        }
    }

    public ResolvedJavaMethod getMethod()
    {
        return method;
    }

    public ResolvedJavaType getReceiverType()
    {
        return receiverType;
    }

    public ResolvedJavaType getCallerType()
    {
        return callerType;
    }
}
