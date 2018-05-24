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
import giraaff.options.OptionValues;
import giraaff.util.GraalError;

public final class FloatEqualsNode extends CompareNode implements BinaryCommutative<ValueNode>
{
    public static final NodeClass<FloatEqualsNode> TYPE = NodeClass.create(FloatEqualsNode.class);
    private static final FloatEqualsOp OP = new FloatEqualsOp();

    public FloatEqualsNode(ValueNode x, ValueNode y)
    {
        super(TYPE, CanonicalCondition.EQ, false, x, y);
    }

    public static LogicNode create(ValueNode x, ValueNode y, NodeView view)
    {
        LogicNode result = CompareNode.tryConstantFoldPrimitive(CanonicalCondition.EQ, x, y, false, view);
        if (result != null)
        {
            return result;
        }
        else
        {
            return new FloatEqualsNode(x, y).maybeCommuteInputs();
        }
    }

    public static LogicNode create(ConstantReflectionProvider constantReflection, MetaAccessProvider metaAccess, OptionValues options, Integer smallestCompareWidth, ValueNode x, ValueNode y, NodeView view)
    {
        LogicNode value = OP.canonical(constantReflection, metaAccess, options, smallestCompareWidth, CanonicalCondition.EQ, false, x, y, view);
        if (value != null)
        {
            return value;
        }
        return create(x, y, view);
    }

    @Override
    public boolean isIdentityComparison()
    {
        FloatStamp xStamp = (FloatStamp) x.stamp(NodeView.DEFAULT);
        FloatStamp yStamp = (FloatStamp) y.stamp(NodeView.DEFAULT);
        /*
         * If both stamps have at most one 0.0 and it's the same 0.0 then this is an identity
         * comparison. FloatStamp isn't careful about tracking the presence of -0.0 so assume that
         * anything that includes 0.0 might include -0.0. So if either one is non-zero then it's an
         * identity comparison.
         */
        return (!xStamp.contains(0.0) || !yStamp.contains(0.0));
    }

    @Override
    public Node canonical(CanonicalizerTool tool, ValueNode forX, ValueNode forY)
    {
        NodeView view = NodeView.from(tool);
        ValueNode value = OP.canonical(tool.getConstantReflection(), tool.getMetaAccess(), tool.getOptions(), tool.smallestCompareWidth(), CanonicalCondition.EQ, unorderedIsTrue, forX, forY, view);
        if (value != null)
        {
            return value;
        }
        return this;
    }

    public static class FloatEqualsOp extends CompareOp
    {
        @Override
        public LogicNode canonical(ConstantReflectionProvider constantReflection, MetaAccessProvider metaAccess, OptionValues options, Integer smallestCompareWidth, CanonicalCondition condition, boolean unorderedIsTrue, ValueNode forX, ValueNode forY, NodeView view)
        {
            LogicNode result = super.canonical(constantReflection, metaAccess, options, smallestCompareWidth, condition, unorderedIsTrue, forX, forY, view);
            if (result != null)
            {
                return result;
            }
            Stamp xStampGeneric = forX.stamp(view);
            Stamp yStampGeneric = forY.stamp(view);
            if (xStampGeneric instanceof FloatStamp && yStampGeneric instanceof FloatStamp)
            {
                FloatStamp xStamp = (FloatStamp) xStampGeneric;
                FloatStamp yStamp = (FloatStamp) yStampGeneric;
                if (GraphUtil.unproxify(forX) == GraphUtil.unproxify(forY) && xStamp.isNonNaN() && yStamp.isNonNaN())
                {
                    return LogicConstantNode.tautology();
                }
                else if (xStamp.alwaysDistinct(yStamp))
                {
                    return LogicConstantNode.contradiction();
                }
            }
            return null;
        }

        @Override
        protected CompareNode duplicateModified(ValueNode newX, ValueNode newY, boolean unorderedIsTrue, NodeView view)
        {
            if (newX.stamp(view) instanceof FloatStamp && newY.stamp(view) instanceof FloatStamp)
            {
                return new FloatEqualsNode(newX, newY);
            }
            else if (newX.stamp(view) instanceof IntegerStamp && newY.stamp(view) instanceof IntegerStamp)
            {
                return new IntegerEqualsNode(newX, newY);
            }
            throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public Stamp getSucceedingStampForX(boolean negated, Stamp xStamp, Stamp yStamp)
    {
        if (!negated)
        {
            return xStamp.join(yStamp);
        }
        return null;
    }

    @Override
    public Stamp getSucceedingStampForY(boolean negated, Stamp xStamp, Stamp yStamp)
    {
        if (!negated)
        {
            return xStamp.join(yStamp);
        }
        return null;
    }

    @Override
    public TriState tryFold(Stamp xStampGeneric, Stamp yStampGeneric)
    {
        if (xStampGeneric instanceof FloatStamp && yStampGeneric instanceof FloatStamp)
        {
            FloatStamp xStamp = (FloatStamp) xStampGeneric;
            FloatStamp yStamp = (FloatStamp) yStampGeneric;
            if (xStamp.alwaysDistinct(yStamp))
            {
                return TriState.FALSE;
            }
            else if (xStamp.neverDistinct(yStamp))
            {
                return TriState.TRUE;
            }
        }
        return TriState.UNKNOWN;
    }
}
