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

///
// The {@code InstanceOfDynamicNode} represents a type check where the type being checked is not
// known at compile time. This is used, for instance, to intrinsify {@link Class#isInstance(Object)}.
///
// @class InstanceOfDynamicNode
public final class InstanceOfDynamicNode extends BinaryOpLogicNode implements Canonicalizable.Binary<ValueNode>, Lowerable
{
    // @def
    public static final NodeClass<InstanceOfDynamicNode> TYPE = NodeClass.create(InstanceOfDynamicNode.class);

    // @field
    private final boolean ___allowNull;
    // @field
    private final boolean ___exact;

    public static LogicNode create(Assumptions __assumptions, ConstantReflectionProvider __constantReflection, ValueNode __mirror, ValueNode __object, boolean __allowNull, boolean __exact)
    {
        LogicNode __synonym = findSynonym(__assumptions, __constantReflection, __mirror, __object, __allowNull, __exact);
        if (__synonym != null)
        {
            return __synonym;
        }
        return new InstanceOfDynamicNode(__mirror, __object, __allowNull, __exact);
    }

    public static LogicNode create(Assumptions __assumptions, ConstantReflectionProvider __constantReflection, ValueNode __mirror, ValueNode __object, boolean __allowNull)
    {
        return create(__assumptions, __constantReflection, __mirror, __object, __allowNull, false);
    }

    // @cons
    protected InstanceOfDynamicNode(ValueNode __mirror, ValueNode __object, boolean __allowNull, boolean __exact)
    {
        super(TYPE, __mirror, __object);
        this.___allowNull = __allowNull;
        this.___exact = __exact;
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
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
    }

    private static LogicNode findSynonym(Assumptions __assumptions, ConstantReflectionProvider __constantReflection, ValueNode __forMirror, ValueNode __forObject, boolean __allowNull, boolean __exact)
    {
        if (__forMirror.isConstant())
        {
            ResolvedJavaType __t = __constantReflection.asJavaType(__forMirror.asConstant());
            if (__t != null)
            {
                if (__t.isPrimitive())
                {
                    if (__allowNull)
                    {
                        return IsNullNode.create(__forObject);
                    }
                    else
                    {
                        return LogicConstantNode.contradiction();
                    }
                }
                else
                {
                    TypeReference __type = __exact ? TypeReference.createExactTrusted(__t) : TypeReference.createTrusted(__assumptions, __t);
                    if (__allowNull)
                    {
                        return InstanceOfNode.createAllowNull(__type, __forObject, null, null);
                    }
                    else
                    {
                        return InstanceOfNode.create(__type, __forObject);
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
    public LogicNode canonical(CanonicalizerTool __tool, ValueNode __forMirror, ValueNode __forObject)
    {
        LogicNode __result = findSynonym(__tool.getAssumptions(), __tool.getConstantReflection(), __forMirror, __forObject, this.___allowNull, this.___exact);
        if (__result != null)
        {
            return __result;
        }
        return this;
    }

    public void setMirror(ValueNode __newObject)
    {
        this.updateUsages(this.___x, __newObject);
        this.___x = __newObject;
    }

    public boolean allowsNull()
    {
        return this.___allowNull;
    }

    public boolean isExact()
    {
        return this.___exact;
    }

    @Override
    public Stamp getSucceedingStampForX(boolean __negated, Stamp __xStamp, Stamp __yStamp)
    {
        return null;
    }

    @Override
    public Stamp getSucceedingStampForY(boolean __negated, Stamp __xStamp, Stamp __yStamp)
    {
        return null;
    }

    @Override
    public TriState tryFold(Stamp __xStamp, Stamp __yStamp)
    {
        return TriState.UNKNOWN;
    }
}
