package giraaff.nodes.calc;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.TriState;

import giraaff.core.common.calc.CanonicalCondition;
import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.graph.NodeClass;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.LogicConstantNode;
import giraaff.nodes.LogicNegationNode;
import giraaff.nodes.LogicNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.util.GraphUtil;

/**
 * Common super-class for "a < b" comparisons both {@linkplain IntegerLowerThanNode signed} and
 * {@linkplain IntegerBelowNode unsigned}.
 */
// @class IntegerLowerThanNode
public abstract class IntegerLowerThanNode extends CompareNode
{
    // @def
    public static final NodeClass<IntegerLowerThanNode> TYPE = NodeClass.create(IntegerLowerThanNode.class);

    // @field
    private final LowerOp op;

    // @cons
    protected IntegerLowerThanNode(NodeClass<? extends CompareNode> __c, ValueNode __x, ValueNode __y, LowerOp __op)
    {
        super(__c, __op.getCondition(), false, __x, __y);
        this.op = __op;
    }

    protected LowerOp getOp()
    {
        return op;
    }

    @Override
    public Stamp getSucceedingStampForX(boolean __negated, Stamp __xStampGeneric, Stamp __yStampGeneric)
    {
        return getSucceedingStampForX(__negated, !__negated, __xStampGeneric, __yStampGeneric, getX(), getY());
    }

    @Override
    public Stamp getSucceedingStampForY(boolean __negated, Stamp __xStampGeneric, Stamp __yStampGeneric)
    {
        return getSucceedingStampForX(!__negated, !__negated, __yStampGeneric, __xStampGeneric, getY(), getX());
    }

    private Stamp getSucceedingStampForX(boolean __mirror, boolean __strict, Stamp __xStampGeneric, Stamp __yStampGeneric, ValueNode __forX, ValueNode __forY)
    {
        Stamp __s = getSucceedingStampForX(__mirror, __strict, __xStampGeneric, __yStampGeneric);
        if (__s != null && __s.isUnrestricted())
        {
            __s = null;
        }
        if (__forY instanceof AddNode && __xStampGeneric instanceof IntegerStamp)
        {
            IntegerStamp __xStamp = (IntegerStamp) __xStampGeneric;
            AddNode __addNode = (AddNode) __forY;
            IntegerStamp __aStamp = null;
            if (__addNode.getX() == __forX && __addNode.getY().stamp(NodeView.DEFAULT) instanceof IntegerStamp)
            {
                // x < x + a
                __aStamp = (IntegerStamp) __addNode.getY().stamp(NodeView.DEFAULT);
            }
            else if (__addNode.getY() == __forX && __addNode.getX().stamp(NodeView.DEFAULT) instanceof IntegerStamp)
            {
                // x < a + x
                __aStamp = (IntegerStamp) __addNode.getX().stamp(NodeView.DEFAULT);
            }
            if (__aStamp != null)
            {
                IntegerStamp __result = getOp().getSucceedingStampForXLowerXPlusA(__mirror, __strict, __aStamp);
                __result = (IntegerStamp) __xStamp.tryImproveWith(__result);
                if (__result != null)
                {
                    if (__s != null)
                    {
                        __s = __s.improveWith(__result);
                    }
                    else
                    {
                        __s = __result;
                    }
                }
            }
        }
        return __s;
    }

    private Stamp getSucceedingStampForX(boolean __mirror, boolean __strict, Stamp __xStampGeneric, Stamp __yStampGeneric)
    {
        if (__xStampGeneric instanceof IntegerStamp)
        {
            IntegerStamp __xStamp = (IntegerStamp) __xStampGeneric;
            if (__yStampGeneric instanceof IntegerStamp)
            {
                IntegerStamp __yStamp = (IntegerStamp) __yStampGeneric;
                Stamp __s = getOp().getSucceedingStampForX(__xStamp, __yStamp, __mirror, __strict);
                if (__s != null)
                {
                    return __s;
                }
            }
        }
        return null;
    }

    @Override
    public TriState tryFold(Stamp __xStampGeneric, Stamp __yStampGeneric)
    {
        return getOp().tryFold(__xStampGeneric, __yStampGeneric);
    }

    // @class IntegerLowerThanNode.LowerOp
    public abstract static class LowerOp extends CompareOp
    {
        @Override
        public LogicNode canonical(ConstantReflectionProvider __constantReflection, MetaAccessProvider __metaAccess, Integer __smallestCompareWidth, CanonicalCondition __condition, boolean __unorderedIsTrue, ValueNode __forX, ValueNode __forY, NodeView __view)
        {
            LogicNode __result = super.canonical(__constantReflection, __metaAccess, __smallestCompareWidth, __condition, __unorderedIsTrue, __forX, __forY, __view);
            if (__result != null)
            {
                return __result;
            }
            LogicNode __synonym = findSynonym(__forX, __forY, __view);
            if (__synonym != null)
            {
                return __synonym;
            }
            return null;
        }

