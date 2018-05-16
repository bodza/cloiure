package graalvm.compiler.hotspot.replacements;

import static graalvm.compiler.hotspot.GraalHotSpotVMConfig.INJECTED_VMCONFIG;
import static graalvm.compiler.hotspot.meta.HotSpotForeignCallsProviderImpl.IDENTITY_HASHCODE;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.biasedLockMaskInPlace;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.identityHashCode;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.identityHashCodeShift;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.loadWordFromObject;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.markOffset;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.uninitializedIdentityHashCodeValue;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.unlockedMask;
import static graalvm.compiler.nodes.extended.BranchProbabilityNode.FAST_PATH_PROBABILITY;
import static graalvm.compiler.nodes.extended.BranchProbabilityNode.NOT_FREQUENT_PROBABILITY;
import static graalvm.compiler.nodes.extended.BranchProbabilityNode.probability;

import graalvm.compiler.api.replacements.Snippet;
import graalvm.compiler.debug.DebugHandlersFactory;
import graalvm.compiler.hotspot.meta.HotSpotProviders;
import graalvm.compiler.nodes.StructuredGraph;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.replacements.SnippetTemplate;
import graalvm.compiler.replacements.SnippetTemplate.AbstractTemplates;
import graalvm.compiler.replacements.SnippetTemplate.Arguments;
import graalvm.compiler.replacements.SnippetTemplate.SnippetInfo;
import graalvm.compiler.replacements.Snippets;
import graalvm.compiler.word.Word;
import org.graalvm.word.WordFactory;

import jdk.vm.ci.code.TargetDescription;

public class HashCodeSnippets implements Snippets
{
    @Snippet
    public static int identityHashCodeSnippet(final Object thisObj)
    {
        if (probability(NOT_FREQUENT_PROBABILITY, thisObj == null))
        {
            return 0;
        }
        return computeHashCode(thisObj);
    }

    static int computeHashCode(final Object x)
    {
        Word mark = loadWordFromObject(x, markOffset(INJECTED_VMCONFIG));

        // this code is independent from biased locking (although it does not look that way)
        final Word biasedLock = mark.and(biasedLockMaskInPlace(INJECTED_VMCONFIG));
        if (probability(FAST_PATH_PROBABILITY, biasedLock.equal(WordFactory.unsigned(unlockedMask(INJECTED_VMCONFIG)))))
        {
            int hash = (int) mark.unsignedShiftRight(identityHashCodeShift(INJECTED_VMCONFIG)).rawValue();
            if (probability(FAST_PATH_PROBABILITY, hash != uninitializedIdentityHashCodeValue(INJECTED_VMCONFIG)))
            {
                return hash;
            }
        }
        return identityHashCode(IDENTITY_HASHCODE, x);
    }

    public static class Templates extends AbstractTemplates
    {
        private final SnippetInfo identityHashCodeSnippet = snippet(HashCodeSnippets.class, "identityHashCodeSnippet", HotSpotReplacementsUtil.MARK_WORD_LOCATION);

        public Templates(OptionValues options, Iterable<DebugHandlersFactory> factories, HotSpotProviders providers, TargetDescription target)
        {
            super(options, factories, providers, providers.getSnippetReflection(), target);
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
