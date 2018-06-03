package giraaff.loop;

import giraaff.nodes.StructuredGraph;

/**
 * Base class of the derived induction variables.
 */
// @class DerivedInductionVariable
public abstract class DerivedInductionVariable extends InductionVariable
{
    // @field
    protected final InductionVariable base;

    // @cons
    public DerivedInductionVariable(LoopEx __loop, InductionVariable __base)
    {
        super(__loop);
        this.base = __base;
    }

    @Override
    public StructuredGraph graph()
    {
        return base.graph();
    }

    public InductionVariable getBase()
    {
        return base;
    }
}
