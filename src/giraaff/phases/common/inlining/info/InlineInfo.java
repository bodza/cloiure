package giraaff.phases.common.inlining.info;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import org.graalvm.collections.EconomicSet;

import giraaff.graph.Node;
import giraaff.nodes.Invoke;
import giraaff.nodes.StructuredGraph;
import giraaff.options.OptionValues;
import giraaff.phases.common.CanonicalizerPhase;
import giraaff.phases.common.inlining.info.elem.Inlineable;
import giraaff.phases.tiers.HighTierContext;
import giraaff.phases.util.Providers;

/**
 * Represents an opportunity for inlining at a given invoke, with the given weight and level. The
 * weight is the amortized weight of the additional code - so smaller is better. The level is the
 * number of nested inlinings that lead to this invoke.
 */
public interface InlineInfo
{
    /**
     * The graph containing the {@link #invoke() invocation} that may be inlined.
     */
    StructuredGraph graph();

    /**
     * The invocation that may be inlined.
     */
    Invoke invoke();

    /**
     * Returns the number of methods that may be inlined by the {@link #invoke() invocation}. This
     * may be more than one in the case of a invocation profile showing a number of "hot" concrete
     * methods dispatched to by the invocation.
     */
    int numberOfMethods();

    ResolvedJavaMethod methodAt(int index);

    Inlineable inlineableElementAt(int index);

    double probabilityAt(int index);

    double relevanceAt(int index);

    void setInlinableElement(int index, Inlineable inlineableElement);

    /**
     * Performs the inlining described by this object and returns the node that represents the
     * return value of the inlined method (or null for void methods and methods that have no
     * non-exceptional exit).
     *
     * @return a collection of nodes that need to be canonicalized after the inlining
     */
    EconomicSet<Node> inline(Providers providers, String reason);

    /**
     * Try to make the call static bindable to avoid interface and virtual method calls.
     */
    void tryToDevirtualizeInvoke(Providers providers);

    boolean shouldInline();

    void populateInlinableElements(HighTierContext context, StructuredGraph caller, CanonicalizerPhase canonicalizer, OptionValues optionValues);

    int determineNodeCount();
}
