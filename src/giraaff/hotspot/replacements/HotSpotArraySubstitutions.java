package giraaff.hotspot.replacements;

import java.lang.reflect.Array;

import giraaff.api.directives.GraalDirectives;
import giraaff.api.replacements.ClassSubstitution;
import giraaff.api.replacements.MethodSubstitution;
import giraaff.hotspot.GraalHotSpotVMConfig;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.nodes.java.DynamicNewArrayNode;

/**
 * Substitutions for {@link Array} methods.
 */
@ClassSubstitution(Array.class)
// @class HotSpotArraySubstitutions
public final class HotSpotArraySubstitutions
{
    @MethodSubstitution
    public static Object newInstance(Class<?> componentType, int length)
    {
        if (componentType == null || HotSpotReplacementsUtil.loadKlassFromObject(componentType, GraalHotSpotVMConfig.arrayKlassOffset, HotSpotReplacementsUtil.CLASS_ARRAY_KLASS_LOCATION).isNull())
        {
            // exit the intrinsic here for the case where the array class does not exist
            return newInstance(componentType, length);
        }
        return DynamicNewArrayNode.newArray(GraalDirectives.guardingNonNull(componentType), length);
    }

    // @cons
    private HotSpotArraySubstitutions()
    {
        super();
    }
}
