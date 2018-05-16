package graalvm.compiler.nodes.calc;

import static graalvm.compiler.nodeinfo.NodeCycles.CYCLES_2;

import graalvm.compiler.core.common.calc.CanonicalCondition;
import graalvm.compiler.core.common.type.FloatStamp;
import graalvm.compiler.core.common.type.IntegerStamp;
import graalvm.compiler.core.common.type.Stamp;
import graalvm.compiler.debug.GraalError;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.LogicConstantNode;
import graalvm.compiler.nodes.LogicNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.util.GraphUtil;
import graalvm.compiler.options.OptionValues;

import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.TriState;

@NodeInfo(shortName = "<", cycles = CYCLES_2)
public final class FloatLessThanNode extends CompareNode
{
    public static final NodeClass<FloatLessThanNode> TYPE = NodeClass.create(FloatLessThanNode.class);
    private static final FloatLessThanOp OP = new FloatLessThanOp();

    public FloatLessThanNode(ValueNode x, ValueNode y, boolean unorderedIsTrue)
    {
        super(TYPE, CanonicalCondition.LT, unorderedIsTrue, x, y);
        assert x.stamp(NodeView.DEFAULT) instanceof FloatStamp && y.stamp(NodeView.DEFAULT) instanceof FloatStamp;
        assert x.stamp(NodeView.DEFAULT).isCompatible(y.stamp(NodeView.DEFAULT));
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