        protected abstract long upperBound(IntegerStamp stamp);

        protected abstract long lowerBound(IntegerStamp stamp);

        protected abstract int compare(long a, long b);

        protected abstract long min(long a, long b);

        protected abstract long max(long a, long b);

        protected long min(long __a, long __b, int __bits)
        {
            return min(cast(__a, __bits), cast(__b, __bits));
        }

        protected long max(long __a, long __b, int __bits)
        {
            return max(cast(__a, __bits), cast(__b, __bits));
        }

        protected abstract long cast(long a, int bits);

        protected abstract long minValue(int bits);

        protected abstract long maxValue(int bits);

        protected abstract IntegerStamp forInteger(int bits, long min, long max);

        protected abstract CanonicalCondition getCondition();

        protected abstract IntegerLowerThanNode createNode(ValueNode x, ValueNode y);

        public LogicNode create(ValueNode __x, ValueNode __y, NodeView __view)
        {
            LogicNode __result = CompareNode.tryConstantFoldPrimitive(getCondition(), __x, __y, false, __view);
            if (__result != null)
            {
                return __result;
            }
            else
            {
                __result = findSynonym(__x, __y, __view);
                if (__result != null)
                {
                    return __result;
                }
                return createNode(__x, __y);
            }
        }

        protected LogicNode findSynonym(ValueNode __forX, ValueNode __forY, NodeView __view)
        {
            if (GraphUtil.unproxify(__forX) == GraphUtil.unproxify(__forY))
            {
                return LogicConstantNode.contradiction();
            }
            TriState __fold = tryFold(__forX.stamp(__view), __forY.stamp(__view));
            if (__fold.isTrue())
            {
                return LogicConstantNode.tautology();
            }
            else if (__fold.isFalse())
            {
                return LogicConstantNode.contradiction();
            }
            if (__forY.stamp(__view) instanceof IntegerStamp)
            {
                IntegerStamp __yStamp = (IntegerStamp) __forY.stamp(__view);
                int __bits = __yStamp.getBits();
                if (__forX.isJavaConstant() && !__forY.isConstant())
                {
                    // bring the constant on the right
                    long __xValue = __forX.asJavaConstant().asLong();
                    if (__xValue != maxValue(__bits))
                    {
                        // c < x <=> !(c >= x) <=> !(x <= c) <=> !(x < c + 1)
                        return LogicNegationNode.create(create(__forY, ConstantNode.forIntegerStamp(__yStamp, __xValue + 1), __view));
                    }
                }
                if (__forY.isJavaConstant())
                {
                    long __yValue = __forY.asJavaConstant().asLong();
                    if (__yValue == maxValue(__bits))
                    {
                        // x < MAX <=> x != MAX
                        return LogicNegationNode.create(IntegerEqualsNode.create(__forX, __forY, __view));
                    }
                    if (__yValue == minValue(__bits) + 1)
                    {
                        // x < MIN + 1 <=> x <= MIN <=> x == MIN
                        return IntegerEqualsNode.create(__forX, ConstantNode.forIntegerStamp(__yStamp, minValue(__bits)), __view);
                    }
                }
                else if (__forY instanceof AddNode)
                {
                    AddNode __addNode = (AddNode) __forY;
                    LogicNode __canonical = canonicalizeXLowerXPlusA(__forX, __addNode, false, true, __view);
                    if (__canonical != null)
                    {
                        return __canonical;
                    }
                }
                if (__forX instanceof AddNode)
                {
                    AddNode __addNode = (AddNode) __forX;
                    LogicNode __canonical = canonicalizeXLowerXPlusA(__forY, __addNode, true, false, __view);
                    if (__canonical != null)
                    {
                        return __canonical;
                    }
                }
            }
            return null;
        }

