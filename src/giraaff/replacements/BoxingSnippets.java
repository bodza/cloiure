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
import giraaff.replacements.SnippetCounter.Group;
import giraaff.replacements.SnippetTemplate;
import giraaff.replacements.SnippetTemplate.AbstractTemplates;
import giraaff.replacements.SnippetTemplate.Arguments;
import giraaff.replacements.SnippetTemplate.SnippetInfo;

public class BoxingSnippets implements Snippets
{
    @Snippet
    public static Object booleanValueOf(boolean value, @ConstantParameter SnippetCounter valueOfCounter)
    {
        valueOfCounter.inc();
        return PiNode.piCastToSnippetReplaceeStamp(Boolean.valueOf(value));
    }

    @Snippet
    public static Object byteValueOf(byte value, @ConstantParameter SnippetCounter valueOfCounter)
    {
        valueOfCounter.inc();
        return PiNode.piCastToSnippetReplaceeStamp(Byte.valueOf(value));
    }

    @Snippet
    public static Object charValueOf(char value, @ConstantParameter SnippetCounter valueOfCounter)
    {
        valueOfCounter.inc();
        return PiNode.piCastToSnippetReplaceeStamp(Character.valueOf(value));
    }

    @Snippet
    public static Object doubleValueOf(double value, @ConstantParameter SnippetCounter valueOfCounter)
    {
        valueOfCounter.inc();
        return PiNode.piCastToSnippetReplaceeStamp(Double.valueOf(value));
    }

    @Snippet
    public static Object floatValueOf(float value, @ConstantParameter SnippetCounter valueOfCounter)
    {
        valueOfCounter.inc();
        return PiNode.piCastToSnippetReplaceeStamp(Float.valueOf(value));
    }

    @Snippet
    public static Object intValueOf(int value, @ConstantParameter SnippetCounter valueOfCounter)
    {
        valueOfCounter.inc();
        return PiNode.piCastToSnippetReplaceeStamp(Integer.valueOf(value));
    }

    @Snippet
    public static Object longValueOf(long value, @ConstantParameter SnippetCounter valueOfCounter)
    {
        valueOfCounter.inc();
        return PiNode.piCastToSnippetReplaceeStamp(Long.valueOf(value));
    }

    @Snippet
    public static Object shortValueOf(short value, @ConstantParameter SnippetCounter valueOfCounter)
    {
        valueOfCounter.inc();
        return PiNode.piCastToSnippetReplaceeStamp(Short.valueOf(value));
    }

    @Snippet
    public static boolean booleanValue(Boolean value, @ConstantParameter SnippetCounter valueCounter)
    {
        valueCounter.inc();
        return value.booleanValue();
    }

    @Snippet
    public static byte byteValue(Byte value, @ConstantParameter SnippetCounter valueCounter)
    {
        valueCounter.inc();
        return value.byteValue();
    }

    @Snippet
    public static char charValue(Character value, @ConstantParameter SnippetCounter valueCounter)
    {
        valueCounter.inc();
        return value.charValue();
    }

    @Snippet
    public static double doubleValue(Double value, @ConstantParameter SnippetCounter valueCounter)
    {
        valueCounter.inc();
        return value.doubleValue();
    }

    @Snippet
    public static float floatValue(Float value, @ConstantParameter SnippetCounter valueCounter)
    {
        valueCounter.inc();
        return value.floatValue();
    }

    @Snippet
    public static int intValue(Integer value, @ConstantParameter SnippetCounter valueCounter)
    {
        valueCounter.inc();
        return value.intValue();
    }

    @Snippet
    public static long longValue(Long value, @ConstantParameter SnippetCounter valueCounter)
    {
        valueCounter.inc();
        return value.longValue();
    }

    @Snippet
    public static short shortValue(Short value, @ConstantParameter SnippetCounter valueCounter)
    {
        valueCounter.inc();
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

        private final SnippetCounter valueOfCounter;
        private final SnippetCounter valueCounter;

        public Templates(OptionValues options, SnippetCounter.Group.Factory factory, Providers providers, SnippetReflectionProvider snippetReflection, TargetDescription target)
        {
            super(options, providers, snippetReflection, target);
            for (JavaKind kind : new JavaKind[]{JavaKind.Boolean, JavaKind.Byte, JavaKind.Char, JavaKind.Double, JavaKind.Float, JavaKind.Int, JavaKind.Long, JavaKind.Short})
            {
                boxSnippets.put(kind, snippet(BoxingSnippets.class, kind.getJavaName() + "ValueOf"));
                unboxSnippets.put(kind, snippet(BoxingSnippets.class, kind.getJavaName() + "Value"));
            }
            Group group = factory.createSnippetCounterGroup("Boxing");
            valueOfCounter = new SnippetCounter(group, "valueOf", "box intrinsification");
            valueCounter = new SnippetCounter(group, "<kind>Value", "unbox intrinsification");
        }

        public void lower(BoxNode box, LoweringTool tool)
        {
            FloatingNode canonical = canonicalizeBoxing(box, providers.getMetaAccess(), providers.getConstantReflection());
            // if in AOT mode, we don't want to embed boxed constants.
            if (canonical != null && !GraalOptions.ImmutableCode.getValue(box.getOptions()))
            {
                box.graph().replaceFixedWithFloating(box, canonical);
            }
            else
            {
                Arguments args = new Arguments(boxSnippets.get(box.getBoxingKind()), box.graph().getGuardsStage(), tool.getLoweringStage());
                args.add("value", box.getValue());
                args.addConst("valueOfCounter", valueOfCounter);

                SnippetTemplate template = template(box, args);
                template.instantiate(providers.getMetaAccess(), box, SnippetTemplate.DEFAULT_REPLACER, args);
            }
        }

        public void lower(UnboxNode unbox, LoweringTool tool)
        {
            Arguments args = new Arguments(unboxSnippets.get(unbox.getBoxingKind()), unbox.graph().getGuardsStage(), tool.getLoweringStage());
            args.add("value", unbox.getValue());
            args.addConst("valueCounter", valueCounter);

            SnippetTemplate template = template(unbox, args);
            template.instantiate(providers.getMetaAccess(), unbox, SnippetTemplate.DEFAULT_REPLACER, args);
        }
    }
}
