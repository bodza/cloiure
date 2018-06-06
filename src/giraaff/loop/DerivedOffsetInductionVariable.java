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
    private final ValueNode ___offset;
    // @field
    private final BinaryArithmeticNode<?> ___value;

    // @cons DerivedOffsetInductionVariable
    public DerivedOffsetInductionVariable(LoopEx __loop, InductionVariable __base, ValueNode __offset, BinaryArithmeticNode<?> __value)
    {
        super(__loop, __base);
        this.___offset = __offset;
        this.___value = __value;
    }

    public ValueNode getOffset()
    {
        return this.___offset;
    }

    @Override
    public InductionVariable.Direction direction()
    {
        return this.___base.direction();
    }

    @Override
    public ValueNode valueNode()
    {
        return this.___value;
    }

    @Override
    public boolean isConstantInit()
    {
        return this.___offset.isConstant() && this.___base.isConstantInit();
    }

    @Override
    public boolean isConstantStride()
    {
        return this.___base.isConstantStride();
    }

    @Override
    public long constantInit()
    {
        return op(this.___base.constantInit(), this.___offset.asJavaConstant().asLong());
    }

    @Override
    public long constantStride()
    {
        if (this.___value instanceof SubNode && this.___base.valueNode() == this.___value.getY())
        {
            return -this.___base.constantStride();
        }
        return this.___base.constantStride();
    }

    @Override
    public ValueNode initNode()
    {
        return op(this.___base.initNode(), this.___offset);
    }

    @Override
    public ValueNode strideNode()
    {
        if (this.___value instanceof SubNode && this.___base.valueNode() == this.___value.getY())
        {
            return graph().addOrUniqueWithInputs(NegateNode.create(this.___base.strideNode(), NodeView.DEFAULT));
        }
        return this.___base.strideNode();
    }

    @Override
    public ValueNode extremumNode(boolean __assumePositiveTripCount, Stamp __stamp)
    {
        return op(this.___base.extremumNode(__assumePositiveTripCount, __stamp), IntegerConvertNode.convert(this.___offset, __stamp, graph(), NodeView.DEFAULT));
    }

    @Override
    public ValueNode exitValueNode()
    {
        return op(this.___base.exitValueNode(), this.___offset);
    }

    @Override
    public boolean isConstantExtremum()
    {
        return this.___offset.isConstant() && this.___base.isConstantExtremum();
    }

    @Override
    public long constantExtremum()
    {
        return op(this.___base.constantExtremum(), this.___offset.asJavaConstant().asLong());
    }

    private long op(long __b, long __o)
    {
        if (this.___value instanceof AddNode)
        {
            return __b + __o;
        }
        if (this.___value instanceof SubNode)
        {
            if (this.___base.valueNode() == this.___value.getX())
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
        if (this.___value instanceof AddNode)
        {
            return MathUtil.add(graph(), __b, __o);
        }
        if (this.___value instanceof SubNode)
        {
            if (this.___base.valueNode() == this.___value.getX())
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
