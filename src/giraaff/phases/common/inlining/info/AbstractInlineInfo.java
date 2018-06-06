package giraaff.phases.common.inlining.info;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import org.graalvm.collections.EconomicSet;

import giraaff.graph.Node;
import giraaff.nodes.Invoke;
import giraaff.nodes.StructuredGraph;
import giraaff.phases.common.CanonicalizerPhase;
import giraaff.phases.common.inlining.InliningUtil;
import giraaff.phases.common.inlining.info.elem.Inlineable;
import giraaff.phases.common.inlining.info.elem.InlineableGraph;
import giraaff.phases.tiers.HighTierContext;

// @class AbstractInlineInfo
public abstract class AbstractInlineInfo implements InlineInfo
{
    // @field
    protected final Invoke ___invoke;

    // @cons AbstractInlineInfo
    public AbstractInlineInfo(Invoke __invoke)
    {
        super();
        this.___invoke = __invoke;
    }

    @Override
    public StructuredGraph graph()
    {
        return this.___invoke.asNode().graph();
    }

    @Override
    public Invoke invoke()
    {
        return this.___invoke;
    }

    protected static EconomicSet<Node> inline(Invoke __invoke, ResolvedJavaMethod __concrete, Inlineable __inlineable, boolean __receiverNullCheck, String __reason)
    {
        StructuredGraph __calleeGraph = ((InlineableGraph) __inlineable).getGraph();
        return InliningUtil.inlineForCanonicalization(__invoke, __calleeGraph, __receiverNullCheck, __concrete, __reason, "InliningPhase");
    }

    @Override
    public final void populateInlinableElements(HighTierContext __context, StructuredGraph __caller, CanonicalizerPhase __canonicalizer)
    {
        for (int __i = 0; __i < numberOfMethods(); __i++)
        {
            Inlineable __elem = Inlineable.getInlineableElement(methodAt(__i), this.___invoke, __context, __canonicalizer);
            setInlinableElement(__i, __elem);
        }
    }

    @Override
    public final int determineNodeCount()
    {
        int __nodes = 0;
        for (int __i = 0; __i < numberOfMethods(); __i++)
        {
            Inlineable __elem = inlineableElementAt(__i);
            if (__elem != null)
            {
                __nodes += __elem.getNodeCount();
            }
        }
        return __nodes;
    }
}
