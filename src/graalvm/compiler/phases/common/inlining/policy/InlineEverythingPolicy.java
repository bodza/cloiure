package graalvm.compiler.phases.common.inlining.policy;

import static graalvm.compiler.core.common.GraalOptions.MaximumDesiredSize;

import graalvm.compiler.core.common.GraalOptions;
import graalvm.compiler.core.common.PermanentBailoutException;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.spi.Replacements;
import graalvm.compiler.phases.common.inlining.InliningUtil;
import graalvm.compiler.phases.common.inlining.walker.MethodInvocation;

public class InlineEverythingPolicy implements InliningPolicy
{
    @Override
    public boolean continueInlining(StructuredGraph graph)
    {
        if (InliningUtil.getNodeCount(graph) >= MaximumDesiredSize.getValue(graph.getOptions()))
        {
            throw new PermanentBailoutException("Inline all calls failed. The resulting graph is too large.");
        }
        return true;
    }

    @Override
    public Decision isWorthInlining(Replacements replacements, MethodInvocation invocation, int inliningDepth, boolean fullyProcessed)
    {
        return Decision.YES.withReason(GraalOptions.TraceInlining.getValue(replacements.getOptions()), "inline everything");
    }
}
