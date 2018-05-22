package giraaff.hotspot.replacements;

import jdk.vm.ci.code.TargetDescription;

import giraaff.api.replacements.Snippet;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.nodes.extended.FixedValueAnchorNode;
import giraaff.nodes.extended.RawLoadNode;
import giraaff.nodes.memory.HeapAccess.BarrierType;
import giraaff.nodes.spi.LoweringTool;
import giraaff.options.OptionValues;
import giraaff.replacements.SnippetTemplate;
import giraaff.replacements.SnippetTemplate.AbstractTemplates;
import giraaff.replacements.SnippetTemplate.Arguments;
import giraaff.replacements.SnippetTemplate.SnippetInfo;
import giraaff.replacements.Snippets;
import giraaff.word.Word;

public class UnsafeLoadSnippets implements Snippets
{
    @Snippet
    public static Object lowerUnsafeLoad(Object object, long offset)
    {
        Object fixedObject = FixedValueAnchorNode.getObject(object);
        if (object instanceof java.lang.ref.Reference && HotSpotReplacementsUtil.referentOffset() == offset)
        {
            return Word.objectToTrackedPointer(fixedObject).readObject((int) offset, BarrierType.PRECISE);
        }
        else
        {
            return Word.objectToTrackedPointer(fixedObject).readObject((int) offset, BarrierType.NONE);
        }
    }

    public static class Templates extends AbstractTemplates
    {
        private final SnippetInfo unsafeLoad = snippet(UnsafeLoadSnippets.class, "lowerUnsafeLoad");

        public Templates(OptionValues options, HotSpotProviders providers, TargetDescription target)
        {
            super(options, providers, providers.getSnippetReflection(), target);
        }

        public void lower(RawLoadNode load, LoweringTool tool)
        {
            Arguments args = new Arguments(unsafeLoad, load.graph().getGuardsStage(), tool.getLoweringStage());
            args.add("object", load.object());
            args.add("offset", load.offset());
            template(load, args).instantiate(providers.getMetaAccess(), load, SnippetTemplate.DEFAULT_REPLACER, args);
        }
    }
}
