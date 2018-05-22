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

public class DerivedScaledInductionVariable extends DerivedInductionVariable
{
    private final ValueNode scale;
    private final ValueNode value;

    public DerivedScaledInductionVariable(LoopEx loop, InductionVariable base, ValueNode scale, ValueNode value)
    {
        super(loop, base);
        this.scale = scale;
        this.value = value;
    }

    public DerivedScaledInductionVariable(LoopEx loop, InductionVariable base, NegateNode value)
    {
        super(loop, base);
        this.scale = ConstantNode.forIntegerStamp(value.stamp(NodeView.DEFAULT), -1, value.graph());
        this.value = value;
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
        Stamp stamp = scale.stamp(NodeView.DEFAULT);
        if (stamp instanceof IntegerStamp)
        {
            IntegerStamp integerStamp = (IntegerStamp) stamp;
            if (integerStamp.isStrictlyPositive())
            {
                return base.direction();
            }
            else if (integerStamp.isStrictlyNegative())
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
    public ValueNode extremumNode(boolean assumePositiveTripCount, Stamp stamp)
    {
        return MathUtil.mul(graph(), base.extremumNode(assumePositiveTripCount, stamp), IntegerConvertNode.convert(scale, stamp, graph(), NodeView.DEFAULT));
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

    @Override
    public String toString()
    {
        return String.format("DerivedScaleInductionVariable base (%s) %s %s", base, value.getNodeClass().shortName(), scale);
    }
}
