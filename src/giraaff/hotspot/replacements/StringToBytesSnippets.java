package giraaff.hotspot.replacements;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.JavaKind;

import org.graalvm.word.LocationIdentity;

import giraaff.api.replacements.Snippet;
import giraaff.api.replacements.Snippet.ConstantParameter;
import giraaff.hotspot.meta.HotSpotProviders;
import giraaff.nodes.NamedLocationIdentity;
import giraaff.nodes.debug.StringToBytesNode;
import giraaff.nodes.java.NewArrayNode;
import giraaff.nodes.spi.LoweringTool;
import giraaff.options.OptionValues;
import giraaff.replacements.SnippetTemplate;
import giraaff.replacements.SnippetTemplate.AbstractTemplates;
import giraaff.replacements.SnippetTemplate.Arguments;
import giraaff.replacements.SnippetTemplate.SnippetInfo;
import giraaff.replacements.Snippets;
import giraaff.replacements.nodes.CStringConstant;
import giraaff.word.Word;
import giraaff.util.UnsafeAccess;

/**
 * The {@code StringToBytesSnippets} contains a snippet for lowering {@link StringToBytesNode}.
 */
// @class StringToBytesSnippets
public final class StringToBytesSnippets implements Snippets
{
    public static final LocationIdentity CSTRING_LOCATION = NamedLocationIdentity.immutable("CString location");

    // @Fold
    static long arrayBaseOffset()
    {
        return UnsafeAccess.UNSAFE.arrayBaseOffset(char[].class);
    }

    @Snippet
    public static byte[] transform(@ConstantParameter String compilationTimeString)
    {
        int i = compilationTimeString.length();
        byte[] array = (byte[]) NewArrayNode.newUninitializedArray(byte.class, i);
        Word cArray = CStringConstant.cstring(compilationTimeString);
        while (i-- > 0)
        {
            // array[i] = cArray.readByte(i);
            UnsafeAccess.UNSAFE.putByte(array, arrayBaseOffset() + i, cArray.readByte(i, CSTRING_LOCATION));
        }
        return array;
    }

    // @class StringToBytesSnippets.Templates
    public static final class Templates extends AbstractTemplates
    {
        private final SnippetInfo create;

        // @cons
        public Templates(OptionValues options, HotSpotProviders providers, TargetDescription target)
        {
            super(options, providers, providers.getSnippetReflection(), target);
            create = snippet(StringToBytesSnippets.class, "transform", NamedLocationIdentity.getArrayLocation(JavaKind.Byte));
        }

        public void lower(StringToBytesNode stringToBytesNode, LoweringTool tool)
        {
            Arguments args = new Arguments(create, stringToBytesNode.graph().getGuardsStage(), tool.getLoweringStage());
            args.addConst("compilationTimeString", stringToBytesNode.getValue());
            SnippetTemplate template = template(stringToBytesNode, args);
            template.instantiate(providers.getMetaAccess(), stringToBytesNode, SnippetTemplate.DEFAULT_REPLACER, args);
        }
    }
}
