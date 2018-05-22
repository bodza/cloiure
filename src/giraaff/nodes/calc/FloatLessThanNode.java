package giraaff.nodes.calc;

import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.TriState;

import giraaff.core.common.calc.CanonicalCondition;
import giraaff.core.common.type.FloatStamp;
import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.debug.GraalError;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.LogicConstantNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.util.GraphUtil;
import giraaff.options.OptionValues;

public final class FloatLessThanNode extends CompareNode
{
    public static final NodeClass<FloatLessThanNode> TYPE = NodeClass.create(FloatLessThanNode.class);
    private static final FloatLessThanOp OP = new FloatLessThanOp();

    public FloatLessThanNode(ValueNode x, ValueNode y, boolean unorderedIsTrue)
    {
        super(TYPE, CanonicalCondition.LT, unorderedIsTrue, x, y);
    }

    public static LogicNode create(ValueNode x, ValueNode y, boolean unorderedIsTrue, NodeView view)
    {
        LogicNode result = CompareNode.tryConstantFoldPrimitive(CanonicalCondition.LT, x, y, unorderedIsTrue, view);
        if (result != null)
        {
            return result;
        }
        return new FloatLessThanNode(x, y, unorderedIsTrue);
    }

    public static LogicNode create(ConstantReflectionProvider constantReflection, MetaAccessProvider metaAccess, OptionValues options, Integer smallestCompareWidth, ValueNode x, ValueNode y, boolean unorderedIsTrue, NodeView view)
    {
        LogicNode result = OP.canonical(constantReflection, metaAccess, options, smallestCompareWidth, CanonicalCondition.LT, unorderedIsTrue, x, y, view);
        if (result != null)
        {
            return result;
        }
        return create(x, y, unorderedIsTrue, view);
    }

    @Override
    public Node canonical(CanonicalizerTool tool, ValueNode forX, ValueNode forY)
    {
        NodeView view = NodeView.from(tool);
        ValueNode value = OP.canonical(tool.getConstantReflection(), tool.getMetaAccess(), tool.getOptions(), tool.smallestCompareWidth(), CanonicalCondition.LT, unorderedIsTrue, forX, forY, view);
        if (value != null)
        {
            return value;
        }
        return this;
    }

    public static class FloatLessThanOp extends CompareOp
    {
        @Override
        public LogicNode canonical(ConstantReflectionProvider constantReflection, MetaAccessProvider metaAccess, OptionValues options, Integer smallestCompareWidth, CanonicalCondition condition, boolean unorderedIsTrue, ValueNode forX, ValueNode forY, NodeView view)
        {
            LogicNode result = super.canonical(constantReflection, metaAccess, options, smallestCompareWidth, condition, unorderedIsTrue, forX, forY, view);
            if (result != null)
            {
                return result;
            }
            if (GraphUtil.unproxify(forX) == GraphUtil.unproxify(forY) && !unorderedIsTrue)
            {
                return LogicConstantNode.contradiction();
            }
            return null;
        }

        @Override
        protected CompareNode duplicateModified(ValueNode newX, ValueNode newY, boolean unorderedIsTrue, NodeView view)
        {
            if (newX.stamp(NodeView.DEFAULT) instanceof FloatStamp && newY.stamp(NodeView.DEFAULT) instanceof FloatStamp)
            {
                return new FloatLessThanNode(newX, newY, unorderedIsTrue);
            }
            else if (newX.stamp(NodeView.DEFAULT) instanceof IntegerStamp && newY.stamp(NodeView.DEFAULT) instanceof IntegerStamp)
            {
                return new IntegerLessThanNode(newX, newY);
            }
            throw GraalError.shouldNotReachHere();
        }
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
    public TriState tryFold(Stamp xStampGeneric, Stamp yStampGeneric)
    {
        return TriState.UNKNOWN;
    }
}
