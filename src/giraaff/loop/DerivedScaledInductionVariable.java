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
    private final ValueNode ___scale;
    // @field
    private final ValueNode ___value;

    // @cons DerivedScaledInductionVariable
    public DerivedScaledInductionVariable(LoopEx __loop, InductionVariable __base, ValueNode __scale, ValueNode __value)
    {
        super(__loop, __base);
        this.___scale = __scale;
        this.___value = __value;
    }

    // @cons DerivedScaledInductionVariable
    public DerivedScaledInductionVariable(LoopEx __loop, InductionVariable __base, NegateNode __value)
    {
        super(__loop, __base);
        this.___scale = ConstantNode.forIntegerStamp(__value.stamp(NodeView.DEFAULT), -1, __value.graph());
        this.___value = __value;
    }

    public ValueNode getScale()
    {
        return this.___scale;
    }

    @Override
    public ValueNode valueNode()
    {
        return this.___value;
    }

    @Override
    public InductionVariable.Direction direction()
    {
        Stamp __stamp = this.___scale.stamp(NodeView.DEFAULT);
        if (__stamp instanceof IntegerStamp)
        {
            IntegerStamp __integerStamp = (IntegerStamp) __stamp;
            if (__integerStamp.isStrictlyPositive())
            {
                return this.___base.direction();
            }
            else if (__integerStamp.isStrictlyNegative())
            {
                return this.___base.direction().opposite();
            }
        }
        return null;
    }

    @Override
    public ValueNode initNode()
    {
        return MathUtil.mul(graph(), this.___base.initNode(), this.___scale);
    }

    @Override
    public ValueNode strideNode()
    {
        return MathUtil.mul(graph(), this.___base.strideNode(), this.___scale);
    }

    @Override
    public boolean isConstantInit()
    {
        return this.___scale.isConstant() && this.___base.isConstantInit();
    }

    @Override
    public boolean isConstantStride()
    {
        return this.___scale.isConstant() && this.___base.isConstantStride();
    }

    @Override
    public long constantInit()
    {
        return this.___base.constantInit() * this.___scale.asJavaConstant().asLong();
    }

    @Override
    public long constantStride()
    {
        return this.___base.constantStride() * this.___scale.asJavaConstant().asLong();
    }

    @Override
    public ValueNode extremumNode(boolean __assumePositiveTripCount, Stamp __stamp)
    {
        return MathUtil.mul(graph(), this.___base.extremumNode(__assumePositiveTripCount, __stamp), IntegerConvertNode.convert(this.___scale, __stamp, graph(), NodeView.DEFAULT));
    }

    @Override
    public ValueNode exitValueNode()
    {
        return MathUtil.mul(graph(), this.___base.exitValueNode(), this.___scale);
    }

    @Override
    public boolean isConstantExtremum()
    {
        return this.___scale.isConstant() && this.___base.isConstantExtremum();
    }

    @Override
    public long constantExtremum()
    {
        return this.___base.constantExtremum() * this.___scale.asJavaConstant().asLong();
    }

    @Override
    public void deleteUnusedNodes()
    {
        GraphUtil.tryKillUnused(this.___scale);
    }
}
