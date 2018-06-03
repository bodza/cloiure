package giraaff.loop;

import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.util.UnsignedLong;
import giraaff.loop.MathUtil;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.ValuePhiNode;
import giraaff.nodes.calc.AddNode;
import giraaff.nodes.calc.BinaryArithmeticNode;
import giraaff.nodes.calc.IntegerConvertNode;
import giraaff.nodes.calc.NegateNode;
import giraaff.nodes.calc.SubNode;
import giraaff.util.GraalError;

// @class BasicInductionVariable
public final class BasicInductionVariable extends InductionVariable
{
    // @field
    private final ValuePhiNode ___phi;
    // @field
    private final ValueNode ___init;
    // @field
    private ValueNode ___rawStride;
    // @field
    private BinaryArithmeticNode<?> ___op;

    // @cons
    public BasicInductionVariable(LoopEx __loop, ValuePhiNode __phi, ValueNode __init, ValueNode __rawStride, BinaryArithmeticNode<?> __op)
    {
        super(__loop);
        this.___phi = __phi;
        this.___init = __init;
        this.___rawStride = __rawStride;
        this.___op = __op;
    }

    @Override
    public StructuredGraph graph()
    {
        return this.___phi.graph();
    }

    public BinaryArithmeticNode<?> getOp()
    {
        return this.___op;
    }

    public void setOP(BinaryArithmeticNode<?> __newOp)
    {
        this.___rawStride = __newOp.getY();
        this.___op = __newOp;
    }

    @Override
    public Direction direction()
    {
        Stamp __stamp = this.___rawStride.stamp(NodeView.DEFAULT);
        if (__stamp instanceof IntegerStamp)
        {
            IntegerStamp __integerStamp = (IntegerStamp) __stamp;
            Direction __dir = null;
            if (__integerStamp.isStrictlyPositive())
            {
                __dir = Direction.Up;
            }
            else if (__integerStamp.isStrictlyNegative())
            {
                __dir = Direction.Down;
            }
            if (__dir != null)
            {
                if (this.___op instanceof AddNode)
                {
                    return __dir;
                }
                else
                {
                    return __dir.opposite();
                }
            }
        }
        return null;
    }

    @Override
    public ValuePhiNode valueNode()
    {
        return this.___phi;
    }

    @Override
    public ValueNode initNode()
    {
        return this.___init;
    }

    @Override
    public ValueNode strideNode()
    {
        if (this.___op instanceof AddNode)
        {
            return this.___rawStride;
        }
        if (this.___op instanceof SubNode)
        {
            return graph().unique(new NegateNode(this.___rawStride));
        }
        throw GraalError.shouldNotReachHere();
    }

    @Override
    public boolean isConstantInit()
    {
        return this.___init.isConstant();
    }

    @Override
    public boolean isConstantStride()
    {
        return this.___rawStride.isConstant();
    }

    @Override
    public long constantInit()
    {
        return this.___init.asJavaConstant().asLong();
    }

    @Override
    public long constantStride()
    {
        if (this.___op instanceof AddNode)
        {
            return this.___rawStride.asJavaConstant().asLong();
        }
        if (this.___op instanceof SubNode)
        {
            return -this.___rawStride.asJavaConstant().asLong();
        }
        throw GraalError.shouldNotReachHere();
    }

    @Override
    public ValueNode extremumNode(boolean __assumePositiveTripCount, Stamp __stamp)
    {
        Stamp __fromStamp = this.___phi.stamp(NodeView.DEFAULT);
        StructuredGraph __graph = graph();
        ValueNode __stride = strideNode();
        ValueNode __initNode = this.initNode();
        if (!__fromStamp.isCompatible(__stamp))
        {
            __stride = IntegerConvertNode.convert(__stride, __stamp, graph(), NodeView.DEFAULT);
            __initNode = IntegerConvertNode.convert(__initNode, __stamp, graph(), NodeView.DEFAULT);
        }
        ValueNode __maxTripCount = this.___loop.counted().maxTripCountNode(__assumePositiveTripCount);
        if (!__maxTripCount.stamp(NodeView.DEFAULT).isCompatible(__stamp))
        {
            __maxTripCount = IntegerConvertNode.convert(__maxTripCount, __stamp, graph(), NodeView.DEFAULT);
        }
        return MathUtil.add(__graph, MathUtil.mul(__graph, __stride, MathUtil.sub(__graph, __maxTripCount, ConstantNode.forIntegerStamp(__stamp, 1, __graph))), __initNode);
    }

    @Override
    public ValueNode exitValueNode()
    {
        Stamp __stamp = this.___phi.stamp(NodeView.DEFAULT);
        ValueNode __maxTripCount = this.___loop.counted().maxTripCountNode();
        if (!__maxTripCount.stamp(NodeView.DEFAULT).isCompatible(__stamp))
        {
            __maxTripCount = IntegerConvertNode.convert(__maxTripCount, __stamp, graph(), NodeView.DEFAULT);
        }
        return MathUtil.add(graph(), MathUtil.mul(graph(), strideNode(), __maxTripCount), initNode());
    }

    @Override
    public boolean isConstantExtremum()
    {
        return isConstantInit() && isConstantStride() && this.___loop.counted().isConstantMaxTripCount();
    }

    @Override
    public long constantExtremum()
    {
        UnsignedLong __tripCount = this.___loop.counted().constantMaxTripCount();
        if (__tripCount.isLessThan(1))
        {
            return constantInit();
        }
        return __tripCount.minus(1).wrappingTimes(constantStride()).wrappingPlus(constantInit()).asLong();
    }

    @Override
    public void deleteUnusedNodes()
    {
    }
}
