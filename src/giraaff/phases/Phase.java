package giraaff.phases;

import giraaff.nodes.StructuredGraph;

/**
 * Base class for compiler phases that don't need a context object.
 */
// @class Phase
public abstract class Phase extends BasePhase<Object>
{
    // @cons
    protected Phase()
    {
        super();
    }

    public final void apply(final StructuredGraph graph)
    {
        apply(graph, null);
    }

    protected abstract void run(StructuredGraph graph);

    @Override
    protected final void run(StructuredGraph graph, Object context)
    {
        run(graph);
    }
}
