package giraaff.nodes.calc;

import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.TriState;

import giraaff.core.common.calc.CanonicalCondition;
import giraaff.core.common.type.FloatStamp;
import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.LogicConstantNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.util.GraphUtil;
import giraaff.util.GraalError;

// @class FloatLessThanNode
public final class FloatLessThanNode extends CompareNode
{
    // @def
    public static final NodeClass<FloatLessThanNode> TYPE = NodeClass.create(FloatLessThanNode.class);

    // @def
    private static final FloatLessThanOp OP = new FloatLessThanOp();

    // @cons
    public FloatLessThanNode(ValueNode __x, ValueNode __y, boolean __unorderedIsTrue)
    {
        super(TYPE, CanonicalCondition.LT, __unorderedIsTrue, __x, __y);
    }

    public static LogicNode create(ValueNode __x, ValueNode __y, boolean __unorderedIsTrue, NodeView __view)
    {
        LogicNode __result = CompareNode.tryConstantFoldPrimitive(CanonicalCondition.LT, __x, __y, __unorderedIsTrue, __view);
        if (__result != null)
        {
            return __result;
        }
        return new FloatLessThanNode(__x, __y, __unorderedIsTrue);
    }

    public static LogicNode create(ConstantReflectionProvider __constantReflection, MetaAccessProvider __metaAccess, Integer __smallestCompareWidth, ValueNode __x, ValueNode __y, boolean __unorderedIsTrue, NodeView __view)
    {
        LogicNode __result = OP.canonical(__constantReflection, __metaAccess, __smallestCompareWidth, CanonicalCondition.LT, __unorderedIsTrue, __x, __y, __view);
        if (__result != null)
        {
            return __result;
        }
        return create(__x, __y, __unorderedIsTrue, __view);
    }

    @Override
    public Node canonical(CanonicalizerTool __tool, ValueNode __forX, ValueNode __forY)
    {
        NodeView __view = NodeView.from(__tool);
        ValueNode __value = OP.canonical(__tool.getConstantReflection(), __tool.getMetaAccess(), __tool.smallestCompareWidth(), CanonicalCondition.LT, this.___unorderedIsTrue, __forX, __forY, __view);
        if (__value != null)
        {
            return __value;
        }
        return this;
    }

    // @class FloatLessThanNode.FloatLessThanOp
    public static final class FloatLessThanOp extends CompareOp
    {
        @Override
        public LogicNode canonical(ConstantReflectionProvider __constantReflection, MetaAccessProvider __metaAccess, Integer __smallestCompareWidth, CanonicalCondition __condition, boolean __unorderedIsTrue, ValueNode __forX, ValueNode __forY, NodeView __view)
        {
            LogicNode __result = super.canonical(__constantReflection, __metaAccess, __smallestCompareWidth, __condition, __unorderedIsTrue, __forX, __forY, __view);
            if (__result != null)
            {
                return __result;
            }
            if (GraphUtil.unproxify(__forX) == GraphUtil.unproxify(__forY) && !__unorderedIsTrue)
            {
                return LogicConstantNode.contradiction();
            }
            return null;
        }

        @Override
        protected CompareNode duplicateModified(ValueNode __newX, ValueNode __newY, boolean __unorderedIsTrue, NodeView __view)
        {
            if (__newX.stamp(NodeView.DEFAULT) instanceof FloatStamp && __newY.stamp(NodeView.DEFAULT) instanceof FloatStamp)
            {
                return new FloatLessThanNode(__newX, __newY, __unorderedIsTrue);
            }
            else if (__newX.stamp(NodeView.DEFAULT) instanceof IntegerStamp && __newY.stamp(NodeView.DEFAULT) instanceof IntegerStamp)
            {
                return new IntegerLessThanNode(__newX, __newY);
            }
            throw GraalError.shouldNotReachHere();
        }
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
    public TriState tryFold(Stamp __xStampGeneric, Stamp __yStampGeneric)
    {
        return TriState.UNKNOWN;
    }
}
