package giraaff.loop;

import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.loop.MathUtil;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.IntegerConvertNode;
import giraaff.nodes.calc.NegateNode;
import giraaff.nodes.util.GraphUtil;

// @class DerivedScaledInductionVariable
public final class DerivedScaledInductionVariable extends DerivedInductionVariable
{
    // @field
    private final ValueNode scale;
    // @field
    private final ValueNode value;

    // @cons
    public DerivedScaledInductionVariable(LoopEx __loop, InductionVariable __base, ValueNode __scale, ValueNode __value)
    {
        super(__loop, __base);
        this.scale = __scale;
        this.value = __value;
    }

    // @cons
    public DerivedScaledInductionVariable(LoopEx __loop, InductionVariable __base, NegateNode __value)
    {
        super(__loop, __base);
        this.scale = ConstantNode.forIntegerStamp(__value.stamp(NodeView.DEFAULT), -1, __value.graph());
        this.value = __value;
    }

    public ValueNode getScale()
    {
        return scale;
    }

    @Override
    public ValueNode valueNode()
    {
        return value;
    }

    @Override
    public Direction direction()
    {
        Stamp __stamp = scale.stamp(NodeView.DEFAULT);
        if (__stamp instanceof IntegerStamp)
        {
            IntegerStamp __integerStamp = (IntegerStamp) __stamp;
            if (__integerStamp.isStrictlyPositive())
            {
                return base.direction();
            }
            else if (__integerStamp.isStrictlyNegative())
            {
                return base.direction().opposite();
            }
        }
        return null;
    }

    @Override
    public ValueNode initNode()
    {
        return MathUtil.mul(graph(), base.initNode(), scale);
    }

    @Override
    public ValueNode strideNode()
    {
        return MathUtil.mul(graph(), base.strideNode(), scale);
    }

    @Override
    public boolean isConstantInit()
    {
        return scale.isConstant() && base.isConstantInit();
    }

    @Override
    public boolean isConstantStride()
    {
        return scale.isConstant() && base.isConstantStride();
    }

    @Override
    public long constantInit()
    {
        return base.constantInit() * scale.asJavaConstant().asLong();
    }

    @Override
    public long constantStride()
    {
        return base.constantStride() * scale.asJavaConstant().asLong();
    }

    @Override
    public ValueNode extremumNode(boolean __assumePositiveTripCount, Stamp __stamp)
    {
        return MathUtil.mul(graph(), base.extremumNode(__assumePositiveTripCount, __stamp), IntegerConvertNode.convert(scale, __stamp, graph(), NodeView.DEFAULT));
    }

    @Override
    public ValueNode exitValueNode()
    {
        return MathUtil.mul(graph(), base.exitValueNode(), scale);
    }

    @Override
    public boolean isConstantExtremum()
    {
        return scale.isConstant() && base.isConstantExtremum();
    }

    @Override
    public long constantExtremum()
    {
        return base.constantExtremum() * scale.asJavaConstant().asLong();
    }

    @Override
    public void deleteUnusedNodes()
    {
        GraphUtil.tryKillUnused(scale);
    }
}
