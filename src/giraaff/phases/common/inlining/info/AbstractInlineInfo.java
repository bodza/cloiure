package giraaff.phases.common.inlining.info;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import org.graalvm.collections.EconomicSet;

import giraaff.graph.Node;
import giraaff.nodes.Invoke;
import giraaff.nodes.StructuredGraph;
import giraaff.options.OptionValues;
import giraaff.phases.common.CanonicalizerPhase;
import giraaff.phases.common.inlining.InliningUtil;
import giraaff.phases.common.inlining.info.elem.Inlineable;
import giraaff.phases.common.inlining.info.elem.InlineableGraph;
import giraaff.phases.tiers.HighTierContext;

public abstract class AbstractInlineInfo implements InlineInfo
{
    protected final Invoke invoke;

    public AbstractInlineInfo(Invoke invoke)
    {
        this.invoke = invoke;
    }

    @Override
    public StructuredGraph graph()
    {
        return invoke.asNode().graph();
    }

    @Override
    public Invoke invoke()
    {
        return invoke;
    }

    protected static EconomicSet<Node> inline(Invoke invoke, ResolvedJavaMethod concrete, Inlineable inlineable, boolean receiverNullCheck, String reason)
    {
        StructuredGraph calleeGraph = ((InlineableGraph) inlineable).getGraph();
        return InliningUtil.inlineForCanonicalization(invoke, calleeGraph, receiverNullCheck, concrete, reason, "InliningPhase");
    }

    @Override
    public final void populateInlinableElements(HighTierContext context, StructuredGraph caller, CanonicalizerPhase canonicalizer, OptionValues options)
    {
        for (int i = 0; i < numberOfMethods(); i++)
        {
            Inlineable elem = Inlineable.getInlineableElement(methodAt(i), invoke, context, canonicalizer);
            setInlinableElement(i, elem);
        }
    }

    @Override
    public final int determineNodeCount()
    {
        int nodes = 0;
        for (int i = 0; i < numberOfMethods(); i++)
        {
            Inlineable elem = inlineableElementAt(i);
            if (elem != null)
            {
                nodes += elem.getNodeCount();
            }
        }
        return nodes;
    }
}
