package graalvm.compiler.hotspot.replacements;

import java.lang.reflect.Array;

import graalvm.compiler.api.directives.GraalDirectives;
import graalvm.compiler.api.replacements.ClassSubstitution;
import graalvm.compiler.api.replacements.MethodSubstitution;
import graalvm.compiler.hotspot.GraalHotSpotVMConfig;
import graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil;
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
        if (componentType == null || HotSpotReplacementsUtil.loadKlassFromObject(componentType, HotSpotReplacementsUtil.arrayKlassOffset(GraalHotSpotVMConfig.INJECTED_VMCONFIG), HotSpotReplacementsUtil.CLASS_ARRAY_KLASS_LOCATION).isNull())
        {
            // Exit the intrinsic here for the case where the array class does not exist
            return newInstance(componentType, length);
        }
        return DynamicNewArrayNode.newArray(GraalDirectives.guardingNonNull(componentType), length);
    }
}
