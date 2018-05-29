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
    private final ValueNode offset;
    private final BinaryArithmeticNode<?> value;

    // @cons
    public DerivedOffsetInductionVariable(LoopEx loop, InductionVariable base, ValueNode offset, BinaryArithmeticNode<?> value)
    {
        super(loop, base);
        this.offset = offset;
        this.value = value;
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
    public ValueNode extremumNode(boolean assumePositiveTripCount, Stamp stamp)
    {
        return op(base.extremumNode(assumePositiveTripCount, stamp), IntegerConvertNode.convert(offset, stamp, graph(), NodeView.DEFAULT));
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

    private long op(long b, long o)
    {
        if (value instanceof AddNode)
        {
            return b + o;
        }
        if (value instanceof SubNode)
        {
            if (base.valueNode() == value.getX())
            {
                return b - o;
            }
            else
            {
                return o - b;
            }
        }
        throw GraalError.shouldNotReachHere();
    }

    private ValueNode op(ValueNode b, ValueNode o)
    {
        if (value instanceof AddNode)
        {
            return MathUtil.add(graph(), b, o);
        }
        if (value instanceof SubNode)
        {
            if (base.valueNode() == value.getX())
            {
                return MathUtil.sub(graph(), b, o);
            }
            else
            {
                return MathUtil.sub(graph(), o, b);
            }
        }
        throw GraalError.shouldNotReachHere();
    }

    @Override
    public void deleteUnusedNodes()
    {
    }

    @Override
    public String toString()
    {
        return String.format("DerivedOffsetInductionVariable base (%s) %s %s", base, value.getNodeClass().shortName(), offset);
    }
}
