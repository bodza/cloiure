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
    private final ValuePhiNode phi;
    // @field
    private final ValueNode init;
    // @field
    private ValueNode rawStride;
    // @field
    private BinaryArithmeticNode<?> op;

    // @cons
    public BasicInductionVariable(LoopEx __loop, ValuePhiNode __phi, ValueNode __init, ValueNode __rawStride, BinaryArithmeticNode<?> __op)
    {
        super(__loop);
        this.phi = __phi;
        this.init = __init;
        this.rawStride = __rawStride;
        this.op = __op;
    }

    @Override
    public StructuredGraph graph()
    {
        return phi.graph();
    }

    public BinaryArithmeticNode<?> getOp()
    {
        return op;
    }

    public void setOP(BinaryArithmeticNode<?> __newOp)
    {
        rawStride = __newOp.getY();
        op = __newOp;
    }

    @Override
    public Direction direction()
    {
        Stamp __stamp = rawStride.stamp(NodeView.DEFAULT);
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
                if (op instanceof AddNode)
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
        return phi;
    }

    @Override
    public ValueNode initNode()
    {
        return init;
    }

    @Override
    public ValueNode strideNode()
    {
        if (op instanceof AddNode)
        {
            return rawStride;
        }
        if (op instanceof SubNode)
        {
            return graph().unique(new NegateNode(rawStride));
        }
        throw GraalError.shouldNotReachHere();
    }

    @Override
    public boolean isConstantInit()
    {
        return init.isConstant();
    }

    @Override
    public boolean isConstantStride()
    {
        return rawStride.isConstant();
    }

    @Override
    public long constantInit()
    {
        return init.asJavaConstant().asLong();
    }

    @Override
    public long constantStride()
    {
        if (op instanceof AddNode)
        {
            return rawStride.asJavaConstant().asLong();
        }
        if (op instanceof SubNode)
        {
            return -rawStride.asJavaConstant().asLong();
        }
        throw GraalError.shouldNotReachHere();
    }

    @Override
    public ValueNode extremumNode(boolean __assumePositiveTripCount, Stamp __stamp)
    {
        Stamp __fromStamp = phi.stamp(NodeView.DEFAULT);
        StructuredGraph __graph = graph();
        ValueNode __stride = strideNode();
        ValueNode __initNode = this.initNode();
        if (!__fromStamp.isCompatible(__stamp))
        {
            __stride = IntegerConvertNode.convert(__stride, __stamp, graph(), NodeView.DEFAULT);
            __initNode = IntegerConvertNode.convert(__initNode, __stamp, graph(), NodeView.DEFAULT);
        }
        ValueNode __maxTripCount = loop.counted().maxTripCountNode(__assumePositiveTripCount);
        if (!__maxTripCount.stamp(NodeView.DEFAULT).isCompatible(__stamp))
        {
            __maxTripCount = IntegerConvertNode.convert(__maxTripCount, __stamp, graph(), NodeView.DEFAULT);
        }
        return MathUtil.add(__graph, MathUtil.mul(__graph, __stride, MathUtil.sub(__graph, __maxTripCount, ConstantNode.forIntegerStamp(__stamp, 1, __graph))), __initNode);
    }

    @Override
    public ValueNode exitValueNode()
    {
        Stamp __stamp = phi.stamp(NodeView.DEFAULT);
        ValueNode __maxTripCount = loop.counted().maxTripCountNode();
        if (!__maxTripCount.stamp(NodeView.DEFAULT).isCompatible(__stamp))
        {
            __maxTripCount = IntegerConvertNode.convert(__maxTripCount, __stamp, graph(), NodeView.DEFAULT);
        }
        return MathUtil.add(graph(), MathUtil.mul(graph(), strideNode(), __maxTripCount), initNode());
    }

    @Override
    public boolean isConstantExtremum()
    {
        return isConstantInit() && isConstantStride() && loop.counted().isConstantMaxTripCount();
    }

    @Override
    public long constantExtremum()
    {
        UnsignedLong __tripCount = loop.counted().constantMaxTripCount();
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
