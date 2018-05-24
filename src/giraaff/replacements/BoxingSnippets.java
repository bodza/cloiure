package giraaff.replacements;

import java.util.EnumMap;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;

import giraaff.api.replacements.Snippet;
import giraaff.api.replacements.Snippet.ConstantParameter;
import giraaff.api.replacements.SnippetReflectionProvider;
import giraaff.core.common.GraalOptions;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.PiNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.extended.BoxNode;
import giraaff.nodes.extended.UnboxNode;
import giraaff.nodes.spi.LoweringTool;
import giraaff.options.OptionValues;
import giraaff.phases.util.Providers;
import giraaff.replacements.SnippetTemplate;
import giraaff.replacements.SnippetTemplate.AbstractTemplates;
import giraaff.replacements.SnippetTemplate.Arguments;
import giraaff.replacements.SnippetTemplate.SnippetInfo;

public class BoxingSnippets implements Snippets
{
    @Snippet
    public static Object booleanValueOf(boolean value)
    {
        return PiNode.piCastToSnippetReplaceeStamp(Boolean.valueOf(value));
    }

    @Snippet
    public static Object byteValueOf(byte value)
    {
        return PiNode.piCastToSnippetReplaceeStamp(Byte.valueOf(value));
    }

    @Snippet
    public static Object charValueOf(char value)
    {
        return PiNode.piCastToSnippetReplaceeStamp(Character.valueOf(value));
    }

    @Snippet
    public static Object doubleValueOf(double value)
    {
        return PiNode.piCastToSnippetReplaceeStamp(Double.valueOf(value));
    }

    @Snippet
    public static Object floatValueOf(float value)
    {
        return PiNode.piCastToSnippetReplaceeStamp(Float.valueOf(value));
    }

    @Snippet
    public static Object intValueOf(int value)
    {
        return PiNode.piCastToSnippetReplaceeStamp(Integer.valueOf(value));
    }

    @Snippet
    public static Object longValueOf(long value)
    {
        return PiNode.piCastToSnippetReplaceeStamp(Long.valueOf(value));
    }

    @Snippet
    public static Object shortValueOf(short value)
    {
        return PiNode.piCastToSnippetReplaceeStamp(Short.valueOf(value));
    }

    @Snippet
    public static boolean booleanValue(Boolean value)
    {
        return value.booleanValue();
    }

    @Snippet
    public static byte byteValue(Byte value)
    {
        return value.byteValue();
    }

    @Snippet
    public static char charValue(Character value)
    {
        return value.charValue();
    }

    @Snippet
    public static double doubleValue(Double value)
    {
        return value.doubleValue();
    }

    @Snippet
    public static float floatValue(Float value)
    {
        return value.floatValue();
    }

    @Snippet
    public static int intValue(Integer value)
    {
        return value.intValue();
    }

    @Snippet
    public static long longValue(Long value)
    {
        return value.longValue();
    }

    @Snippet
    public static short shortValue(Short value)
    {
        return value.shortValue();
    }

    public static FloatingNode canonicalizeBoxing(BoxNode box, MetaAccessProvider metaAccess, ConstantReflectionProvider constantReflection)
    {
        ValueNode value = box.getValue();
        if (value.isConstant())
        {
            JavaConstant sourceConstant = value.asJavaConstant();
            if (sourceConstant.getJavaKind() != box.getBoxingKind() && sourceConstant.getJavaKind().isNumericInteger())
            {
                switch (box.getBoxingKind())
                {
                    case Boolean:
                        sourceConstant = JavaConstant.forBoolean(sourceConstant.asLong() != 0L);
                        break;
                    case Byte:
                        sourceConstant = JavaConstant.forByte((byte) sourceConstant.asLong());
                        break;
                    case Char:
                        sourceConstant = JavaConstant.forChar((char) sourceConstant.asLong());
                        break;
                    case Short:
                        sourceConstant = JavaConstant.forShort((short) sourceConstant.asLong());
                        break;
                }
            }
            JavaConstant boxedConstant = constantReflection.boxPrimitive(sourceConstant);
            if (boxedConstant != null && sourceConstant.getJavaKind() == box.getBoxingKind())
            {
                return ConstantNode.forConstant(boxedConstant, metaAccess, box.graph());
            }
        }
        return null;
    }

    public static class Templates extends AbstractTemplates
    {
        private final EnumMap<JavaKind, SnippetInfo> boxSnippets = new EnumMap<>(JavaKind.class);
        private final EnumMap<JavaKind, SnippetInfo> unboxSnippets = new EnumMap<>(JavaKind.class);

        public Templates(OptionValues options, Providers providers, SnippetReflectionProvider snippetReflection, TargetDescription target)
        {
            super(options, providers, snippetReflection, target);
            for (JavaKind kind : new JavaKind[] { JavaKind.Boolean, JavaKind.Byte, JavaKind.Char, JavaKind.Double, JavaKind.Float, JavaKind.Int, JavaKind.Long, JavaKind.Short })
            {
                boxSnippets.put(kind, snippet(BoxingSnippets.class, kind.getJavaName() + "ValueOf"));
                unboxSnippets.put(kind, snippet(BoxingSnippets.class, kind.getJavaName() + "Value"));
            }
        }

        public void lower(BoxNode box, LoweringTool tool)
        {
            FloatingNode canonical = canonicalizeBoxing(box, providers.getMetaAccess(), providers.getConstantReflection());
            // if in AOT mode, we don't want to embed boxed constants.
            if (canonical != null)
            {
                box.graph().replaceFixedWithFloating(box, canonical);
            }
            else
            {
                Arguments args = new Arguments(boxSnippets.get(box.getBoxingKind()), box.graph().getGuardsStage(), tool.getLoweringStage());
                args.add("value", box.getValue());

                SnippetTemplate template = template(box, args);
                template.instantiate(providers.getMetaAccess(), box, SnippetTemplate.DEFAULT_REPLACER, args);
            }
        }

        public void lower(UnboxNode unbox, LoweringTool tool)
        {
            Arguments args = new Arguments(unboxSnippets.get(unbox.getBoxingKind()), unbox.graph().getGuardsStage(), tool.getLoweringStage());
            args.add("value", unbox.getValue());

            SnippetTemplate template = template(unbox, args);
            template.instantiate(providers.getMetaAccess(), unbox, SnippetTemplate.DEFAULT_REPLACER, args);
        }
    }
}
