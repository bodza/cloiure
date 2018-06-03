package giraaff.loop;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaConstant;

import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.util.UnsignedLong;
import giraaff.loop.InductionVariable.Direction;
import giraaff.loop.MathUtil;
import giraaff.nodes.AbstractBeginNode;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.GuardNode;
import giraaff.nodes.IfNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.CompareNode;
import giraaff.nodes.calc.ConditionalNode;
import giraaff.nodes.calc.IntegerLessThanNode;
import giraaff.nodes.calc.NegateNode;
import giraaff.nodes.extended.GuardingNode;

// @class CountedLoopInfo
public final class CountedLoopInfo
{
    // @field
    private final LoopEx loop;
    // @field
    private InductionVariable iv;
    // @field
    private ValueNode end;
    // @field
    private boolean oneOff;
    // @field
    private AbstractBeginNode body;
    // @field
    private IfNode ifNode;

    // @cons
    CountedLoopInfo(LoopEx __loop, InductionVariable __iv, IfNode __ifNode, ValueNode __end, boolean __oneOff, AbstractBeginNode __body)
    {
        super();
        this.loop = __loop;
        this.iv = __iv;
        this.end = __end;
        this.oneOff = __oneOff;
        this.body = __body;
        this.ifNode = __ifNode;
    }

    /**
     * Returns a node that computes the maximum trip count of this loop. That is the trip count of
     * this loop assuming it is not exited by an other exit than the {@linkplain #getLimitTest()
     * count check}.
     *
     * This count is exact if {@link #isExactTripCount()} returns true.
     *
     * THIS VALUE SHOULD BE TREATED AS UNSIGNED.
     */
    public ValueNode maxTripCountNode()
    {
        return maxTripCountNode(false);
    }

    /**
     * Returns a node that computes the maximum trip count of this loop. That is the trip count of
     * this loop assuming it is not exited by an other exit than the {@linkplain #getLimitTest()
     * count check}.
     *
     * This count is exact if {@link #isExactTripCount()} returns true.
     *
     * THIS VALUE SHOULD BE TREATED AS UNSIGNED.
     *
     * @param assumePositive if true the check that the loop is entered at all will be omitted.
     */
    public ValueNode maxTripCountNode(boolean __assumePositive)
    {
        StructuredGraph __graph = iv.valueNode().graph();
        Stamp __stamp = iv.valueNode().stamp(NodeView.DEFAULT);

        ValueNode __max;
        ValueNode __min;
        ValueNode __range;
        ValueNode __absStride;
        if (iv.direction() == Direction.Up)
        {
            __absStride = iv.strideNode();
            __range = MathUtil.sub(__graph, end, iv.initNode());
            __max = end;
            __min = iv.initNode();
        }
        else
        {
            __absStride = __graph.maybeAddOrUnique(NegateNode.create(iv.strideNode(), NodeView.DEFAULT));
            __range = MathUtil.sub(__graph, iv.initNode(), end);
            __max = iv.initNode();
            __min = end;
        }

        ConstantNode __one = ConstantNode.forIntegerStamp(__stamp, 1, __graph);
        if (oneOff)
        {
            __range = MathUtil.add(__graph, __range, __one);
        }
        // round-away-from-zero divison: (range + stride -/+ 1) / stride
        ValueNode __denominator = MathUtil.add(__graph, __range, MathUtil.sub(__graph, __absStride, __one));
        ValueNode __div = MathUtil.unsignedDivBefore(__graph, loop.entryPoint(), __denominator, __absStride);

        if (__assumePositive)
        {
            return __div;
        }
        ConstantNode __zero = ConstantNode.forIntegerStamp(__stamp, 0, __graph);
        return __graph.unique(new ConditionalNode(__graph.unique(new IntegerLessThanNode(__max, __min)), __zero, __div));
    }

    /**
     * @return true if the loop has constant bounds.
     */
    public boolean isConstantMaxTripCount()
    {
        return end instanceof ConstantNode && iv.isConstantInit() && iv.isConstantStride();
    }

