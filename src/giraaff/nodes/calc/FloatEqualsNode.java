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
import giraaff.graph.spi.Canonicalizable.BinaryCommutative;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.LogicConstantNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.util.GraphUtil;
import giraaff.util.GraalError;

// @class FloatEqualsNode
public final class FloatEqualsNode extends CompareNode implements BinaryCommutative<ValueNode>
{
    // @def
    public static final NodeClass<FloatEqualsNode> TYPE = NodeClass.create(FloatEqualsNode.class);

    // @def
    private static final FloatEqualsOp OP = new FloatEqualsOp();

    // @cons
    public FloatEqualsNode(ValueNode __x, ValueNode __y)
    {
        super(TYPE, CanonicalCondition.EQ, false, __x, __y);
    }

    public static LogicNode create(ValueNode __x, ValueNode __y, NodeView __view)
    {
        LogicNode __result = CompareNode.tryConstantFoldPrimitive(CanonicalCondition.EQ, __x, __y, false, __view);
        if (__result != null)
        {
            return __result;
        }
        else
        {
            return new FloatEqualsNode(__x, __y).maybeCommuteInputs();
        }
    }

    public static LogicNode create(ConstantReflectionProvider __constantReflection, MetaAccessProvider __metaAccess, Integer __smallestCompareWidth, ValueNode __x, ValueNode __y, NodeView __view)
    {
        LogicNode __value = OP.canonical(__constantReflection, __metaAccess, __smallestCompareWidth, CanonicalCondition.EQ, false, __x, __y, __view);
        if (__value != null)
        {
            return __value;
        }
        return create(__x, __y, __view);
    }

    @Override
    public boolean isIdentityComparison()
    {
        FloatStamp __xStamp = (FloatStamp) x.stamp(NodeView.DEFAULT);
        FloatStamp __yStamp = (FloatStamp) y.stamp(NodeView.DEFAULT);
        /*
         * If both stamps have at most one 0.0 and it's the same 0.0 then this is an identity
         * comparison. FloatStamp isn't careful about tracking the presence of -0.0 so assume that
         * anything that includes 0.0 might include -0.0. So if either one is non-zero then it's an
         * identity comparison.
         */
        return (!__xStamp.contains(0.0) || !__yStamp.contains(0.0));
    }

    @Override
    public Node canonical(CanonicalizerTool __tool, ValueNode __forX, ValueNode __forY)
    {
        NodeView __view = NodeView.from(__tool);
        ValueNode __value = OP.canonical(__tool.getConstantReflection(), __tool.getMetaAccess(), __tool.smallestCompareWidth(), CanonicalCondition.EQ, unorderedIsTrue, __forX, __forY, __view);
        if (__value != null)
        {
            return __value;
        }
        return this;
    }

    // @class FloatEqualsNode.FloatEqualsOp
    public static final class FloatEqualsOp extends CompareOp
    {
        @Override
        public LogicNode canonical(ConstantReflectionProvider __constantReflection, MetaAccessProvider __metaAccess, Integer __smallestCompareWidth, CanonicalCondition __condition, boolean __unorderedIsTrue, ValueNode __forX, ValueNode __forY, NodeView __view)
        {
            LogicNode __result = super.canonical(__constantReflection, __metaAccess, __smallestCompareWidth, __condition, __unorderedIsTrue, __forX, __forY, __view);
            if (__result != null)
            {
                return __result;
            }
            Stamp __xStampGeneric = __forX.stamp(__view);
            Stamp __yStampGeneric = __forY.stamp(__view);
            if (__xStampGeneric instanceof FloatStamp && __yStampGeneric instanceof FloatStamp)
            {
                FloatStamp __xStamp = (FloatStamp) __xStampGeneric;
                FloatStamp __yStamp = (FloatStamp) __yStampGeneric;
                if (GraphUtil.unproxify(__forX) == GraphUtil.unproxify(__forY) && __xStamp.isNonNaN() && __yStamp.isNonNaN())
                {
                    return LogicConstantNode.tautology();
                }
                else if (__xStamp.alwaysDistinct(__yStamp))
                {
                    return LogicConstantNode.contradiction();
                }
            }
            return null;
        }

        @Override
        protected CompareNode duplicateModified(ValueNode __newX, ValueNode __newY, boolean __unorderedIsTrue, NodeView __view)
        {
            if (__newX.stamp(__view) instanceof FloatStamp && __newY.stamp(__view) instanceof FloatStamp)
            {
                return new FloatEqualsNode(__newX, __newY);
            }
            else if (__newX.stamp(__view) instanceof IntegerStamp && __newY.stamp(__view) instanceof IntegerStamp)
            {
                return new IntegerEqualsNode(__newX, __newY);
            }
            throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public Stamp getSucceedingStampForX(boolean __negated, Stamp __xStamp, Stamp __yStamp)
    {
        if (!__negated)
        {
            return __xStamp.join(__yStamp);
        }
        return null;
    }

    @Override
    public Stamp getSucceedingStampForY(boolean __negated, Stamp __xStamp, Stamp __yStamp)
    {
        if (!__negated)
        {
            return __xStamp.join(__yStamp);
        }
        return null;
    }

    @Override
    public TriState tryFold(Stamp __xStampGeneric, Stamp __yStampGeneric)
    {
        if (__xStampGeneric instanceof FloatStamp && __yStampGeneric instanceof FloatStamp)
        {
            FloatStamp __xStamp = (FloatStamp) __xStampGeneric;
            FloatStamp __yStamp = (FloatStamp) __yStampGeneric;
            if (__xStamp.alwaysDistinct(__yStamp))
            {
                return TriState.FALSE;
            }
            else if (__xStamp.neverDistinct(__yStamp))
            {
                return TriState.TRUE;
            }
        }
        return TriState.UNKNOWN;
    }
}
