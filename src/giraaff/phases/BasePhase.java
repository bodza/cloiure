package giraaff.phases;

import giraaff.graph.Graph.Mark;
import giraaff.nodes.StructuredGraph;
import giraaff.options.OptionKey;
import giraaff.options.OptionValues;

/**
 * Base class for all compiler phases. Subclasses should be stateless. There will be one global
 * instance for each compiler phase that is shared for all compilations. VM-, target- and
 * compilation-specific data can be passed with a context object.
 */
public abstract class BasePhase<C>
{
    protected BasePhase()
    {
    }

    public final void apply(final StructuredGraph graph, final C context)
    {
        this.run(graph, context);
    }

    protected CharSequence getName()
    {
        return new ClassTypeSequence(BasePhase.this.getClass());
    }

    protected abstract void run(StructuredGraph graph, C context);
}
