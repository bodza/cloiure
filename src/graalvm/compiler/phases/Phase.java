package graalvm.compiler.phases;

import graalvm.compiler.nodes.StructuredGraph;

/**
 * Base class for compiler phases that don't need a context object.
 */
public abstract class Phase extends BasePhase<Object> {

    protected Phase() {
    }

    public final void apply(final StructuredGraph graph) {
        apply(graph, true);
    }

    public final void apply(final StructuredGraph graph, final boolean dumpGraph) {
        apply(graph, null, dumpGraph);
    }

    protected abstract void run(StructuredGraph graph);

    @Override
    protected final void run(StructuredGraph graph, Object context) {
        run(graph);
    }
}