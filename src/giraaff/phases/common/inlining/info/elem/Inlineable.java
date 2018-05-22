package giraaff.phases.common.inlining.info.elem;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.nodes.Invoke;
import giraaff.phases.common.CanonicalizerPhase;
import giraaff.phases.tiers.HighTierContext;

public interface Inlineable
{
    static Inlineable getInlineableElement(final ResolvedJavaMethod method, Invoke invoke, HighTierContext context, CanonicalizerPhase canonicalizer)
    {
        return new InlineableGraph(method, invoke, context, canonicalizer);
    }

    int getNodeCount();

    Iterable<Invoke> getInvokes();

    double getProbability(Invoke invoke);
}
