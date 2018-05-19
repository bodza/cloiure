package graalvm.compiler.hotspot.replacements;

import graalvm.util.UnsafeAccess;
import static graalvm.compiler.replacements.SnippetTemplate.DEFAULT_REPLACER;

import graalvm.compiler.api.replacements.Fold;
import graalvm.compiler.api.replacements.Snippet;
import graalvm.compiler.api.replacements.Snippet.ConstantParameter;
import graalvm.compiler.hotspot.meta.HotSpotProviders;
import graalvm.compiler.nodes.NamedLocationIdentity;
import graalvm.compiler.nodes.debug.StringToBytesNode;
import graalvm.compiler.nodes.java.NewArrayNode;
import graalvm.compiler.nodes.spi.LoweringTool;
import graalvm.compiler.options.OptionValues;
import graalvm.compiler.replacements.SnippetTemplate;
import graalvm.compiler.replacements.SnippetTemplate.AbstractTemplates;
import graalvm.compiler.replacements.SnippetTemplate.Arguments;
import graalvm.compiler.replacements.SnippetTemplate.SnippetInfo;
import graalvm.compiler.replacements.Snippets;
import graalvm.compiler.replacements.nodes.CStringConstant;
import graalvm.compiler.word.Word;
import org.graalvm.word.LocationIdentity;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.JavaKind;

/**
 * The {@code StringToBytesSnippets} contains a snippet for lowering {@link StringToBytesNode}.
 */
public class StringToBytesSnippets implements Snippets
{
    public static final LocationIdentity CSTRING_LOCATION = NamedLocationIdentity.immutable("CString location");

    @Fold
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

    public static class Templates extends AbstractTemplates
    {
        private final SnippetInfo create;

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
            template.instantiate(providers.getMetaAccess(), stringToBytesNode, DEFAULT_REPLACER, args);
        }
    }
}
