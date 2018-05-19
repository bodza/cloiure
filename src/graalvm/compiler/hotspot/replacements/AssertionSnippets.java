package graalvm.compiler.hotspot.replacements;

import static graalvm.compiler.replacements.SnippetTemplate.DEFAULT_REPLACER;
import static graalvm.compiler.replacements.nodes.CStringConstant.cstring;

import graalvm.compiler.api.replacements.Snippet;
import graalvm.compiler.api.replacements.Snippet.ConstantParameter;
import graalvm.compiler.core.common.spi.ForeignCallDescriptor;
import graalvm.compiler.graph.Node.ConstantNodeParameter;
import graalvm.compiler.graph.Node.NodeIntrinsic;
import graalvm.compiler.hotspot.meta.HotSpotProviders;
import graalvm.compiler.hotspot.nodes.StubStartNode;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.extended.ForeignCallNode;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.replacements.SnippetTemplate.AbstractTemplates;
import graalvm.compiler.replacements.SnippetTemplate.Arguments;
import graalvm.compiler.replacements.SnippetTemplate.SnippetInfo;
import graalvm.compiler.replacements.Snippets;
import graalvm.compiler.replacements.nodes.AssertionNode;
import graalvm.compiler.word.Word;

import jdk.vm.ci.code.TargetDescription;

public class AssertionSnippets implements Snippets
{
    /**
     * This call can only be used with true for the "vmError" parameter, so that it can be
     * configured to be a leaf method.
     */
    public static final ForeignCallDescriptor ASSERTION_VM_MESSAGE_C = new ForeignCallDescriptor("assertionVmMessageC", void.class, boolean.class, Word.class, long.class, long.class, long.class);

    @Snippet
    public static void assertion(boolean condition, @ConstantParameter String message)
    {
        if (!condition)
        {
            vmMessageC(ASSERTION_VM_MESSAGE_C, true, cstring(message), 0L, 0L, 0L);
        }
    }

    @Snippet
    public static void stubAssertion(boolean condition, @ConstantParameter String message)
    {
        if (!condition)
        {
            vmMessageC(ASSERTION_VM_MESSAGE_C, true, cstring(message), 0L, 0L, 0L);
        }
    }

    @NodeIntrinsic(ForeignCallNode.class)
    static native void vmMessageC(@ConstantNodeParameter ForeignCallDescriptor stubPrintfC, boolean vmError, Word format, long v1, long v2, long v3);

    public static class Templates extends AbstractTemplates
    {
        private final SnippetInfo assertion = snippet(AssertionSnippets.class, "assertion");
        private final SnippetInfo stubAssertion = snippet(AssertionSnippets.class, "stubAssertion");

        public Templates(OptionValues options, HotSpotProviders providers, TargetDescription target)
        {
            super(options, providers, providers.getSnippetReflection(), target);
        }

        public void lower(AssertionNode assertionNode, LoweringTool tool)
        {
            StructuredGraph graph = assertionNode.graph();
            Arguments args = new Arguments(graph.start() instanceof StubStartNode ? stubAssertion : assertion, graph.getGuardsStage(), tool.getLoweringStage());
            args.add("condition", assertionNode.condition());
            args.addConst("message", "failed runtime assertion in snippet/stub: " + assertionNode.message() + " (" + graph.method() + ")");

            template(assertionNode, args).instantiate(providers.getMetaAccess(), assertionNode, DEFAULT_REPLACER, args);
        }
    }
}
