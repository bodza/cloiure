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
public final class LoadMethodNode extends FixedWithNextNode implements Lowerable, Canonicalizable
{
    public static final NodeClass<LoadMethodNode> TYPE = NodeClass.create(LoadMethodNode.class);
    @Input ValueNode hub;
    protected final ResolvedJavaMethod method;
    protected final ResolvedJavaType receiverType;

    /**
     * The caller or context type used to perform access checks when resolving {@link #method}.
     */
    protected final ResolvedJavaType callerType;

    public ValueNode getHub()
    {
        return hub;
    }

    public LoadMethodNode(@InjectedNodeParameter Stamp stamp, ResolvedJavaMethod method, ResolvedJavaType receiverType, ResolvedJavaType callerType, ValueNode hub)
    {
        super(TYPE, stamp);
        this.receiverType = receiverType;
        this.callerType = callerType;
        this.hub = hub;
        this.method = method;
        if (!method.isInVirtualMethodTable(receiverType))
        {
            throw new GraalError("%s does not have a vtable entry in type %s", method, receiverType);
        }
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }

    @Override
    public Node canonical(CanonicalizerTool tool)
    {
        if (hub instanceof LoadHubNode)
        {
            ValueNode object = ((LoadHubNode) hub).getValue();
            TypeReference type = StampTool.typeReferenceOrNull(object);
            if (type != null)
            {
                if (type.isExact())
                {
                    return resolveExactMethod(tool, type.getType());
                }
                Assumptions assumptions = graph().getAssumptions();
                AssumptionResult<ResolvedJavaMethod> resolvedMethod = type.getType().findUniqueConcreteMethod(method);
                if (resolvedMethod != null && resolvedMethod.canRecordTo(assumptions) && !type.getType().isInterface() && method.getDeclaringClass().isAssignableFrom(type.getType()))
                {
                    NodeView view = NodeView.from(tool);
                    resolvedMethod.recordTo(assumptions);
                    return ConstantNode.forConstant(stamp(view), resolvedMethod.getResult().getEncoding(), tool.getMetaAccess());
                }
            }
        }
        if (hub.isConstant())
        {
            return resolveExactMethod(tool, tool.getConstantReflection().asJavaType(hub.asConstant()));
        }

        return this;
    }

    /**
     * Find the method which would be loaded.
     *
     * @param type the exact type of object being loaded from
     * @return the method which would be invoked for {@code type} or null if it doesn't implement the method
     */
    private Node resolveExactMethod(CanonicalizerTool tool, ResolvedJavaType type)
    {
        ResolvedJavaMethod newMethod = type.resolveConcreteMethod(method, callerType);
        if (newMethod == null)
        {
            /*
             * This really represent a misuse of LoadMethod since we're loading from a class which
             * isn't known to implement the original method but for now at least fold it away.
             */
            return ConstantNode.forConstant(stamp(NodeView.DEFAULT), JavaConstant.NULL_POINTER, null);
        }
        else
        {
            return ConstantNode.forConstant(stamp(NodeView.DEFAULT), newMethod.getEncoding(), tool.getMetaAccess());
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
