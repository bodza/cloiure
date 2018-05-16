package graalvm.compiler.hotspot.replacements;

import static graalvm.compiler.hotspot.GraalHotSpotVMConfig.INJECTED_VMCONFIG;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.CLASS_ARRAY_KLASS_LOCATION;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.arrayKlassOffset;
import static graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil.loadKlassFromObject;

import java.lang.reflect.Array;

import graalvm.compiler.api.directives.GraalDirectives;
import graalvm.compiler.api.replacements.ClassSubstitution;
import graalvm.compiler.api.replacements.MethodSubstitution;
import graalvm.compiler.nodes.java.DynamicNewArrayNode;

/**
 * Substitutions for {@link Array} methods.
 */
@ClassSubstitution(Array.class)
public class HotSpotArraySubstitutions
{
    @MethodSubstitution
    public static Object newInstance(Class<?> componentType, int length)
    {
        if (componentType == null || loadKlassFromObject(componentType, arrayKlassOffset(INJECTED_VMCONFIG), CLASS_ARRAY_KLASS_LOCATION).isNull())
        {
            // Exit the intrinsic here for the case where the array class does not exist
            return newInstance(componentType, length);
        }
        return DynamicNewArrayNode.newArray(GraalDirectives.guardingNonNull(componentType), length);
    }
}
