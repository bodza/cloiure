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
    private final InliningPolicy ___inliningPolicy;
    // @field
    private final CanonicalizerPhase ___canonicalizer;
    // @field
    private LinkedList<Invoke> ___rootInvokes = null;

    // @field
    private int ___maxMethodPerInlining = Integer.MAX_VALUE;

    // @cons InliningPhase
    public InliningPhase(CanonicalizerPhase __canonicalizer)
    {
        this(new GreedyInliningPolicy(null), __canonicalizer);
    }

    // @cons InliningPhase
    public InliningPhase(Map<Invoke, Double> __hints, CanonicalizerPhase __canonicalizer)
    {
        this(new GreedyInliningPolicy(__hints), __canonicalizer);
    }

    // @cons InliningPhase
    public InliningPhase(InliningPolicy __policy, CanonicalizerPhase __canonicalizer)
    {
        super();
        this.___inliningPolicy = __policy;
        this.___canonicalizer = __canonicalizer;
    }

    public CanonicalizerPhase getCanonicalizer()
    {
        return this.___canonicalizer;
    }

    public void setMaxMethodsPerInlining(int __max)
    {
        this.___maxMethodPerInlining = __max;
    }

    public void setRootInvokes(LinkedList<Invoke> __rootInvokes)
    {
        this.___rootInvokes = __rootInvokes;
    }

    ///
    // This method sets in motion the inlining machinery.
    //
    // @see InliningData
    // @see InliningData#moveForward()
    ///
    @Override
    protected void run(final StructuredGraph __graph, final HighTierContext __context)
    {
        final InliningData __data = new InliningData(__graph, __context, this.___maxMethodPerInlining, this.___canonicalizer, this.___inliningPolicy, this.___rootInvokes);

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
