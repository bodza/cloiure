package giraaff.loop;

import giraaff.nodes.StructuredGraph;

/**
 * Base class of the derived induction variables.
 */
// @class DerivedInductionVariable
public abstract class DerivedInductionVariable extends InductionVariable
{
    protected final InductionVariable base;

    // @cons
    public DerivedInductionVariable(LoopEx loop, InductionVariable base)
    {
        super(loop);
        this.base = base;
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
