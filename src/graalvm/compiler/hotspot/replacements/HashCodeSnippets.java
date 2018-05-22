package graalvm.compiler.hotspot.replacements;

import jdk.vm.ci.code.TargetDescription;

import org.graalvm.word.WordFactory;

import graalvm.compiler.api.replacements.Snippet;
import graalvm.compiler.hotspot.GraalHotSpotVMConfig;
import graalvm.compiler.hotspot.meta.HotSpotForeignCallsProviderImpl;
import graalvm.compiler.hotspot.meta.HotSpotProviders;
import graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.extended.BranchProbabilityNode;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.replacements.SnippetTemplate;
import graalvm.compiler.replacements.SnippetTemplate.AbstractTemplates;
import graalvm.compiler.replacements.SnippetTemplate.Arguments;
import graalvm.compiler.replacements.SnippetTemplate.SnippetInfo;
import graalvm.compiler.replacements.Snippets;
import graalvm.compiler.word.Word;

public class HashCodeSnippets implements Snippets
{
    @Snippet
    public static int identityHashCodeSnippet(final Object thisObj)
    {
        if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_FREQUENT_PROBABILITY, thisObj == null))
        {
            return 0;
        }
        return computeHashCode(thisObj);
    }

    static int computeHashCode(final Object x)
    {
        Word mark = HotSpotReplacementsUtil.loadWordFromObject(x, HotSpotReplacementsUtil.markOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG));

        // this code is independent from biased locking (although it does not look that way)
        final Word biasedLock = mark.and(HotSpotReplacementsUtil.biasedLockMaskInPlace(GraalHotSpotVMConfig.INJECTED_VMCONFIG));
        if (BranchProbabilityNode.probability(BranchProbabilityNode.FAST_PATH_PROBABILITY, biasedLock.equal(WordFactory.unsigned(HotSpotReplacementsUtil.unlockedMask(GraalHotSpotVMConfig.INJECTED_VMCONFIG)))))
        {
            int hash = (int) mark.unsignedShiftRight(HotSpotReplacementsUtil.identityHashCodeShift(GraalHotSpotVMConfig.INJECTED_VMCONFIG)).rawValue();
            if (BranchProbabilityNode.probability(BranchProbabilityNode.FAST_PATH_PROBABILITY, hash != HotSpotReplacementsUtil.uninitializedIdentityHashCodeValue(GraalHotSpotVMConfig.INJECTED_VMCONFIG)))
            {
                return hash;
            }
        }
        return HotSpotReplacementsUtil.identityHashCode(HotSpotForeignCallsProviderImpl.IDENTITY_HASHCODE, x);
    }

    public static class Templates extends AbstractTemplates
    {
        private final SnippetInfo identityHashCodeSnippet = snippet(HashCodeSnippets.class, "identityHashCodeSnippet", HotSpotReplacementsUtil.MARK_WORD_LOCATION);

        public Templates(OptionValues options, HotSpotProviders providers, TargetDescription target)
        {
            super(options, providers, providers.getSnippetReflection(), target);
        }

        public void lower(IdentityHashCodeNode node, LoweringTool tool)
        {
            StructuredGraph graph = node.graph();
            Arguments args = new Arguments(identityHashCodeSnippet, graph.getGuardsStage(), tool.getLoweringStage());
            args.add("thisObj", node.object);
            SnippetTemplate template = template(node, args);
            template.instantiate(providers.getMetaAccess(), node, SnippetTemplate.DEFAULT_REPLACER, args);
        }
    }
}
