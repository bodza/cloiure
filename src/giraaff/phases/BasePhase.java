package giraaff.phases;

import giraaff.nodes.StructuredGraph;

/**
 * Base class for all compiler phases. Subclasses should be stateless. There will be one global
 * instance for each compiler phase that is shared for all compilations. VM-, target- and
 * compilation-specific data can be passed with a context object.
 */
// @class BasePhase
public abstract class BasePhase<C>
{
    // @cons
    protected BasePhase()
    {
        super();
    }

    public final void apply(final StructuredGraph __graph, final C __context)
    {
        this.run(__graph, __context);
    }

    protected CharSequence getName()
    {
        return new ClassTypeSequence(BasePhase.this.getClass());
    }

    protected abstract void run(StructuredGraph graph, C context);
}
