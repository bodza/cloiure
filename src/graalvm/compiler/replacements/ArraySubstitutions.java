package graalvm.compiler.replacements;

import java.lang.reflect.Array;

import graalvm.compiler.api.replacements.ClassSubstitution;
import graalvm.compiler.api.replacements.MethodSubstitution;
import graalvm.compiler.nodes.DeoptimizeNode;
import graalvm.compiler.nodes.java.ArrayLengthNode;

import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;

/**
 * Substitutions for {@link java.lang.reflect.Array} methods.
 */
@ClassSubstitution(Array.class)
public class ArraySubstitutions {

    @MethodSubstitution
    public static int getLength(Object array) {
        if (!array.getClass().isArray()) {
            DeoptimizeNode.deopt(DeoptimizationAction.None, DeoptimizationReason.RuntimeConstraint);
        }
        return ArrayLengthNode.arrayLength(array);
    }

}
