package graalvm.compiler.hotspot.replacements;

import java.lang.reflect.Modifier;

import graalvm.compiler.api.directives.GraalDirectives;
import graalvm.compiler.api.replacements.ClassSubstitution;
import graalvm.compiler.api.replacements.MethodSubstitution;
import graalvm.compiler.hotspot.GraalHotSpotVMConfig;
import graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil;
import graalvm.compiler.hotspot.word.KlassPointer;

/**
 * Substitutions for {@link sun.reflect.Reflection} methods.
 */
@ClassSubstitution(className = {"jdk.internal.reflect.Reflection", "sun.reflect.Reflection"}, optional = true)
public class ReflectionSubstitutions
{
    @MethodSubstitution
    public static int getClassAccessFlags(Class<?> aClass)
    {
        KlassPointer klass = ClassGetHubNode.readClass(GraalDirectives.guardingNonNull(aClass));
        if (klass.isNull())
        {
            // Class for primitive type
            return Modifier.ABSTRACT | Modifier.FINAL | Modifier.PUBLIC;
        }
        else
        {
            return klass.readInt(HotSpotReplacementsUtil.klassAccessFlagsOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), HotSpotReplacementsUtil.KLASS_ACCESS_FLAGS_LOCATION) & HotSpotReplacementsUtil.jvmAccWrittenFlags(GraalHotSpotVMConfig.INJECTED_VMCONFIG);
        }
    }
}
