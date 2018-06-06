package giraaff.hotspot.replacements;

import java.lang.reflect.Modifier;

import giraaff.api.replacements.ClassSubstitution;
import giraaff.api.replacements.MethodSubstitution;
import giraaff.hotspot.HotSpotRuntime;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.hotspot.word.KlassPointer;
import giraaff.nodes.PiNode;
import giraaff.nodes.SnippetAnchorNode;

///
// Substitutions for {@link java.lang.Class} methods.
///
@ClassSubstitution(Class.class)
// @class HotSpotClassSubstitutions
public final class HotSpotClassSubstitutions
{
    // @cons HotSpotClassSubstitutions
    private HotSpotClassSubstitutions()
    {
        super();
    }

    @MethodSubstitution(isStatic = false)
    public static int getModifiers(final Class<?> __thisObj)
    {
        KlassPointer __klass = ClassGetHubNode.readClass(__thisObj);
        if (__klass.isNull())
        {
            // Class for primitive type
            return Modifier.ABSTRACT | Modifier.FINAL | Modifier.PUBLIC;
        }
        else
        {
            return __klass.readInt(HotSpotRuntime.klassModifierFlagsOffset, HotSpotReplacementsUtil.KLASS_MODIFIER_FLAGS_LOCATION);
        }
    }

    @MethodSubstitution(isStatic = false)
    public static boolean isInterface(final Class<?> __thisObj)
    {
        KlassPointer __klass = ClassGetHubNode.readClass(__thisObj);
        if (__klass.isNull())
        {
            // Class for primitive type
            return false;
        }
        else
        {
            int __accessFlags = __klass.readInt(HotSpotRuntime.klassAccessFlagsOffset, HotSpotReplacementsUtil.KLASS_ACCESS_FLAGS_LOCATION);
            return (__accessFlags & Modifier.INTERFACE) != 0;
        }
    }

    @MethodSubstitution(isStatic = false)
    public static boolean isArray(final Class<?> __thisObj)
    {
        KlassPointer __klass = ClassGetHubNode.readClass(__thisObj);
        if (__klass.isNull())
        {
            // Class for primitive type
            return false;
        }
        else
        {
            KlassPointer __klassNonNull = ClassGetHubNode.piCastNonNull(__klass, SnippetAnchorNode.anchor());
            return HotSpotReplacementsUtil.klassIsArray(__klassNonNull);
        }
    }

    @MethodSubstitution(isStatic = false)
    public static boolean isPrimitive(final Class<?> __thisObj)
    {
        KlassPointer __klass = ClassGetHubNode.readClass(__thisObj);
        return __klass.isNull();
    }

    @MethodSubstitution(isStatic = false)
    public static Class<?> getSuperclass(final Class<?> __thisObj)
    {
        KlassPointer __klass = ClassGetHubNode.readClass(__thisObj);
        if (!__klass.isNull())
        {
            KlassPointer __klassNonNull = ClassGetHubNode.piCastNonNull(__klass, SnippetAnchorNode.anchor());
            int __accessFlags = __klassNonNull.readInt(HotSpotRuntime.klassAccessFlagsOffset, HotSpotReplacementsUtil.KLASS_ACCESS_FLAGS_LOCATION);
            if ((__accessFlags & Modifier.INTERFACE) == 0)
            {
                if (HotSpotReplacementsUtil.klassIsArray(__klassNonNull))
                {
                    return Object.class;
                }
                else
                {
                    KlassPointer __superKlass = __klassNonNull.readKlassPointer(HotSpotRuntime.klassSuperKlassOffset, HotSpotReplacementsUtil.KLASS_SUPER_KLASS_LOCATION);
                    if (__superKlass.isNull())
                    {
                        return null;
                    }
                    else
                    {
                        KlassPointer __superKlassNonNull = ClassGetHubNode.piCastNonNull(__superKlass, SnippetAnchorNode.anchor());
                        return HubGetClassNode.readClass(__superKlassNonNull);
                    }
                }
            }
        }
        else
        {
            // Class for primitive type
        }
        return null;
    }

    @MethodSubstitution(isStatic = false)
    public static Class<?> getComponentType(final Class<?> __thisObj)
    {
        KlassPointer __klass = ClassGetHubNode.readClass(__thisObj);
        if (!__klass.isNull())
        {
            KlassPointer __klassNonNull = ClassGetHubNode.piCastNonNull(__klass, SnippetAnchorNode.anchor());
            if (HotSpotReplacementsUtil.klassIsArray(__klassNonNull))
            {
                return PiNode.asNonNullClass(__klassNonNull.readObject(HotSpotRuntime.arrayKlassComponentMirrorOffset, HotSpotReplacementsUtil.ARRAY_KLASS_COMPONENT_MIRROR));
            }
        }
        else
        {
            // Class for primitive type
        }
        return null;
    }
}
