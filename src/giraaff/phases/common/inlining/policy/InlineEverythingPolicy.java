package giraaff.phases.common.inlining.policy;

import giraaff.core.common.GraalOptions;
import giraaff.core.common.PermanentBailoutException;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.spi.Replacements;
import giraaff.phases.common.inlining.InliningUtil;
import giraaff.phases.common.inlining.walker.MethodInvocation;

public class InlineEverythingPolicy implements InliningPolicy
{
    @Override
    public boolean continueInlining(StructuredGraph graph)
    {
        if (InliningUtil.getNodeCount(graph) >= GraalOptions.MaximumDesiredSize.getValue(graph.getOptions()))
        {
            throw new PermanentBailoutException("Inline all calls failed. The resulting graph is too large.");
        }
        return true;
    }

    @Override
    public Decision isWorthInlining(Replacements replacements, MethodInvocation invocation, int inliningDepth, boolean fullyProcessed)
    {
        return Decision.YES;
    }
}