    public UnsignedLong constantMaxTripCount()
    {
        return new UnsignedLong(rawConstantMaxTripCount());
    }

    /**
     * Compute the raw value of the trip count for this loop. THIS IS AN UNSIGNED VALUE;
     */
    private long rawConstantMaxTripCount()
    {
        long __endValue = end.asJavaConstant().asLong();
        long __initValue = iv.constantInit();
        long __range;
        long __absStride;
        if (iv.direction() == Direction.Up)
        {
            if (__endValue < __initValue)
            {
                return 0;
            }
            __range = __endValue - iv.constantInit();
            __absStride = iv.constantStride();
        }
        else
        {
            if (__initValue < __endValue)
            {
                return 0;
            }
            __range = iv.constantInit() - __endValue;
            __absStride = -iv.constantStride();
        }
        if (oneOff)
        {
            __range += 1;
        }
        long __denominator = __range + __absStride - 1;
        return Long.divideUnsigned(__denominator, __absStride);
    }

    public boolean isExactTripCount()
    {
        return loop.loopBegin().loopExits().count() == 1;
    }

    public ValueNode exactTripCountNode()
    {
        return maxTripCountNode();
    }

    public boolean isConstantExactTripCount()
    {
        return isConstantMaxTripCount();
    }

    public UnsignedLong constantExactTripCount()
    {
        return constantMaxTripCount();
    }

    public ValueNode getLimit()
    {
        return end;
    }

    public IfNode getLimitTest()
    {
        return ifNode;
    }

    public ValueNode getStart()
    {
        return iv.initNode();
    }

    public boolean isLimitIncluded()
    {
        return oneOff;
    }

    public AbstractBeginNode getBody()
    {
        return body;
    }

    public Direction getDirection()
    {
        return iv.direction();
    }

    public InductionVariable getCounter()
    {
        return iv;
    }

    public GuardingNode getOverFlowGuard()
    {
        return loop.loopBegin().getOverflowGuard();
    }

    public GuardingNode createOverFlowGuard()
    {
        GuardingNode __overflowGuard = getOverFlowGuard();
        if (__overflowGuard != null)
        {
            return __overflowGuard;
        }
        IntegerStamp __stamp = (IntegerStamp) iv.valueNode().stamp(NodeView.DEFAULT);
        StructuredGraph __graph = iv.valueNode().graph();
        CompareNode __cond; // we use a negated guard with a < condition to achieve a >=
        ConstantNode __one = ConstantNode.forIntegerStamp(__stamp, 1, __graph);
        if (iv.direction() == Direction.Up)
        {
            ValueNode __v1 = MathUtil.sub(__graph, ConstantNode.forIntegerStamp(__stamp, CodeUtil.maxValue(__stamp.getBits()), __graph), MathUtil.sub(__graph, iv.strideNode(), __one));
            if (oneOff)
            {
                __v1 = MathUtil.sub(__graph, __v1, __one);
            }
            __cond = __graph.unique(new IntegerLessThanNode(__v1, end));
        }
        else
        {
            ValueNode __v1 = MathUtil.add(__graph, ConstantNode.forIntegerStamp(__stamp, CodeUtil.minValue(__stamp.getBits()), __graph), MathUtil.sub(__graph, __one, iv.strideNode()));
            if (oneOff)
            {
                __v1 = MathUtil.add(__graph, __v1, __one);
            }
            __cond = __graph.unique(new IntegerLessThanNode(end, __v1));
        }
        __overflowGuard = __graph.unique(new GuardNode(__cond, AbstractBeginNode.prevBegin(loop.entryPoint()), DeoptimizationReason.LoopLimitCheck, DeoptimizationAction.InvalidateRecompile, true, JavaConstant.NULL_POINTER)); // TODO use speculation
        loop.loopBegin().setOverflowGuard(__overflowGuard);
        return __overflowGuard;
    }

    public IntegerStamp getStamp()
    {
        return (IntegerStamp) iv.valueNode().stamp(NodeView.DEFAULT);
    }
}
