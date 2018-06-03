package giraaff.loop;

import giraaff.nodes.StructuredGraph;

///
// Base class of the derived induction variables.
///
// @class DerivedInductionVariable
public abstract class DerivedInductionVariable extends InductionVariable
{
    // @field
    protected final InductionVariable ___base;

    // @cons
    public DerivedInductionVariable(LoopEx __loop, InductionVariable __base)
    {
        super(__loop);
        this.___base = __base;
    }

    @Override
    public StructuredGraph graph()
    {
        return this.___base.graph();
    }

    public InductionVariable getBase()
    {
        return this.___base;
    }
}
