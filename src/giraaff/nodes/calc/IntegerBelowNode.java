package giraaff.nodes.calc;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;

import giraaff.core.common.NumUtil;
import giraaff.core.common.calc.CanonicalCondition;
import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.StampFactory;
import giraaff.graph.Node;
import giraaff.graph.NodeClass;
import giraaff.graph.spi.CanonicalizerTool;
import giraaff.nodes.LogicNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;

// @class IntegerBelowNode
public final class IntegerBelowNode extends IntegerLowerThanNode
{
    // @def
    public static final NodeClass<IntegerBelowNode> TYPE = NodeClass.create(IntegerBelowNode.class);

    // @def
    private static final BelowOp OP = new BelowOp();

    // @cons
    public IntegerBelowNode(ValueNode __x, ValueNode __y)
    {
        super(TYPE, __x, __y, OP);
    }

    public static LogicNode create(ValueNode __x, ValueNode __y, NodeView __view)
    {
        return OP.create(__x, __y, __view);
    }

    public static LogicNode create(ConstantReflectionProvider __constantReflection, MetaAccessProvider __metaAccess, Integer __smallestCompareWidth, ValueNode __x, ValueNode __y, NodeView __view)
    {
        LogicNode __value = OP.canonical(__constantReflection, __metaAccess, __smallestCompareWidth, OP.getCondition(), __x, __y, __view);
        if (__value != null)
        {
            return __value;
        }
        return create(__x, __y, __view);
    }

    @Override
    public Node canonical(CanonicalizerTool __tool, ValueNode __forX, ValueNode __forY)
    {
        NodeView __view = NodeView.from(__tool);
        ValueNode __value = OP.canonical(__tool.getConstantReflection(), __tool.getMetaAccess(), __tool.smallestCompareWidth(), OP.getCondition(), __forX, __forY, __view);
        if (__value != null)
        {
            return __value;
        }
        return this;
    }

    // @class IntegerBelowNode.BelowOp
    public static final class BelowOp extends LowerOp
    {
        @Override
        protected CompareNode duplicateModified(ValueNode __newX, ValueNode __newY, NodeView __view)
        {
            return new IntegerBelowNode(__newX, __newY);
        }

        @Override
        protected long upperBound(IntegerStamp __stamp)
        {
            return __stamp.unsignedUpperBound();
        }

        @Override
        protected long lowerBound(IntegerStamp __stamp)
        {
            return __stamp.unsignedLowerBound();
        }

        @Override
        protected int compare(long __a, long __b)
        {
            return Long.compareUnsigned(__a, __b);
        }

        @Override
        protected long min(long __a, long __b)
        {
            return NumUtil.minUnsigned(__a, __b);
        }

        @Override
        protected long max(long __a, long __b)
        {
            return NumUtil.maxUnsigned(__a, __b);
        }

        @Override
        protected long cast(long __a, int __bits)
        {
            return CodeUtil.zeroExtend(__a, __bits);
        }

        @Override
        protected long minValue(int __bits)
        {
            return 0;
        }

        @Override
        protected long maxValue(int __bits)
        {
            return NumUtil.maxValueUnsigned(__bits);
        }

        @Override
        protected IntegerStamp forInteger(int __bits, long __min, long __max)
        {
            return StampFactory.forUnsignedInteger(__bits, __min, __max);
        }

        @Override
        protected CanonicalCondition getCondition()
        {
            return CanonicalCondition.BT;
        }

        @Override
        protected IntegerLowerThanNode createNode(ValueNode __x, ValueNode __y)
        {
            return new IntegerBelowNode(__x, __y);
        }
    }
}
