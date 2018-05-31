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
    private final InliningPolicy inliningPolicy;
    private final CanonicalizerPhase canonicalizer;
    private LinkedList<Invoke> rootInvokes = null;

    private int maxMethodPerInlining = Integer.MAX_VALUE;

    // @cons
    public InliningPhase(CanonicalizerPhase canonicalizer)
    {
        this(new GreedyInliningPolicy(null), canonicalizer);
    }

    // @cons
    public InliningPhase(Map<Invoke, Double> hints, CanonicalizerPhase canonicalizer)
    {
        this(new GreedyInliningPolicy(hints), canonicalizer);
    }

    // @cons
    public InliningPhase(InliningPolicy policy, CanonicalizerPhase canonicalizer)
    {
        super();
        this.inliningPolicy = policy;
        this.canonicalizer = canonicalizer;
    }

    public CanonicalizerPhase getCanonicalizer()
    {
        return canonicalizer;
    }

    public void setMaxMethodsPerInlining(int max)
    {
        maxMethodPerInlining = max;
    }

    public void setRootInvokes(LinkedList<Invoke> rootInvokes)
    {
        this.rootInvokes = rootInvokes;
    }

    /**
     * This method sets in motion the inlining machinery.
     *
     * @see InliningData
     * @see InliningData#moveForward()
     */
    @Override
    protected void run(final StructuredGraph graph, final HighTierContext context)
    {
        final InliningData data = new InliningData(graph, context, maxMethodPerInlining, canonicalizer, inliningPolicy, rootInvokes);

        int count = 0;
        int limit = GraalOptions.methodInlineBailoutLimit;
        while (data.hasUnprocessedGraphs())
        {
            boolean wasInlined = data.moveForward();
            count++;
            if (!wasInlined)
            {
                if (limit > 0 && count == limit)
                {
                    // limit the amount of exploration which is done
                    break;
                }
            }
        }
    }
}
