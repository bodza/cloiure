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
public class ArraySubstitutions
{
    @MethodSubstitution
    public static int getLength(Object array)
    {
        if (!array.getClass().isArray())
        {
            DeoptimizeNode.deopt(DeoptimizationAction.None, DeoptimizationReason.RuntimeConstraint);
        }
        return ArrayLengthNode.arrayLength(array);
    }
}
