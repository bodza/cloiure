package graalvm.compiler.hotspot.replacements;

import jdk.vm.ci.code.TargetDescription;

import graalvm.compiler.api.replacements.Snippet;
import graalvm.compiler.hotspot.meta.HotSpotProviders;
import graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil;
import graalvm.compiler.nodes.extended.FixedValueAnchorNode;
import graalvm.compiler.nodes.extended.RawLoadNode;
import graalvm.compiler.nodes.memory.HeapAccess.BarrierType;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.replacements.SnippetTemplate;
import graalvm.compiler.replacements.SnippetTemplate.AbstractTemplates;
import graalvm.compiler.replacements.SnippetTemplate.Arguments;
import graalvm.compiler.replacements.SnippetTemplate.SnippetInfo;
import graalvm.compiler.replacements.Snippets;
import graalvm.compiler.word.Word;

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
