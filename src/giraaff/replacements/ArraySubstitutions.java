package giraaff.replacements;

import java.lang.reflect.Array;

import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;

import giraaff.api.replacements.ClassSubstitution;
import giraaff.api.replacements.MethodSubstitution;
import giraaff.nodes.DeoptimizeNode;
import giraaff.nodes.java.ArrayLengthNode;

/**
 * Substitutions for {@link java.lang.reflect.Array} methods.
 */
@ClassSubstitution(Array.class)
// @class ArraySubstitutions
public final class ArraySubstitutions
{
    @MethodSubstitution
    public static int getLength(Object __array)
    {
        if (!__array.getClass().isArray())
        {
            DeoptimizeNode.deopt(DeoptimizationAction.None, DeoptimizationReason.RuntimeConstraint);
        }
        return ArrayLengthNode.arrayLength(__array);
    }
}
