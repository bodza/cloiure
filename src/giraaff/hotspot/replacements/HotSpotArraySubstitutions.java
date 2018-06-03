package giraaff.hotspot.replacements;

import java.lang.reflect.Array;

import giraaff.api.directives.GraalDirectives;
import giraaff.api.replacements.ClassSubstitution;
import giraaff.api.replacements.MethodSubstitution;
import giraaff.hotspot.HotSpotRuntime;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;
import giraaff.nodes.java.DynamicNewArrayNode;

/**
 * Substitutions for {@link Array} methods.
 */
@ClassSubstitution(Array.class)
// @class HotSpotArraySubstitutions
public final class HotSpotArraySubstitutions
{
    // @cons
    private HotSpotArraySubstitutions()
    {
        super();
    }

    @MethodSubstitution
    public static Object newInstance(Class<?> __componentType, int __length)
    {
        if (__componentType == null || HotSpotReplacementsUtil.loadKlassFromObject(__componentType, HotSpotRuntime.arrayKlassOffset, HotSpotReplacementsUtil.CLASS_ARRAY_KLASS_LOCATION).isNull())
        {
            // exit the intrinsic here for the case where the array class does not exist
            return newInstance(__componentType, __length);
        }
        return DynamicNewArrayNode.newArray(GraalDirectives.guardingNonNull(__componentType), __length);
    }
}
