package giraaff.phases.common.inlining.policy;

import jdk.vm.ci.code.BailoutException;

import giraaff.core.common.GraalOptions;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.spi.Replacements;
import giraaff.phases.common.inlining.InliningUtil;
import giraaff.phases.common.inlining.walker.MethodInvocation;

// @class InlineEverythingPolicy
public class InlineEverythingPolicy implements InliningPolicy
{
    @Override
    public boolean continueInlining(StructuredGraph graph)
    {
        if (InliningUtil.getNodeCount(graph) >= GraalOptions.maximumDesiredSize)
        {
            throw new BailoutException("Inline all calls failed. The resulting graph is too large.");
        }
        return true;
    }

    @Override
    public Decision isWorthInlining(Replacements replacements, MethodInvocation invocation, int inliningDepth, boolean fullyProcessed)
    {
        return Decision.YES;
    }
}