        private LogicNode canonicalizeXLowerXPlusA(ValueNode __forX, AddNode __addNode, boolean __mirrored, boolean __strict, NodeView __view)
        {
            // x < x + a
            IntegerStamp __succeedingXStamp;
            boolean __exact;
            if (__addNode.getX() == __forX && __addNode.getY().stamp(__view) instanceof IntegerStamp)
            {
                IntegerStamp __aStamp = (IntegerStamp) __addNode.getY().stamp(__view);
                __succeedingXStamp = getSucceedingStampForXLowerXPlusA(__mirrored, __strict, __aStamp);
                __exact = __aStamp.lowerBound() == __aStamp.upperBound();
            }
            else if (__addNode.getY() == __forX && __addNode.getX().stamp(__view) instanceof IntegerStamp)
            {
                IntegerStamp __aStamp = (IntegerStamp) __addNode.getX().stamp(__view);
                __succeedingXStamp = getSucceedingStampForXLowerXPlusA(__mirrored, __strict, __aStamp);
                __exact = __aStamp.lowerBound() == __aStamp.upperBound();
            }
            else
            {
                return null;
            }
            if (__succeedingXStamp.join(__forX.stamp(__view)).isEmpty())
            {
                return LogicConstantNode.contradiction();
            }
            else if (__exact && !__succeedingXStamp.isEmpty())
            {
                int __bits = __succeedingXStamp.getBits();
                if (compare(lowerBound(__succeedingXStamp), minValue(__bits)) > 0)
                {
                    // x must be in [L..MAX] <=> x >= L <=> !(x < L)
                    return LogicNegationNode.create(create(__forX, ConstantNode.forIntegerStamp(__succeedingXStamp, lowerBound(__succeedingXStamp)), __view));
                }
                else if (compare(upperBound(__succeedingXStamp), maxValue(__bits)) < 0)
                {
                    // x must be in [MIN..H] <=> x <= H <=> !(H < x)
                    return LogicNegationNode.create(create(ConstantNode.forIntegerStamp(__succeedingXStamp, upperBound(__succeedingXStamp)), __forX, __view));
                }
            }
            return null;
        }

        protected TriState tryFold(Stamp __xStampGeneric, Stamp __yStampGeneric)
        {
            if (__xStampGeneric instanceof IntegerStamp && __yStampGeneric instanceof IntegerStamp)
            {
                IntegerStamp __xStamp = (IntegerStamp) __xStampGeneric;
                IntegerStamp __yStamp = (IntegerStamp) __yStampGeneric;
                if (compare(upperBound(__xStamp), lowerBound(__yStamp)) < 0)
                {
                    return TriState.TRUE;
                }
                if (compare(lowerBound(__xStamp), upperBound(__yStamp)) >= 0)
                {
                    return TriState.FALSE;
                }
            }
            return TriState.UNKNOWN;
        }

        protected IntegerStamp getSucceedingStampForX(IntegerStamp __xStamp, IntegerStamp __yStamp, boolean __mirror, boolean __strict)
        {
            int __bits = __xStamp.getBits();
            if (__mirror)
            {
                long __low = lowerBound(__yStamp);
                if (__strict)
                {
                    if (__low == maxValue(__bits))
                    {
                        return null;
                    }
                    __low += 1;
                }
                if (compare(__low, lowerBound(__xStamp)) > 0 || upperBound(__xStamp) != (__xStamp.upperBound() & CodeUtil.mask(__xStamp.getBits())))
                {
                    return forInteger(__bits, __low, upperBound(__xStamp));
                }
            }
            else
            {
                // x < y, i.e., x < y <= Y_UPPER_BOUND so x <= Y_UPPER_BOUND - 1
                long __low = upperBound(__yStamp);
                if (__strict)
                {
                    if (__low == minValue(__bits))
                    {
                        return null;
                    }
                    __low -= 1;
                }
                if (compare(__low, upperBound(__xStamp)) < 0 || lowerBound(__xStamp) != (__xStamp.lowerBound() & CodeUtil.mask(__xStamp.getBits())))
                {
                    return forInteger(__bits, lowerBound(__xStamp), __low);
                }
            }
            return null;
        }

        protected IntegerStamp getSucceedingStampForXLowerXPlusA(boolean __mirrored, boolean __strict, IntegerStamp __a)
        {
            int __bits = __a.getBits();
            long __min = minValue(__bits);
            long __max = maxValue(__bits);
            /*
             * if x < x + a <=> x + a didn't overflow:
             *
             * x is outside ]MAX - a, MAX], i.e., inside [MIN, MAX - a]
             *
             * if a is negative those bounds wrap around correctly.
             *
             * If a is exactly zero this gives an unbounded stamp (any integer) in the positive case
             * and an empty stamp in the negative case: if x |<| x is true, then either x has no
             * value or any value...
             *
             * This does not use upper/lowerBound from LowerOp because it's about the (signed)
             * addition not the comparison.
             */
            if (__mirrored)
            {
                if (__a.contains(0))
                {
                    // a may be zero
                    return __a.unrestricted();
                }
                return forInteger(__bits, min(__max - __a.lowerBound() + 1, __max - __a.upperBound() + 1, __bits), __max);
            }
            else
            {
                long __aLower = __a.lowerBound();
                long __aUpper = __a.upperBound();
                if (__strict)
                {
                    if (__aLower == 0)
                    {
                        __aLower = 1;
                    }
                    if (__aUpper == 0)
                    {
                        __aUpper = -1;
                    }
                    if (__aLower > __aUpper)
                    {
                        // impossible
                        return __a.empty();
                    }
                }
                if (__aLower < 0 && __aUpper > 0)
                {
                    // a may be zero
                    return __a.unrestricted();
                }
                return forInteger(__bits, __min, max(__max - __aLower, __max - __aUpper, __bits));
            }
        }
    }
}
