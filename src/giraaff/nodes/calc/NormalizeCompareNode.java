package giraaff.nodes.calc;

import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.JavaKind;

import giraaff.core.common.calc.CanonicalCondition;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.IterableNodeType;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.LogicConstantNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;

///
// Returns -1, 0, or 1 if either x < y, x == y, or x > y.
///
// @class NormalizeCompareNode
public final class NormalizeCompareNode extends BinaryNode implements IterableNodeType
{
    // @def
    public static final NodeClass<NormalizeCompareNode> TYPE = NodeClass.create(NormalizeCompareNode.class);

    // @cons NormalizeCompareNode
    public NormalizeCompareNode(ValueNode __x, ValueNode __y, JavaKind __kind)
    {
        super(TYPE, StampFactory.forInteger(__kind, -1, 1), __x, __y);
    }

    public static ValueNode create(ValueNode __x, ValueNode __y, JavaKind __kind, ConstantReflectionProvider __constantReflection)
    {
        ValueNode __result = tryConstantFold(__x, __y, __kind, __constantReflection);
        if (__result != null)
        {
            return __result;
        }

        return new NormalizeCompareNode(__x, __y, __kind);
    }

    protected static ValueNode tryConstantFold(ValueNode __x, ValueNode __y, JavaKind __kind, ConstantReflectionProvider __constantReflection)
    {
        LogicNode __result = CompareNode.tryConstantFold(CanonicalCondition.EQ, __x, __y, null);
        if (__result instanceof LogicConstantNode)
        {
            LogicConstantNode __logicConstantNode = (LogicConstantNode) __result;
            LogicNode __resultLT = CompareNode.tryConstantFold(CanonicalCondition.LT, __x, __y, __constantReflection);
            if (__resultLT instanceof LogicConstantNode)
            {
                LogicConstantNode __logicConstantNodeLT = (LogicConstantNode) __resultLT;
                if (__logicConstantNodeLT.getValue())
                {
                    return ConstantNode.forIntegerKind(__kind, -1);
                }
                else if (__logicConstantNode.getValue())
                {
                    return ConstantNode.forIntegerKind(__kind, 0);
                }
                else
                {
                    return ConstantNode.forIntegerKind(__kind, 1);
                }
            }
        }
        return null;
    }

    @Override
    public ValueNode canonical(CanonicalizerTool __tool, ValueNode __forX, ValueNode __forY)
    {
        NodeView __view = NodeView.from(__tool);
        ValueNode __result = tryConstantFold(this.___x, this.___y, stamp(__view).getStackKind(), __tool.getConstantReflection());
        if (__result != null)
        {
            return __result;
        }
        return this;
    }

    @Override
    public boolean inferStamp()
    {
        return false;
    }

    @Override
    public Stamp foldStamp(Stamp __stampX, Stamp __stampY)
    {
        return stamp(NodeView.DEFAULT);
    }
}
