package giraaff.phases.common.inlining.info.elem;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.nodes.Invoke;
import giraaff.phases.common.CanonicalizerPhase;
import giraaff.phases.tiers.HighTierContext;

// @iface Inlineable
public interface Inlineable
{
    static Inlineable getInlineableElement(final ResolvedJavaMethod __method, Invoke __invoke, HighTierContext __context, CanonicalizerPhase __canonicalizer)
    {
        return new InlineableGraph(__method, __invoke, __context, __canonicalizer);
    }

    int getNodeCount();

    Iterable<Invoke> getInvokes();

    double getProbability(Invoke __invoke);
}
