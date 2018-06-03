package giraaff.hotspot.replacements;

import java.lang.reflect.Modifier;

import giraaff.api.directives.GraalDirectives;
import giraaff.api.replacements.ClassSubstitution;
import giraaff.api.replacements.MethodSubstitution;
import giraaff.hotspot.HotSpotRuntime;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.hotspot.word.KlassPointer;

/**
 * Substitutions for {@link sun.reflect.Reflection} methods.
 */
@ClassSubstitution(className = { "jdk.internal.reflect.Reflection", "sun.reflect.Reflection" }, optional = true)
// @class ReflectionSubstitutions
public final class ReflectionSubstitutions
{
    @MethodSubstitution
    public static int getClassAccessFlags(Class<?> __aClass)
    {
        KlassPointer __klass = ClassGetHubNode.readClass(GraalDirectives.guardingNonNull(__aClass));
        if (__klass.isNull())
        {
            // Class for primitive type
            return Modifier.ABSTRACT | Modifier.FINAL | Modifier.PUBLIC;
        }
        else
        {
            return __klass.readInt(HotSpotRuntime.klassAccessFlagsOffset, HotSpotReplacementsUtil.KLASS_ACCESS_FLAGS_LOCATION) & HotSpotRuntime.jvmAccWrittenFlags;
        }
    }
}
