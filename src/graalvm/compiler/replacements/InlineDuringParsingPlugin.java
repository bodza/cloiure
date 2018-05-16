package graalvm.compiler.replacements;

import static graalvm.compiler.core.common.GraalOptions.TrivialInliningSize;
import static graalvm.compiler.java.BytecodeParserOptions.InlineDuringParsingMaxDepth;
import static graalvm.compiler.nodes.graphbuilderconf.InlineInvokePlugin.InlineInfo.createStandardInlineInfo;

import graalvm.compiler.java.BytecodeParserOptions;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.ValueNode;
import graalvm.compiler.nodes.graphbuilderconf.GraphBuilderContext;
import graalvm.compiler.nodes.graphbuilderconf.InlineInvokePlugin;

import jdk.vm.ci.meta.ResolvedJavaMethod;

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
            // Test force inlining first
            if (method.shouldBeInlined())
            {
                return createStandardInlineInfo(method);
            }

            if (!method.isSynchronized() && checkSize(method, args, b.getGraph()) && checkInliningDepth(b))
            {
                return createStandardInlineInfo(method);
            }
        }
        return null;
    }

    private static boolean checkInliningDepth(GraphBuilderContext b)
    {
        int nodeCount = b.getGraph().getNodeCount();
        int maxDepth = InlineDuringParsingMaxDepth.getValue(b.getOptions());
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
        return method.getCode().length <= TrivialInliningSize.getValue(graph.getOptions()) * bonus;
    }
}
