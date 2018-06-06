package giraaff.loop;

import giraaff.core.common.type.Stamp;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.IntegerConvertNode;

// @class DerivedConvertedInductionVariable
public final class DerivedConvertedInductionVariable extends DerivedInductionVariable
{
    // @field
    private final Stamp ___stamp;
    // @field
    private final ValueNode ___value;

    // @cons DerivedConvertedInductionVariable
    public DerivedConvertedInductionVariable(LoopEx __loop, InductionVariable __base, Stamp __stamp, ValueNode __value)
    {
        super(__loop, __base);
        this.___stamp = __stamp;
        this.___value = __value;
    }

    @Override
    public ValueNode valueNode()
    {
        return this.___value;
    }

    @Override
    public InductionVariable.Direction direction()
    {
        return this.___base.direction();
    }

    @Override
    public ValueNode initNode()
    {
        return IntegerConvertNode.convert(this.___base.initNode(), this.___stamp, graph(), NodeView.DEFAULT);
    }

    @Override
    public ValueNode strideNode()
    {
        return IntegerConvertNode.convert(this.___base.strideNode(), this.___stamp, graph(), NodeView.DEFAULT);
    }

    @Override
    public boolean isConstantInit()
    {
        return this.___base.isConstantInit();
    }

    @Override
    public boolean isConstantStride()
    {
        return this.___base.isConstantStride();
    }

    @Override
    public long constantInit()
    {
        return this.___base.constantInit();
    }

    @Override
    public long constantStride()
    {
        return this.___base.constantStride();
    }

    @Override
    public ValueNode extremumNode(boolean __assumePositiveTripCount, Stamp __s)
    {
        return this.___base.extremumNode(__assumePositiveTripCount, __s);
    }

    @Override
    public ValueNode exitValueNode()
    {
        return IntegerConvertNode.convert(this.___base.exitValueNode(), this.___stamp, graph(), NodeView.DEFAULT);
    }

    @Override
    public boolean isConstantExtremum()
    {
        return this.___base.isConstantExtremum();
    }

    @Override
    public long constantExtremum()
    {
        return this.___base.constantExtremum();
    }

    @Override
    public void deleteUnusedNodes()
    {
    }
}
