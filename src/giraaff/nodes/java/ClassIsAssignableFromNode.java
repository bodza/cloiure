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
 * against instances. This is used, for instance, to intrinsify
 * {@link Class#isAssignableFrom(Class)} .
 */
public final class ClassIsAssignableFromNode extends BinaryOpLogicNode implements Canonicalizable.Binary<ValueNode>, Lowerable
{
    public static final NodeClass<ClassIsAssignableFromNode> TYPE = NodeClass.create(ClassIsAssignableFromNode.class);

    public ClassIsAssignableFromNode(ValueNode thisClass, ValueNode otherClass)
    {
        super(TYPE, thisClass, otherClass);
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
    public Node canonical(CanonicalizerTool tool, ValueNode forX, ValueNode forY)
    {
        if (forX.isConstant() && forY.isConstant())
        {
            ConstantReflectionProvider constantReflection = tool.getConstantReflection();
            ResolvedJavaType thisType = constantReflection.asJavaType(forX.asJavaConstant());
            ResolvedJavaType otherType = constantReflection.asJavaType(forY.asJavaConstant());
            if (thisType != null && otherType != null)
            {
                return LogicConstantNode.forBoolean(thisType.isAssignableFrom(otherType));
            }
        }
        return this;
    }

    @Override
    public void lower(LoweringTool tool)
    {
        tool.getLowerer().lower(this, tool);
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
