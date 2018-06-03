package giraaff.replacements;

import giraaff.api.replacements.ClassSubstitution;
import giraaff.api.replacements.MethodSubstitution;
import giraaff.nodes.graphbuilderconf.InvocationPlugin;
import giraaff.nodes.java.LoadFieldNode;
import giraaff.replacements.nodes.ArrayEqualsNode;

///
// Substitutions for {@link java.lang.String} methods.
///
@ClassSubstitution(String.class)
// @class StringSubstitutions
public final class StringSubstitutions
{
    @MethodSubstitution(isStatic = false)
    public static boolean equals(final String __thisString, Object __obj)
    {
        if (__thisString == __obj)
        {
            return true;
        }
        if (!(__obj instanceof String))
        {
            return false;
        }
        String __thatString = (String) __obj;
        if (__thisString.length() != __thatString.length())
        {
            return false;
        }
        if (__thisString.length() == 0)
        {
            return true;
        }

        final char[] __array1 = getValue(__thisString);
        final char[] __array2 = getValue(__thatString);

        return ArrayEqualsNode.equals(__array1, __array2, __array1.length);
    }

    ///
    // Will be intrinsified with an {@link InvocationPlugin} to a {@link LoadFieldNode}.
    ///
    public static native char[] getValue(String __s);
}
