package giraaff.nodes.java;

import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.meta.TriState;

import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.Canonicalizable;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.BinaryOpLogicNode;
import giraaff.nodes.LogicConstantNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.spi.Lowerable;
import giraaff.nodes.spi.LoweringTool;

/**
 * The {@code ClassIsAssignableFromNode} represents a type check against {@link Class} instead of
 * against instances. This is used, for instance, to intrinsify {@link Class#isAssignableFrom(Class)}.
 */
// @class ClassIsAssignableFromNode
public final class ClassIsAssignableFromNode extends BinaryOpLogicNode implements Canonicalizable.Binary<ValueNode>, Lowerable
{
    // @def
    public static final NodeClass<ClassIsAssignableFromNode> TYPE = NodeClass.create(ClassIsAssignableFromNode.class);

    // @cons
    public ClassIsAssignableFromNode(ValueNode __thisClass, ValueNode __otherClass)
    {
        super(TYPE, __thisClass, __otherClass);
    }

    public ValueNode getThisClass()
    {
        return getX();
    }

    public ValueNode getOtherClass()
    {
        return getY();
    }

    @Override
    public Node canonical(CanonicalizerTool __tool, ValueNode __forX, ValueNode __forY)
    {
        if (__forX.isConstant() && __forY.isConstant())
        {
            ConstantReflectionProvider __constantReflection = __tool.getConstantReflection();
            ResolvedJavaType __thisType = __constantReflection.asJavaType(__forX.asJavaConstant());
            ResolvedJavaType __otherType = __constantReflection.asJavaType(__forY.asJavaConstant());
            if (__thisType != null && __otherType != null)
            {
                return LogicConstantNode.forBoolean(__thisType.isAssignableFrom(__otherType));
            }
        }
        return this;
    }

    @Override
    public void lower(LoweringTool __tool)
    {
        __tool.getLowerer().lower(this, __tool);
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
