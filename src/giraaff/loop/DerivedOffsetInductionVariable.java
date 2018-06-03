package giraaff.loop;

import giraaff.core.common.type.Stamp;
import giraaff.loop.MathUtil;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.AddNode;
import giraaff.nodes.calc.BinaryArithmeticNode;
import giraaff.nodes.calc.IntegerConvertNode;
import giraaff.nodes.calc.NegateNode;
import giraaff.nodes.calc.SubNode;
import giraaff.util.GraalError;

// @class DerivedOffsetInductionVariable
public final class DerivedOffsetInductionVariable extends DerivedInductionVariable
{
    // @field
    private final ValueNode offset;
    // @field
    private final BinaryArithmeticNode<?> value;

    // @cons
    public DerivedOffsetInductionVariable(LoopEx __loop, InductionVariable __base, ValueNode __offset, BinaryArithmeticNode<?> __value)
    {
        super(__loop, __base);
        this.offset = __offset;
        this.value = __value;
    }

    public ValueNode getOffset()
    {
        return offset;
    }

    @Override
    public Direction direction()
    {
        return base.direction();
    }

    @Override
    public ValueNode valueNode()
    {
        return value;
    }

    @Override
    public boolean isConstantInit()
    {
        return offset.isConstant() && base.isConstantInit();
    }

    @Override
    public boolean isConstantStride()
    {
        return base.isConstantStride();
    }

    @Override
    public long constantInit()
    {
        return op(base.constantInit(), offset.asJavaConstant().asLong());
    }

    @Override
    public long constantStride()
    {
        if (value instanceof SubNode && base.valueNode() == value.getY())
        {
            return -base.constantStride();
        }
        return base.constantStride();
    }

    @Override
    public ValueNode initNode()
    {
        return op(base.initNode(), offset);
    }

    @Override
    public ValueNode strideNode()
    {
        if (value instanceof SubNode && base.valueNode() == value.getY())
        {
            return graph().addOrUniqueWithInputs(NegateNode.create(base.strideNode(), NodeView.DEFAULT));
        }
        return base.strideNode();
    }

    @Override
    public ValueNode extremumNode(boolean __assumePositiveTripCount, Stamp __stamp)
    {
        return op(base.extremumNode(__assumePositiveTripCount, __stamp), IntegerConvertNode.convert(offset, __stamp, graph(), NodeView.DEFAULT));
    }

    @Override
    public ValueNode exitValueNode()
    {
        return op(base.exitValueNode(), offset);
    }

    @Override
    public boolean isConstantExtremum()
    {
        return offset.isConstant() && base.isConstantExtremum();
    }

    @Override
    public long constantExtremum()
    {
        return op(base.constantExtremum(), offset.asJavaConstant().asLong());
    }

    private long op(long __b, long __o)
    {
        if (value instanceof AddNode)
        {
            return __b + __o;
        }
        if (value instanceof SubNode)
        {
            if (base.valueNode() == value.getX())
            {
                return __b - __o;
            }
            else
            {
                return __o - __b;
            }
        }
        throw GraalError.shouldNotReachHere();
    }

    private ValueNode op(ValueNode __b, ValueNode __o)
    {
        if (value instanceof AddNode)
        {
            return MathUtil.add(graph(), __b, __o);
        }
        if (value instanceof SubNode)
        {
            if (base.valueNode() == value.getX())
            {
                return MathUtil.sub(graph(), __b, __o);
            }
            else
            {
                return MathUtil.sub(graph(), __o, __b);
            }
        }
        throw GraalError.shouldNotReachHere();
    }

    @Override
    public void deleteUnusedNodes()
    {
    }
}
