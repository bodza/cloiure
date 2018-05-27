package giraaff.phases.common.inlining;

import java.util.LinkedList;
import java.util.Map;

import giraaff.nodes.Invoke;
import giraaff.nodes.StructuredGraph;
import giraaff.options.OptionKey;
import giraaff.phases.common.AbstractInliningPhase;
import giraaff.phases.common.CanonicalizerPhase;
import giraaff.phases.common.inlining.policy.GreedyInliningPolicy;
import giraaff.phases.common.inlining.policy.InliningPolicy;
import giraaff.phases.common.inlining.walker.InliningData;
import giraaff.phases.tiers.HighTierContext;

public class InliningPhase extends AbstractInliningPhase
{
    public static class Options
    {
        // @Option "Unconditionally inline intrinsics."
        public static final OptionKey<Boolean> AlwaysInlineIntrinsics = new OptionKey<>(false);

        /**
         * This is a defensive measure against known pathologies of the inliner where the breadth of
         * the inlining call tree exploration can be wide enough to prevent inlining from completing
         * in reasonable time.
         */
        // @Option "Per-compilation method inlining exploration limit before giving up (use 0 to disable)."
        public static final OptionKey<Integer> MethodInlineBailoutLimit = new OptionKey<>(5000);
    }

    private final InliningPolicy inliningPolicy;
    private final CanonicalizerPhase canonicalizer;
    private LinkedList<Invoke> rootInvokes = null;

    private int maxMethodPerInlining = Integer.MAX_VALUE;

    public InliningPhase(CanonicalizerPhase canonicalizer)
    {
        this(new GreedyInliningPolicy(null), canonicalizer);
    }

    public InliningPhase(Map<Invoke, Double> hints, CanonicalizerPhase canonicalizer)
    {
        this(new GreedyInliningPolicy(hints), canonicalizer);
    }

    public InliningPhase(InliningPolicy policy, CanonicalizerPhase canonicalizer)
    {
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
     *
     * This method sets in motion the inlining machinery.
     *
     * @see InliningData
     * @see InliningData#moveForward()
     *
     */
    @Override
    protected void run(final StructuredGraph graph, final HighTierContext context)
    {
        final InliningData data = new InliningData(graph, context, maxMethodPerInlining, canonicalizer, inliningPolicy, rootInvokes);

        int count = 0;
        int limit = Options.MethodInlineBailoutLimit.getValue(graph.getOptions());
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
