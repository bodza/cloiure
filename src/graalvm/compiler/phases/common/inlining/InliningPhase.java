package graalvm.compiler.phases.common.inlining;

import java.util.LinkedList;
import java.util.Map;

import graalvm.compiler.nodes.Invoke;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.options.Option;
import graalvm.compiler.options.OptionType;
import graalvm.compiler.options.OptionKey;
import graalvm.compiler.phases.common.AbstractInliningPhase;
import graalvm.compiler.phases.common.CanonicalizerPhase;
import graalvm.compiler.phases.common.inlining.policy.GreedyInliningPolicy;
import graalvm.compiler.phases.common.inlining.policy.InliningPolicy;
import graalvm.compiler.phases.common.inlining.walker.InliningData;
import graalvm.compiler.phases.tiers.HighTierContext;

public class InliningPhase extends AbstractInliningPhase {

    public static class Options {

        @Option(help = "Unconditionally inline intrinsics", type = OptionType.Debug)//
        public static final OptionKey<Boolean> AlwaysInlineIntrinsics = new OptionKey<>(false);

        /**
         * This is a defensive measure against known pathologies of the inliner where the breadth of
         * the inlining call tree exploration can be wide enough to prevent inlining from completing
         * in reasonable time.
         */
        @Option(help = "Per-compilation method inlining exploration limit before giving up (use 0 to disable)", type = OptionType.Debug)//
        public static final OptionKey<Integer> MethodInlineBailoutLimit = new OptionKey<>(5000);
    }

    private final InliningPolicy inliningPolicy;
    private final CanonicalizerPhase canonicalizer;
    private LinkedList<Invoke> rootInvokes = null;

    private int maxMethodPerInlining = Integer.MAX_VALUE;

    public InliningPhase(CanonicalizerPhase canonicalizer) {
        this(new GreedyInliningPolicy(null), canonicalizer);
    }

    public InliningPhase(Map<Invoke, Double> hints, CanonicalizerPhase canonicalizer) {
        this(new GreedyInliningPolicy(hints), canonicalizer);
    }

    public InliningPhase(InliningPolicy policy, CanonicalizerPhase canonicalizer) {
        this.inliningPolicy = policy;
        this.canonicalizer = canonicalizer;
    }

    public CanonicalizerPhase getCanonicalizer() {
        return canonicalizer;
    }

    @Override
    public float codeSizeIncrease() {
        return 10_000f;
    }

    public void setMaxMethodsPerInlining(int max) {
        maxMethodPerInlining = max;
    }

    public void setRootInvokes(LinkedList<Invoke> rootInvokes) {
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
    protected void run(final StructuredGraph graph, final HighTierContext context) {
        final InliningData data = new InliningData(graph, context, maxMethodPerInlining, canonicalizer, inliningPolicy, rootInvokes);

        int count = 0;
        assert data.repOK();
        int limit = Options.MethodInlineBailoutLimit.getValue(graph.getOptions());
        while (data.hasUnprocessedGraphs()) {
            boolean wasInlined = data.moveForward();
            assert data.repOK();
            count++;
            if (!wasInlined) {
                if (limit > 0 && count == limit) {
                    // Limit the amount of exploration which is done
                    break;
                }
            }
        }

        assert data.inliningDepth() == 0 || count == limit;
        assert data.graphCount() == 0 || count == limit;
    }

}
