package giraaff.loop;

import giraaff.core.common.type.Stamp;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.IntegerConvertNode;

// @class DerivedConvertedInductionVariable
public final class DerivedConvertedInductionVariable extends DerivedInductionVariable
{
    // @field
    private final Stamp stamp;
    // @field
    private final ValueNode value;

    // @cons
    public DerivedConvertedInductionVariable(LoopEx __loop, InductionVariable __base, Stamp __stamp, ValueNode __value)
    {
        super(__loop, __base);
        this.stamp = __stamp;
        this.value = __value;
    }

    @Override
    public ValueNode valueNode()
    {
        return value;
    }

    @Override
    public Direction direction()
    {
        return base.direction();
    }

    @Override
    public ValueNode initNode()
    {
        return IntegerConvertNode.convert(base.initNode(), stamp, graph(), NodeView.DEFAULT);
    }

    @Override
    public ValueNode strideNode()
    {
        return IntegerConvertNode.convert(base.strideNode(), stamp, graph(), NodeView.DEFAULT);
    }

    @Override
    public boolean isConstantInit()
    {
        return base.isConstantInit();
    }

    @Override
    public boolean isConstantStride()
    {
        return base.isConstantStride();
    }

    @Override
    public long constantInit()
    {
        return base.constantInit();
    }

    @Override
    public long constantStride()
    {
        return base.constantStride();
    }

    @Override
    public ValueNode extremumNode(boolean __assumePositiveTripCount, Stamp __s)
    {
        return base.extremumNode(__assumePositiveTripCount, __s);
    }

    @Override
    public ValueNode exitValueNode()
    {
        return IntegerConvertNode.convert(base.exitValueNode(), stamp, graph(), NodeView.DEFAULT);
    }

    @Override
    public boolean isConstantExtremum()
    {
        return base.isConstantExtremum();
    }

    @Override
    public long constantExtremum()
    {
        return base.constantExtremum();
    }

    @Override
    public void deleteUnusedNodes()
    {
    }
}
