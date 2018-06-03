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
    private final LoopEx ___loop;
    // @field
    private InductionVariable ___iv;
    // @field
    private ValueNode ___end;
    // @field
    private boolean ___oneOff;
    // @field
    private AbstractBeginNode ___body;
    // @field
    private IfNode ___ifNode;

    // @cons
    CountedLoopInfo(LoopEx __loop, InductionVariable __iv, IfNode __ifNode, ValueNode __end, boolean __oneOff, AbstractBeginNode __body)
    {
        super();
        this.___loop = __loop;
        this.___iv = __iv;
        this.___end = __end;
        this.___oneOff = __oneOff;
        this.___body = __body;
        this.___ifNode = __ifNode;
    }

    ///
    // Returns a node that computes the maximum trip count of this loop. That is the trip count of
    // this loop assuming it is not exited by an other exit than the {@linkplain #getLimitTest()
    // count check}.
    //
    // This count is exact if {@link #isExactTripCount()} returns true.
    //
    // THIS VALUE SHOULD BE TREATED AS UNSIGNED.
    ///
    public ValueNode maxTripCountNode()
    {
        return maxTripCountNode(false);
    }

    ///
    // Returns a node that computes the maximum trip count of this loop. That is the trip count of
    // this loop assuming it is not exited by an other exit than the {@linkplain #getLimitTest()
    // count check}.
    //
    // This count is exact if {@link #isExactTripCount()} returns true.
    //
    // THIS VALUE SHOULD BE TREATED AS UNSIGNED.
    //
    // @param assumePositive if true the check that the loop is entered at all will be omitted.
    ///
    public ValueNode maxTripCountNode(boolean __assumePositive)
    {
        StructuredGraph __graph = this.___iv.valueNode().graph();
        Stamp __stamp = this.___iv.valueNode().stamp(NodeView.DEFAULT);

        ValueNode __max;
        ValueNode __min;
        ValueNode __range;
        ValueNode __absStride;
        if (this.___iv.direction() == Direction.Up)
        {
            __absStride = this.___iv.strideNode();
            __range = MathUtil.sub(__graph, this.___end, this.___iv.initNode());
            __max = this.___end;
            __min = this.___iv.initNode();
        }
        else
        {
            __absStride = __graph.maybeAddOrUnique(NegateNode.create(this.___iv.strideNode(), NodeView.DEFAULT));
            __range = MathUtil.sub(__graph, this.___iv.initNode(), this.___end);
            __max = this.___iv.initNode();
            __min = this.___end;
        }

        ConstantNode __one = ConstantNode.forIntegerStamp(__stamp, 1, __graph);
        if (this.___oneOff)
        {
            __range = MathUtil.add(__graph, __range, __one);
        }
        // round-away-from-zero divison: (range + stride -/+ 1) / stride
        ValueNode __denominator = MathUtil.add(__graph, __range, MathUtil.sub(__graph, __absStride, __one));
        ValueNode __div = MathUtil.unsignedDivBefore(__graph, this.___loop.entryPoint(), __denominator, __absStride);

        if (__assumePositive)
        {
            return __div;
        }
        ConstantNode __zero = ConstantNode.forIntegerStamp(__stamp, 0, __graph);
        return __graph.unique(new ConditionalNode(__graph.unique(new IntegerLessThanNode(__max, __min)), __zero, __div));
    }

    ///
    // @return true if the loop has constant bounds.
    ///
    public boolean isConstantMaxTripCount()
    {
        return this.___end instanceof ConstantNode && this.___iv.isConstantInit() && this.___iv.isConstantStride();
    }

    public UnsignedLong constantMaxTripCount()
    {
        return new UnsignedLong(rawConstantMaxTripCount());
    }

    ///
    // Compute the raw value of the trip count for this loop. THIS IS AN UNSIGNED VALUE;
    ///
    private long rawConstantMaxTripCount()
    {
        long __endValue = this.___end.asJavaConstant().asLong();
        long __initValue = this.___iv.constantInit();
        long __range;
        long __absStride;
        if (this.___iv.direction() == Direction.Up)
        {
            if (__endValue < __initValue)
            {
                return 0;
            }
            __range = __endValue - this.___iv.constantInit();
            __absStride = this.___iv.constantStride();
        }
        else
        {
            if (__initValue < __endValue)
            {
                return 0;
            }
            __range = this.___iv.constantInit() - __endValue;
            __absStride = -this.___iv.constantStride();
        }
        if (this.___oneOff)
        {
            __range += 1;
        }
        long __denominator = __range + __absStride - 1;
        return Long.divideUnsigned(__denominator, __absStride);
    }

    public boolean isExactTripCount()
    {
        return this.___loop.loopBegin().loopExits().count() == 1;
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
        return this.___end;
    }

    public IfNode getLimitTest()
    {
        return this.___ifNode;
    }

    public ValueNode getStart()
    {
        return this.___iv.initNode();
    }

    public boolean isLimitIncluded()
    {
        return this.___oneOff;
    }

    public AbstractBeginNode getBody()
    {
        return this.___body;
    }

    public Direction getDirection()
    {
        return this.___iv.direction();
    }

    public InductionVariable getCounter()
    {
        return this.___iv;
    }

    public GuardingNode getOverFlowGuard()
    {
        return this.___loop.loopBegin().getOverflowGuard();
    }

    public GuardingNode createOverFlowGuard()
    {
        GuardingNode __overflowGuard = getOverFlowGuard();
        if (__overflowGuard != null)
        {
            return __overflowGuard;
        }
        IntegerStamp __stamp = (IntegerStamp) this.___iv.valueNode().stamp(NodeView.DEFAULT);
        StructuredGraph __graph = this.___iv.valueNode().graph();
        CompareNode __cond; // we use a negated guard with a < condition to achieve a >=
        ConstantNode __one = ConstantNode.forIntegerStamp(__stamp, 1, __graph);
        if (this.___iv.direction() == Direction.Up)
        {
            ValueNode __v1 = MathUtil.sub(__graph, ConstantNode.forIntegerStamp(__stamp, CodeUtil.maxValue(__stamp.getBits()), __graph), MathUtil.sub(__graph, this.___iv.strideNode(), __one));
            if (this.___oneOff)
            {
                __v1 = MathUtil.sub(__graph, __v1, __one);
            }
            __cond = __graph.unique(new IntegerLessThanNode(__v1, this.___end));
        }
        else
        {
            ValueNode __v1 = MathUtil.add(__graph, ConstantNode.forIntegerStamp(__stamp, CodeUtil.minValue(__stamp.getBits()), __graph), MathUtil.sub(__graph, __one, this.___iv.strideNode()));
            if (this.___oneOff)
            {
                __v1 = MathUtil.add(__graph, __v1, __one);
            }
            __cond = __graph.unique(new IntegerLessThanNode(this.___end, __v1));
        }
        __overflowGuard = __graph.unique(new GuardNode(__cond, AbstractBeginNode.prevBegin(this.___loop.entryPoint()), DeoptimizationReason.LoopLimitCheck, DeoptimizationAction.InvalidateRecompile, true, JavaConstant.NULL_POINTER)); // TODO use speculation
        this.___loop.loopBegin().setOverflowGuard(__overflowGuard);
        return __overflowGuard;
    }

    public IntegerStamp getStamp()
    {
        return (IntegerStamp) this.___iv.valueNode().stamp(NodeView.DEFAULT);
    }
}
