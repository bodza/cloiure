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
import giraaff.replacements.SnippetTemplate;
import giraaff.replacements.Snippets;
import giraaff.replacements.nodes.CStringConstant;
import giraaff.word.Word;
import giraaff.util.UnsafeAccess;

///
// The {@code StringToBytesSnippets} contains a snippet for lowering {@link StringToBytesNode}.
///
// @class StringToBytesSnippets
public final class StringToBytesSnippets implements Snippets
{
    // @def
    public static final LocationIdentity CSTRING_LOCATION = NamedLocationIdentity.immutable("CString location");

    // @Fold
    static long arrayBaseOffset()
    {
        return UnsafeAccess.UNSAFE.arrayBaseOffset(char[].class);
    }

    @Snippet
    public static byte[] transform(@Snippet.ConstantParameter String __compilationTimeString)
    {
        int __i = __compilationTimeString.length();
        byte[] __array = (byte[]) NewArrayNode.newUninitializedArray(byte.class, __i);
        Word __cArray = CStringConstant.cstring(__compilationTimeString);
        while (__i-- > 0)
        {
            // array[i] = cArray.readByte(i);
            UnsafeAccess.UNSAFE.putByte(__array, arrayBaseOffset() + __i, __cArray.readByte(__i, CSTRING_LOCATION));
        }
        return __array;
    }

    // @class StringToBytesSnippets.StringToBytesTemplates
    public static final class StringToBytesTemplates extends SnippetTemplate.AbstractTemplates
    {
        // @field
        private final SnippetTemplate.SnippetInfo ___create;

        // @cons StringToBytesSnippets.StringToBytesTemplates
        public StringToBytesTemplates(HotSpotProviders __providers, TargetDescription __target)
        {
            super(__providers, __providers.getSnippetReflection(), __target);
            this.___create = snippet(StringToBytesSnippets.class, "transform", NamedLocationIdentity.getArrayLocation(JavaKind.Byte));
        }

        public void lower(StringToBytesNode __stringToBytesNode, LoweringTool __tool)
        {
            SnippetTemplate.Arguments __args = new SnippetTemplate.Arguments(this.___create, __stringToBytesNode.graph().getGuardsStage(), __tool.getLoweringStage());
            __args.addConst("compilationTimeString", __stringToBytesNode.getValue());
            SnippetTemplate __template = template(__stringToBytesNode, __args);
            __template.instantiate(this.___providers.getMetaAccess(), __stringToBytesNode, SnippetTemplate.DEFAULT_REPLACER, __args);
        }
    }
}
