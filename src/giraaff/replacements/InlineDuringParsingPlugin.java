package giraaff.replacements;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import giraaff.core.common.GraalOptions;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.ValueNode;
import giraaff.nodes.graphbuilderconf.GraphBuilderContext;
import giraaff.nodes.graphbuilderconf.InlineInvokePlugin;
import giraaff.nodes.graphbuilderconf.InlineInvokePlugin.InlineInfo;

// @class InlineDuringParsingPlugin
public final class InlineDuringParsingPlugin implements InlineInvokePlugin
{
    ///
    // Budget which when exceeded reduces the effective value of
    // {@link GraalOptions#inlineDuringParsingMaxDepth} to {@link #maxDepthAfterBudgetExceeded}.
    ///
    // @def
    private static final int nodeBudget = 2000;
    // @def
    private static final int maxDepthAfterBudgetExceeded = 3;

    @Override
    public InlineInfo shouldInlineInvoke(GraphBuilderContext __b, ResolvedJavaMethod __method, ValueNode[] __args)
    {
        if (__method.hasBytecodes() && __method.getDeclaringClass().isLinked() && __method.canBeInlined())
        {
            // test force inlining first
            if (__method.shouldBeInlined())
            {
                return InlineInfo.createStandardInlineInfo(__method);
            }

            if (!__method.isSynchronized() && checkSize(__method, __args, __b.getGraph()) && checkInliningDepth(__b))
            {
                return InlineInfo.createStandardInlineInfo(__method);
            }
        }
        return null;
    }

    private static boolean checkInliningDepth(GraphBuilderContext __b)
    {
        int __nodeCount = __b.getGraph().getNodeCount();
        int __maxDepth = GraalOptions.inlineDuringParsingMaxDepth;
        if (__nodeCount > nodeBudget && maxDepthAfterBudgetExceeded < __maxDepth)
        {
            __maxDepth = maxDepthAfterBudgetExceeded;
        }
        return __b.getDepth() < __maxDepth;
    }

    private static boolean checkSize(ResolvedJavaMethod __method, ValueNode[] __args, StructuredGraph __graph)
    {
        int __bonus = 1;
        for (ValueNode __v : __args)
        {
            if (__v.isConstant())
            {
                __bonus++;
            }
        }
        return __method.getCode().length <= GraalOptions.trivialInliningSize * __bonus;
    }
}
