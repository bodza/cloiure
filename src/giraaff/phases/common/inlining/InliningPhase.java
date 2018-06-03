package giraaff.phases.common.inlining;

import java.util.LinkedList;
import java.util.Map;

import giraaff.core.common.GraalOptions;
import giraaff.nodes.Invoke;
import giraaff.nodes.StructuredGraph;
import giraaff.phases.common.AbstractInliningPhase;
import giraaff.phases.common.CanonicalizerPhase;
import giraaff.phases.common.inlining.policy.GreedyInliningPolicy;
import giraaff.phases.common.inlining.policy.InliningPolicy;
import giraaff.phases.common.inlining.walker.InliningData;
import giraaff.phases.tiers.HighTierContext;

// @class InliningPhase
public final class InliningPhase extends AbstractInliningPhase
{
    // @field
    private final InliningPolicy inliningPolicy;
    // @field
    private final CanonicalizerPhase canonicalizer;
    // @field
    private LinkedList<Invoke> rootInvokes = null;

    // @field
    private int maxMethodPerInlining = Integer.MAX_VALUE;

    // @cons
    public InliningPhase(CanonicalizerPhase __canonicalizer)
    {
        this(new GreedyInliningPolicy(null), __canonicalizer);
    }

    // @cons
    public InliningPhase(Map<Invoke, Double> __hints, CanonicalizerPhase __canonicalizer)
    {
        this(new GreedyInliningPolicy(__hints), __canonicalizer);
    }

    // @cons
    public InliningPhase(InliningPolicy __policy, CanonicalizerPhase __canonicalizer)
    {
        super();
        this.inliningPolicy = __policy;
        this.canonicalizer = __canonicalizer;
    }

    public CanonicalizerPhase getCanonicalizer()
    {
        return canonicalizer;
    }

    public void setMaxMethodsPerInlining(int __max)
    {
        maxMethodPerInlining = __max;
    }

    public void setRootInvokes(LinkedList<Invoke> __rootInvokes)
    {
        this.rootInvokes = __rootInvokes;
    }

    /**
     * This method sets in motion the inlining machinery.
     *
     * @see InliningData
     * @see InliningData#moveForward()
     */
    @Override
    protected void run(final StructuredGraph __graph, final HighTierContext __context)
    {
        final InliningData __data = new InliningData(__graph, __context, maxMethodPerInlining, canonicalizer, inliningPolicy, rootInvokes);

        int __count = 0;
        int __limit = GraalOptions.methodInlineBailoutLimit;
        while (__data.hasUnprocessedGraphs())
        {
            boolean __wasInlined = __data.moveForward();
            __count++;
            if (!__wasInlined)
            {
                if (__limit > 0 && __count == __limit)
                {
                    // limit the amount of exploration which is done
                    break;
                }
            }
        }
    }
}
