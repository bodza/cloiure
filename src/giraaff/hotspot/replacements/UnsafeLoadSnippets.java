package giraaff.hotspot.replacements;

import jdk.vm.ci.code.TargetDescription;

import giraaff.api.replacements.Snippet;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.nodes.extended.FixedValueAnchorNode;
import giraaff.nodes.extended.RawLoadNode;
import giraaff.nodes.memory.HeapAccess.BarrierType;
import giraaff.nodes.spi.LoweringTool;
import giraaff.replacements.SnippetTemplate;
import giraaff.replacements.SnippetTemplate.AbstractTemplates;
import giraaff.replacements.SnippetTemplate.Arguments;
import giraaff.replacements.SnippetTemplate.SnippetInfo;
import giraaff.replacements.Snippets;
import giraaff.word.Word;

// @class UnsafeLoadSnippets
public final class UnsafeLoadSnippets implements Snippets
{
    @Snippet
    public static Object lowerUnsafeLoad(Object __object, long __offset)
    {
        Object __fixedObject = FixedValueAnchorNode.getObject(__object);
        if (__object instanceof java.lang.ref.Reference && HotSpotReplacementsUtil.referentOffset() == __offset)
        {
            return Word.objectToTrackedPointer(__fixedObject).readObject((int) __offset, BarrierType.PRECISE);
        }
        else
        {
            return Word.objectToTrackedPointer(__fixedObject).readObject((int) __offset, BarrierType.NONE);
        }
    }

    // @class UnsafeLoadSnippets.Templates
    public static final class Templates extends AbstractTemplates
    {
        // @field
        private final SnippetInfo unsafeLoad = snippet(UnsafeLoadSnippets.class, "lowerUnsafeLoad");

        // @cons
        public Templates(HotSpotProviders __providers, TargetDescription __target)
        {
            super(__providers, __providers.getSnippetReflection(), __target);
        }

        public void lower(RawLoadNode __load, LoweringTool __tool)
        {
            Arguments __args = new Arguments(unsafeLoad, __load.graph().getGuardsStage(), __tool.getLoweringStage());
            __args.add("object", __load.object());
            __args.add("offset", __load.offset());
            template(__load, __args).instantiate(providers.getMetaAccess(), __load, SnippetTemplate.DEFAULT_REPLACER, __args);
        }
    }
}
