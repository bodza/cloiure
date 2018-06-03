package giraaff.hotspot.replacements;

import jdk.vm.ci.code.TargetDescription;

import org.graalvm.word.WordFactory;

import giraaff.api.replacements.Snippet;
import giraaff.hotspot.HotSpotRuntime;
import giraaff.hotspot.meta.HotSpotForeignCallsProviderImpl;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.nodes.StructuredGraph;
import giraaff.nodes.extended.BranchProbabilityNode;
import giraaff.nodes.spi.LoweringTool;
import giraaff.replacements.SnippetTemplate;
import giraaff.replacements.SnippetTemplate.AbstractTemplates;
import giraaff.replacements.SnippetTemplate.Arguments;
import giraaff.replacements.SnippetTemplate.SnippetInfo;
import giraaff.replacements.Snippets;
import giraaff.word.Word;

// @class HashCodeSnippets
public final class HashCodeSnippets implements Snippets
{
    // @cons
    private HashCodeSnippets()
    {
        super();
    }

    @Snippet
    public static int identityHashCodeSnippet(final Object __thisObj)
    {
        if (BranchProbabilityNode.probability(BranchProbabilityNode.NOT_FREQUENT_PROBABILITY, __thisObj == null))
        {
            return 0;
        }
        return computeHashCode(__thisObj);
    }

    static int computeHashCode(final Object __x)
    {
        Word __mark = HotSpotReplacementsUtil.loadWordFromObject(__x, HotSpotRuntime.markOffset);

        // this code is independent from biased locking (although it does not look that way)
        final Word __biasedLock = __mark.and(HotSpotRuntime.biasedLockMaskInPlace);
        if (BranchProbabilityNode.probability(BranchProbabilityNode.FAST_PATH_PROBABILITY, __biasedLock.equal(WordFactory.unsigned(HotSpotRuntime.unlockedMask))))
        {
            int __hash = (int) __mark.unsignedShiftRight(HotSpotRuntime.identityHashCodeShift).rawValue();
            if (BranchProbabilityNode.probability(BranchProbabilityNode.FAST_PATH_PROBABILITY, __hash != HotSpotRuntime.uninitializedIdentityHashCodeValue))
            {
                return __hash;
            }
        }
        return HotSpotReplacementsUtil.identityHashCode(HotSpotForeignCallsProviderImpl.IDENTITY_HASHCODE, __x);
    }

    // @class HashCodeSnippets.Templates
    public static final class Templates extends AbstractTemplates
    {
        // @field
        private final SnippetInfo ___identityHashCodeSnippet = snippet(HashCodeSnippets.class, "identityHashCodeSnippet", HotSpotReplacementsUtil.MARK_WORD_LOCATION);

        // @cons
        public Templates(HotSpotProviders __providers, TargetDescription __target)
        {
            super(__providers, __providers.getSnippetReflection(), __target);
        }

        public void lower(IdentityHashCodeNode __node, LoweringTool __tool)
        {
            StructuredGraph __graph = __node.graph();
            Arguments __args = new Arguments(this.___identityHashCodeSnippet, __graph.getGuardsStage(), __tool.getLoweringStage());
            __args.add("thisObj", __node.___object);
            SnippetTemplate __template = template(__node, __args);
            __template.instantiate(this.___providers.getMetaAccess(), __node, SnippetTemplate.DEFAULT_REPLACER, __args);
        }
    }
}
