package giraaff.replacements;

import giraaff.api.replacements.ClassSubstitution;
import giraaff.api.replacements.MethodSubstitution;
import giraaff.nodes.graphbuilderconf.InvocationPlugin;
import giraaff.nodes.java.LoadFieldNode;
import giraaff.replacements.nodes.ArrayEqualsNode;

/**
 * Substitutions for {@link java.lang.String} methods.
 */
@ClassSubstitution(String.class)
public class StringSubstitutions
{
    @MethodSubstitution(isStatic = false)
    public static boolean equals(final String thisString, Object obj)
    {
        if (thisString == obj)
        {
            return true;
        }
        if (!(obj instanceof String))
        {
            return false;
        }
        String thatString = (String) obj;
        if (thisString.length() != thatString.length())
        {
            return false;
        }
        if (thisString.length() == 0)
        {
            return true;
        }

        final char[] array1 = getValue(thisString);
        final char[] array2 = getValue(thatString);

        return ArrayEqualsNode.equals(array1, array2, array1.length);
    }

    /**
     * Will be intrinsified with an {@link InvocationPlugin} to a {@link LoadFieldNode}.
     */
    public static native char[] getValue(String s);
}
