package giraaff.replacements;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.core.common.GraalOptions;
import giraaff.java.BytecodeParserOptions;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.graphbuilderconf.GraphBuilderContext;
import giraaff.nodes.graphbuilderconf.InlineInvokePlugin;
import giraaff.nodes.graphbuilderconf.InlineInvokePlugin.InlineInfo;

public final class InlineDuringParsingPlugin implements InlineInvokePlugin
{
    /**
     * Budget which when exceeded reduces the effective value of
     * {@link BytecodeParserOptions#InlineDuringParsingMaxDepth} to
     * {@link #MaxDepthAfterBudgetExceeded}.
     */
    private static final int NodeBudget = Integer.getInteger("InlineDuringParsingPlugin.NodeBudget", 2000);

    private static final int MaxDepthAfterBudgetExceeded = Integer.getInteger("InlineDuringParsingPlugin.MaxDepthAfterBudgetExceeded", 3);

    @Override
    public InlineInfo shouldInlineInvoke(GraphBuilderContext b, ResolvedJavaMethod method, ValueNode[] args)
    {
        if (method.hasBytecodes() && method.getDeclaringClass().isLinked() && method.canBeInlined())
        {
            // test force inlining first
            if (method.shouldBeInlined())
            {
                return InlineInfo.createStandardInlineInfo(method);
            }

            if (!method.isSynchronized() && checkSize(method, args, b.getGraph()) && checkInliningDepth(b))
            {
                return InlineInfo.createStandardInlineInfo(method);
            }
        }
        return null;
    }

    private static boolean checkInliningDepth(GraphBuilderContext b)
    {
        int nodeCount = b.getGraph().getNodeCount();
        int maxDepth = BytecodeParserOptions.InlineDuringParsingMaxDepth.getValue(b.getOptions());
        if (nodeCount > NodeBudget && MaxDepthAfterBudgetExceeded < maxDepth)
        {
            maxDepth = MaxDepthAfterBudgetExceeded;
        }
        return b.getDepth() < maxDepth;
    }

    private static boolean checkSize(ResolvedJavaMethod method, ValueNode[] args, StructuredGraph graph)
    {
        int bonus = 1;
        for (ValueNode v : args)
        {
            if (v.isConstant())
            {
                bonus++;
            }
        }
        return method.getCode().length <= GraalOptions.TrivialInliningSize.getValue(graph.getOptions()) * bonus;
    }
}
