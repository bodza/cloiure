package giraaff.replacements;

import java.util.EnumMap;

import jdk.vm.ci.code.TargetDescription;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;

import giraaff.api.replacements.Snippet;
import giraaff.api.replacements.SnippetReflectionProvider;
import giraaff.nodes.ConstantNode;
import giraaff.nodes.PiNode;
import giraaff.nodes.ValueNode;
import giraaff.nodes.calc.FloatingNode;
import giraaff.nodes.extended.BoxNode;
import giraaff.nodes.extended.UnboxNode;
import giraaff.nodes.spi.LoweringTool;
import giraaff.phases.util.Providers;
import giraaff.replacements.SnippetTemplate;

// @class BoxingSnippets
public final class BoxingSnippets implements Snippets
{
    @Snippet
    public static Object booleanValueOf(boolean __value)
    {
        return PiNode.piCastToSnippetReplaceeStamp(Boolean.valueOf(__value));
    }

    @Snippet
    public static Object byteValueOf(byte __value)
    {
        return PiNode.piCastToSnippetReplaceeStamp(Byte.valueOf(__value));
    }

    @Snippet
    public static Object charValueOf(char __value)
    {
        return PiNode.piCastToSnippetReplaceeStamp(Character.valueOf(__value));
    }

    @Snippet
    public static Object intValueOf(int __value)
    {
        return PiNode.piCastToSnippetReplaceeStamp(Integer.valueOf(__value));
    }

    @Snippet
    public static Object longValueOf(long __value)
    {
        return PiNode.piCastToSnippetReplaceeStamp(Long.valueOf(__value));
    }

    @Snippet
    public static Object shortValueOf(short __value)
    {
        return PiNode.piCastToSnippetReplaceeStamp(Short.valueOf(__value));
    }

    @Snippet
    public static boolean booleanValue(Boolean __value)
    {
        return __value.booleanValue();
    }

    @Snippet
    public static byte byteValue(Byte __value)
    {
        return __value.byteValue();
    }

    @Snippet
    public static char charValue(Character __value)
    {
        return __value.charValue();
    }

    @Snippet
    public static int intValue(Integer __value)
    {
        return __value.intValue();
    }

    @Snippet
    public static long longValue(Long __value)
    {
        return __value.longValue();
    }

    @Snippet
    public static short shortValue(Short __value)
    {
        return __value.shortValue();
    }

    public static FloatingNode canonicalizeBoxing(BoxNode __box, MetaAccessProvider __metaAccess, ConstantReflectionProvider __constantReflection)
    {
        ValueNode __value = __box.getValue();
        if (__value.isConstant())
        {
            JavaConstant __sourceConstant = __value.asJavaConstant();
            if (__sourceConstant.getJavaKind() != __box.getBoxingKind() && __sourceConstant.getJavaKind().isNumericInteger())
            {
                switch (__box.getBoxingKind())
                {
                    case Boolean:
                    {
                        __sourceConstant = JavaConstant.forBoolean(__sourceConstant.asLong() != 0L);
                        break;
                    }
                    case Byte:
                    {
                        __sourceConstant = JavaConstant.forByte((byte) __sourceConstant.asLong());
                        break;
                    }
                    case Char:
                    {
                        __sourceConstant = JavaConstant.forChar((char) __sourceConstant.asLong());
                        break;
                    }
                    case Short:
                    {
                        __sourceConstant = JavaConstant.forShort((short) __sourceConstant.asLong());
                        break;
                    }
                }
            }
            JavaConstant __boxedConstant = __constantReflection.boxPrimitive(__sourceConstant);
            if (__boxedConstant != null && __sourceConstant.getJavaKind() == __box.getBoxingKind())
            {
                return ConstantNode.forConstant(__boxedConstant, __metaAccess, __box.graph());
            }
        }
        return null;
    }

    // @class BoxingSnippets.BoxingTemplates
    public static final class BoxingTemplates extends SnippetTemplate.AbstractTemplates
    {
        // @field
        private final EnumMap<JavaKind, SnippetTemplate.SnippetInfo> ___boxSnippets = new EnumMap<>(JavaKind.class);
        // @field
        private final EnumMap<JavaKind, SnippetTemplate.SnippetInfo> ___unboxSnippets = new EnumMap<>(JavaKind.class);

        // @cons BoxingSnippets.BoxingTemplates
        public BoxingTemplates(Providers __providers, SnippetReflectionProvider __snippetReflection, TargetDescription __target)
        {
            super(__providers, __snippetReflection, __target);
            for (JavaKind __kind : new JavaKind[] { JavaKind.Boolean, JavaKind.Byte, JavaKind.Char, JavaKind.Int, JavaKind.Long, JavaKind.Short })
            {
                this.___boxSnippets.put(__kind, snippet(BoxingSnippets.class, __kind.getJavaName() + "ValueOf"));
                this.___unboxSnippets.put(__kind, snippet(BoxingSnippets.class, __kind.getJavaName() + "Value"));
            }
        }

        public void lower(BoxNode __box, LoweringTool __tool)
        {
            FloatingNode __canonical = canonicalizeBoxing(__box, this.___providers.getMetaAccess(), this.___providers.getConstantReflection());
            // if in AOT mode, we don't want to embed boxed constants.
            if (__canonical != null)
            {
                __box.graph().replaceFixedWithFloating(__box, __canonical);
            }
            else
            {
                SnippetTemplate.Arguments __args = new SnippetTemplate.Arguments(this.___boxSnippets.get(__box.getBoxingKind()), __box.graph().getGuardsStage(), __tool.getLoweringStage());
                __args.add("value", __box.getValue());

                SnippetTemplate __template = template(__box, __args);
                __template.instantiate(this.___providers.getMetaAccess(), __box, SnippetTemplate.DEFAULT_REPLACER, __args);
            }
        }

        public void lower(UnboxNode __unbox, LoweringTool __tool)
        {
            SnippetTemplate.Arguments __args = new SnippetTemplate.Arguments(this.___unboxSnippets.get(__unbox.getBoxingKind()), __unbox.graph().getGuardsStage(), __tool.getLoweringStage());
            __args.add("value", __unbox.getValue());

            SnippetTemplate __template = template(__unbox, __args);
            __template.instantiate(this.___providers.getMetaAccess(), __unbox, SnippetTemplate.DEFAULT_REPLACER, __args);
        }
    }
}
