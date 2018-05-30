package giraaff.hotspot.replacements;

import java.lang.reflect.Modifier;

import giraaff.api.replacements.ClassSubstitution;
import giraaff.api.replacements.MethodSubstitution;
import giraaff.hotspot.HotSpotRuntime;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.hotspot.word.KlassPointer;
import giraaff.nodes.PiNode;
import giraaff.nodes.SnippetAnchorNode;

/**
 * Substitutions for {@link java.lang.Class} methods.
 */
@ClassSubstitution(Class.class)
// @class HotSpotClassSubstitutions
public final class HotSpotClassSubstitutions
{
    // @cons
    private HotSpotClassSubstitutions()
    {
        super();
    }

    @MethodSubstitution(isStatic = false)
    public static int getModifiers(final Class<?> thisObj)
    {
        KlassPointer klass = ClassGetHubNode.readClass(thisObj);
        if (klass.isNull())
        {
            // Class for primitive type
            return Modifier.ABSTRACT | Modifier.FINAL | Modifier.PUBLIC;
        }
        else
        {
            return klass.readInt(HotSpotRuntime.klassModifierFlagsOffset, HotSpotReplacementsUtil.KLASS_MODIFIER_FLAGS_LOCATION);
        }
    }

    @MethodSubstitution(isStatic = false)
    public static boolean isInterface(final Class<?> thisObj)
    {
        KlassPointer klass = ClassGetHubNode.readClass(thisObj);
        if (klass.isNull())
        {
            // Class for primitive type
            return false;
        }
        else
        {
            int accessFlags = klass.readInt(HotSpotRuntime.klassAccessFlagsOffset, HotSpotReplacementsUtil.KLASS_ACCESS_FLAGS_LOCATION);
            return (accessFlags & Modifier.INTERFACE) != 0;
        }
    }

    @MethodSubstitution(isStatic = false)
    public static boolean isArray(final Class<?> thisObj)
    {
        KlassPointer klass = ClassGetHubNode.readClass(thisObj);
        if (klass.isNull())
        {
            // Class for primitive type
            return false;
        }
        else
        {
            KlassPointer klassNonNull = ClassGetHubNode.piCastNonNull(klass, SnippetAnchorNode.anchor());
            return HotSpotReplacementsUtil.klassIsArray(klassNonNull);
        }
    }

    @MethodSubstitution(isStatic = false)
    public static boolean isPrimitive(final Class<?> thisObj)
    {
        KlassPointer klass = ClassGetHubNode.readClass(thisObj);
        return klass.isNull();
    }

    @MethodSubstitution(isStatic = false)
    public static Class<?> getSuperclass(final Class<?> thisObj)
    {
        KlassPointer klass = ClassGetHubNode.readClass(thisObj);
        if (!klass.isNull())
        {
            KlassPointer klassNonNull = ClassGetHubNode.piCastNonNull(klass, SnippetAnchorNode.anchor());
            int accessFlags = klassNonNull.readInt(HotSpotRuntime.klassAccessFlagsOffset, HotSpotReplacementsUtil.KLASS_ACCESS_FLAGS_LOCATION);
            if ((accessFlags & Modifier.INTERFACE) == 0)
            {
                if (HotSpotReplacementsUtil.klassIsArray(klassNonNull))
                {
                    return Object.class;
                }
                else
                {
                    KlassPointer superKlass = klassNonNull.readKlassPointer(HotSpotRuntime.klassSuperKlassOffset, HotSpotReplacementsUtil.KLASS_SUPER_KLASS_LOCATION);
                    if (superKlass.isNull())
                    {
                        return null;
                    }
                    else
                    {
                        KlassPointer superKlassNonNull = ClassGetHubNode.piCastNonNull(superKlass, SnippetAnchorNode.anchor());
                        return HubGetClassNode.readClass(superKlassNonNull);
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
    public static Class<?> getComponentType(final Class<?> thisObj)
    {
        KlassPointer klass = ClassGetHubNode.readClass(thisObj);
        if (!klass.isNull())
        {
            KlassPointer klassNonNull = ClassGetHubNode.piCastNonNull(klass, SnippetAnchorNode.anchor());
            if (HotSpotReplacementsUtil.klassIsArray(klassNonNull))
            {
                return PiNode.asNonNullClass(klassNonNull.readObject(HotSpotRuntime.arrayKlassComponentMirrorOffset, HotSpotReplacementsUtil.ARRAY_KLASS_COMPONENT_MIRROR));
            }
        }
        else
        {
            // Class for primitive type
        }
        return null;
    }
}
