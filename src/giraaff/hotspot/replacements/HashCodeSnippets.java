package giraaff.hotspot.replacements;

import jdk.vm.ci.code.TargetDescription;

import org.graalvm.word.WordFactory;

import giraaff.api.replacements.Snippet;
import giraaff.hotspot.GraalHotSpotVMConfig;
import giraaff.hotspot.meta.HotSpotForeignCallsProviderImpl;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.extended.BranchProbabilityNode;
import giraaff.nodes.spi.LoweringTool;
import giraaff.options.OptionValues;
import giraaff.replacements.SnippetTemplate;
import giraaff.replacements.SnippetTemplate.AbstractTemplates;
import giraaff.replacements.SnippetTemplate.Arguments;
import giraaff.replacements.SnippetTemplate.SnippetInfo;
import giraaff.replacements.Snippets;
import giraaff.word.Word;

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
