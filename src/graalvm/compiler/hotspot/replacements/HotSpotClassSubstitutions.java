package graalvm.compiler.hotspot.replacements;

import static graalvm.compiler.hotspot.GraalHotSpotVMConfig.INJECTED_VMCONFIG;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.ARRAY_KLASS_COMPONENT_MIRROR;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.KLASS_ACCESS_FLAGS_LOCATION;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.KLASS_MODIFIER_FLAGS_LOCATION;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.KLASS_SUPER_KLASS_LOCATION;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.arrayKlassComponentMirrorOffset;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.klassAccessFlagsOffset;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.klassIsArray;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.klassModifierFlagsOffset;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.klassSuperKlassOffset;

import java.lang.reflect.Modifier;

import graalvm.compiler.api.replacements.ClassSubstitution;
import graalvm.compiler.api.replacements.MethodSubstitution;
import graalvm.compiler.hotspot.word.KlassPointer;
import graalvm.compiler.nodes.PiNode;
import graalvm.compiler.nodes.SnippetAnchorNode;

/**
 * Substitutions for {@link java.lang.Class} methods.
 */
@ClassSubstitution(Class.class)
public class HotSpotClassSubstitutions
{
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
            return klass.readInt(klassModifierFlagsOffset(INJECTED_VMCONFIG), KLASS_MODIFIER_FLAGS_LOCATION);
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
            int accessFlags = klass.readInt(klassAccessFlagsOffset(INJECTED_VMCONFIG), KLASS_ACCESS_FLAGS_LOCATION);
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
            return klassIsArray(klassNonNull);
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
            int accessFlags = klassNonNull.readInt(klassAccessFlagsOffset(INJECTED_VMCONFIG), KLASS_ACCESS_FLAGS_LOCATION);
            if ((accessFlags & Modifier.INTERFACE) == 0)
            {
                if (klassIsArray(klassNonNull))
                {
                    return Object.class;
                }
                else
                {
                    KlassPointer superKlass = klassNonNull.readKlassPointer(klassSuperKlassOffset(INJECTED_VMCONFIG), KLASS_SUPER_KLASS_LOCATION);
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
            if (klassIsArray(klassNonNull))
            {
                return PiNode.asNonNullClass(klassNonNull.readObject(arrayKlassComponentMirrorOffset(INJECTED_VMCONFIG), ARRAY_KLASS_COMPONENT_MIRROR));
            }
        }
        else
        {
            // Class for primitive type
        }
        return null;
    }
}
