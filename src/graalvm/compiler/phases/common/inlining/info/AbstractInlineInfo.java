package graalvm.compiler.phases.common.inlining.info;

import org.graalvm.collections.EconomicSet;
import graalvm.compiler.graph.Node;
import graalvm.compiler.nodes.Invoke;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.phases.common.CanonicalizerPhase;
import graalvm.compiler.phases.common.inlining.InliningUtil;
import graalvm.compiler.phases.common.inlining.info.elem.Inlineable;
import graalvm.compiler.phases.common.inlining.info.elem.InlineableGraph;
import graalvm.compiler.phases.tiers.HighTierContext;

import jdk.vm.ci.meta.ResolvedJavaMethod;

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

    @SuppressWarnings("try")
    protected static EconomicSet<Node> inline(Invoke invoke, ResolvedJavaMethod concrete, Inlineable inlineable, boolean receiverNullCheck, String reason)
    {
        assert inlineable instanceof InlineableGraph;
        StructuredGraph calleeGraph = ((InlineableGraph) inlineable).getGraph();
        return InliningUtil.inlineForCanonicalization(invoke, calleeGraph, receiverNullCheck, concrete, reason, "InliningPhase");
    }

    @Override
    @SuppressWarnings("try")
    public final void populateInlinableElements(HighTierContext context, StructuredGraph caller, CanonicalizerPhase canonicalizer, OptionValues options)
    {
        for (int i = 0; i < numberOfMethods(); i++)
        {
            Inlineable elem = Inlineable.getInlineableElement(methodAt(i), invoke, context, canonicalizer, caller.trackNodeSourcePosition());
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
