package giraaff.hotspot.replacements;

import jdk.vm.ci.code.TargetDescription;

import giraaff.api.replacements.Snippet;
import giraaff.api.replacements.Snippet.ConstantParameter;
import giraaff.core.common.spi.ForeignCallDescriptor;
import giraaff.graph.Node.ConstantNodeParameter;
import giraaff.graph.Node.NodeIntrinsic;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.nodes.StubStartNode;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.extended.ForeignCallNode;
import giraaff.nodes.spi.LoweringTool;
import giraaff.options.OptionValues;
import giraaff.replacements.SnippetTemplate;
import giraaff.replacements.SnippetTemplate.AbstractTemplates;
import giraaff.replacements.SnippetTemplate.Arguments;
import giraaff.replacements.SnippetTemplate.SnippetInfo;
import giraaff.replacements.Snippets;
import giraaff.replacements.nodes.AssertionNode;
import giraaff.replacements.nodes.CStringConstant;
import giraaff.word.Word;

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
            vmMessageC(ASSERTION_VM_MESSAGE_C, true, CStringConstant.cstring(message), 0L, 0L, 0L);
        }
    }

    @Snippet
    public static void stubAssertion(boolean condition, @ConstantParameter String message)
    {
        if (!condition)
        {
            vmMessageC(ASSERTION_VM_MESSAGE_C, true, CStringConstant.cstring(message), 0L, 0L, 0L);
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

            template(assertionNode, args).instantiate(providers.getMetaAccess(), assertionNode, SnippetTemplate.DEFAULT_REPLACER, args);
        }
    }
}
