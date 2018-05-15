package graalvm.compiler.hotspot.replacements;

import static graalvm.compiler.hotspot.GraalHotSpotVMConfig.INJECTED_VMCONFIG;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.KLASS_ACCESS_FLAGS_LOCATION;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.jvmAccWrittenFlags;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.klassAccessFlagsOffset;

import java.lang.reflect.Modifier;

import graalvm.compiler.api.directives.GraalDirectives;
import graalvm.compiler.api.replacements.ClassSubstitution;
import graalvm.compiler.api.replacements.MethodSubstitution;
import graalvm.compiler.hotspot.word.KlassPointer;

/**
 * Substitutions for {@link sun.reflect.Reflection} methods.
 */
@ClassSubstitution(className = {"jdk.internal.reflect.Reflection", "sun.reflect.Reflection"}, optional = true)
public class ReflectionSubstitutions {

    @MethodSubstitution
    public static int getClassAccessFlags(Class<?> aClass) {
        KlassPointer klass = ClassGetHubNode.readClass(GraalDirectives.guardingNonNull(aClass));
        if (klass.isNull()) {
            // Class for primitive type
            return Modifier.ABSTRACT | Modifier.FINAL | Modifier.PUBLIC;
        } else {
            return klass.readInt(klassAccessFlagsOffset(INJECTED_VMCONFIG), KLASS_ACCESS_FLAGS_LOCATION) & jvmAccWrittenFlags(INJECTED_VMCONFIG);
        }
    }
}
