package graalvm.compiler.phases.common.inlining.info.elem;

import graalvm.compiler.nodes.Invoke;
import graalvm.compiler.phases.common.CanonicalizerPhase;
import graalvm.compiler.phases.tiers.HighTierContext;

import jdk.vm.ci.meta.ResolvedJavaMethod;

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
