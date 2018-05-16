package graalvm.compiler.nodes.calc;

import graalvm.compiler.core.common.NumUtil;
import graalvm.compiler.core.common.calc.CanonicalCondition;
import graalvm.compiler.core.common.type.IntegerStamp;
import graalvm.compiler.core.common.type.StampFactory;
import graalvm.compiler.graph.Node;
import graalvm.compiler.graph.NodeClass;
import graalvm.compiler.graph.spi.CanonicalizerTool;
import graalvm.compiler.nodeinfo.NodeInfo;
import graalvm.compiler.nodes.LogicNode;
import graalvm.compiler.nodes.NodeView;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.options.OptionValues;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;

@NodeInfo(shortName = "|<|")
public final class IntegerBelowNode extends IntegerLowerThanNode
{
    public static final NodeClass<IntegerBelowNode> TYPE = NodeClass.create(IntegerBelowNode.class);
    private static final BelowOp OP = new BelowOp();

    public IntegerBelowNode(ValueNode x, ValueNode y)
    {
        super(TYPE, x, y, OP);
        assert x.stamp(NodeView.DEFAULT) instanceof IntegerStamp;
        assert y.stamp(NodeView.DEFAULT) instanceof IntegerStamp;
    }

    public static LogicNode create(ValueNode x, ValueNode y, NodeView view)
    {
        return OP.create(x, y, view);
    }

    public static LogicNode create(ConstantReflectionProvider constantReflection, MetaAccessProvider metaAccess, OptionValues options, Integer smallestCompareWidth, ValueNode x, ValueNode y, NodeView view)
    {
        LogicNode value = OP.canonical(constantReflection, metaAccess, options, smallestCompareWidth, OP.getCondition(), false, x, y, view);
        if (value != null)
        {
            return value;
        }
        return create(x, y, view);
    }

    @Override
    public Node canonical(CanonicalizerTool tool, ValueNode forX, ValueNode forY)
    {
        NodeView view = NodeView.from(tool);
        ValueNode value = OP.canonical(tool.getConstantReflection(), tool.getMetaAccess(), tool.getOptions(), tool.smallestCompareWidth(), OP.getCondition(), false, forX, forY, view);
        if (value != null)
        {
            return value;
        }
        return this;
    }

    public static class BelowOp extends LowerOp
    {
        @Override
        protected CompareNode duplicateModified(ValueNode newX, ValueNode newY, boolean unorderedIsTrue, NodeView view)
        {
            assert newX.stamp(NodeView.DEFAULT) instanceof IntegerStamp && newY.stamp(NodeView.DEFAULT) instanceof IntegerStamp;
            return new IntegerBelowNode(newX, newY);
        }

        @Override
        protected long upperBound(IntegerStamp stamp)
        {
            return stamp.unsignedUpperBound();
        }

        @Override
        protected long lowerBound(IntegerStamp stamp)
        {
            return stamp.unsignedLowerBound();
        }

        @Override
        protected int compare(long a, long b)
        {
            return Long.compareUnsigned(a, b);
        }

        @Override
        protected long min(long a, long b)
        {
            return NumUtil.minUnsigned(a, b);
        }

        @Override
        protected long max(long a, long b)
        {
            return NumUtil.maxUnsigned(a, b);
        }

        @Override
        protected long cast(long a, int bits)
        {
            return CodeUtil.zeroExtend(a, bits);
        }

        @Override
        protected long minValue(int bits)
        {
            return 0;
        }

        @Override
        protected long maxValue(int bits)
        {
            return NumUtil.maxValueUnsigned(bits);
        }

        @Override
        protected IntegerStamp forInteger(int bits, long min, long max)
        {
            return StampFactory.forUnsignedInteger(bits, min, max);
        }

        @Override
        protected CanonicalCondition getCondition()
        {
            return CanonicalCondition.BT;
        }

        @Override
        protected IntegerLowerThanNode createNode(ValueNode x, ValueNode y)
        {
            return new IntegerBelowNode(x, y);
        }
    }
}
