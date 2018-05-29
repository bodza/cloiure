package giraaff.nodes.java;

import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.meta.TriState;

import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.TypeReference;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.BinaryOpLogicNode;
import giraaff.nodes.LogicConstantNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.IsNullNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

/**
 * The {@code InstanceOfDynamicNode} represents a type check where the type being checked is not
 * known at compile time. This is used, for instance, to intrinsify {@link Class#isInstance(Object)}.
 */
// @class InstanceOfDynamicNode
public final class InstanceOfDynamicNode extends BinaryOpLogicNode implements Canonicalizable.Binary<ValueNode>, Lowerable
{
    public static final NodeClass<InstanceOfDynamicNode> TYPE = NodeClass.create(InstanceOfDynamicNode.class);

    private final boolean allowNull;
    private final boolean exact;

    public static LogicNode create(Assumptions assumptions, ConstantReflectionProvider constantReflection, ValueNode mirror, ValueNode object, boolean allowNull, boolean exact)
    {
        LogicNode synonym = findSynonym(assumptions, constantReflection, mirror, object, allowNull, exact);
        if (synonym != null)
        {
            return synonym;
        }
        return new InstanceOfDynamicNode(mirror, object, allowNull, exact);
    }

    public static LogicNode create(Assumptions assumptions, ConstantReflectionProvider constantReflection, ValueNode mirror, ValueNode object, boolean allowNull)
    {
        return create(assumptions, constantReflection, mirror, object, allowNull, false);
    }

    // @cons
    protected InstanceOfDynamicNode(ValueNode mirror, ValueNode object, boolean allowNull, boolean exact)
    {
        super(TYPE, mirror, object);
        this.allowNull = allowNull;
        this.exact = exact;
    }

    public boolean isMirror()
    {
        return getMirrorOrHub().getStackKind() == JavaKind.Object;
    }

    public boolean isHub()
    {
        return !isMirror();
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
    }

    private static LogicNode findSynonym(Assumptions assumptions, ConstantReflectionProvider constantReflection, ValueNode forMirror, ValueNode forObject, boolean allowNull, boolean exact)
    {
        if (forMirror.isConstant())
        {
            ResolvedJavaType t = constantReflection.asJavaType(forMirror.asConstant());
            if (t != null)
            {
                if (t.isPrimitive())
                {
                    if (allowNull)
                    {
                        return IsNullNode.create(forObject);
                    }
                    else
                    {
                        return LogicConstantNode.contradiction();
                    }
                }
                else
                {
                    TypeReference type = exact ? TypeReference.createExactTrusted(t) : TypeReference.createTrusted(assumptions, t);
                    if (allowNull)
                    {
                        return InstanceOfNode.createAllowNull(type, forObject, null, null);
                    }
                    else
                    {
                        return InstanceOfNode.create(type, forObject);
                    }
                }
            }
        }
        return null;
    }

    public ValueNode getMirrorOrHub()
    {
        return this.getX();
    }

    public ValueNode getObject()
    {
        return this.getY();
    }

    @Override
    public LogicNode canonical(CanonicalizerTool tool, ValueNode forMirror, ValueNode forObject)
    {
        LogicNode result = findSynonym(tool.getAssumptions(), tool.getConstantReflection(), forMirror, forObject, allowNull, exact);
        if (result != null)
        {
            return result;
        }
        return this;
    }

    public void setMirror(ValueNode newObject)
    {
        this.updateUsages(x, newObject);
        this.x = newObject;
    }

    public boolean allowsNull()
    {
        return allowNull;
    }

    public boolean isExact()
    {
        return exact;
    }

    @Override
    public Stamp getSucceedingStampForX(boolean negated, Stamp xStamp, Stamp yStamp)
    {
        return null;
    }

    @Override
    public Stamp getSucceedingStampForY(boolean negated, Stamp xStamp, Stamp yStamp)
    {
        return null;
    }

    @Override
    public TriState tryFold(Stamp xStamp, Stamp yStamp)
    {
        return TriState.UNKNOWN;
    }
}
