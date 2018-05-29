package giraaff.replacements;

import java.util.Arrays;

import giraaff.api.replacements.ClassSubstitution;
import giraaff.api.replacements.MethodSubstitution;
import giraaff.replacements.nodes.ArrayEqualsNode;

/**
 * Substitutions for {@link java.util.Arrays} methods.
 */
@ClassSubstitution(Arrays.class)
// @class ArraysSubstitutions
public final class ArraysSubstitutions
{
    @MethodSubstitution
    public static boolean equals(boolean[] a, boolean[] a2)
    {
        if (a == a2)
        {
            return true;
        }
        if (a == null || a2 == null || a.length != a2.length)
        {
            return false;
        }
        return ArrayEqualsNode.equals(a, a2, a.length);
    }

    @MethodSubstitution
    public static boolean equals(byte[] a, byte[] a2)
    {
        if (a == a2)
        {
            return true;
        }
        if (a == null || a2 == null || a.length != a2.length)
        {
            return false;
        }
        return ArrayEqualsNode.equals(a, a2, a.length);
    }

    @MethodSubstitution
    public static boolean equals(char[] a, char[] a2)
    {
        if (a == a2)
        {
            return true;
        }
        if (a == null || a2 == null || a.length != a2.length)
        {
            return false;
        }
        return ArrayEqualsNode.equals(a, a2, a.length);
    }

    @MethodSubstitution
    public static boolean equals(short[] a, short[] a2)
    {
        if (a == a2)
        {
            return true;
        }
        if (a == null || a2 == null || a.length != a2.length)
        {
            return false;
        }
        return ArrayEqualsNode.equals(a, a2, a.length);
    }

    @MethodSubstitution
    public static boolean equals(int[] a, int[] a2)
    {
        if (a == a2)
        {
            return true;
        }
        if (a == null || a2 == null || a.length != a2.length)
        {
            return false;
        }
        return ArrayEqualsNode.equals(a, a2, a.length);
    }

    @MethodSubstitution
    public static boolean equals(long[] a, long[] a2)
    {
        if (a == a2)
        {
            return true;
        }
        if (a == null || a2 == null || a.length != a2.length)
        {
            return false;
        }
        return ArrayEqualsNode.equals(a, a2, a.length);
    }

    @MethodSubstitution
    public static boolean equals(float[] a, float[] a2)
    {
        if (a == a2)
        {
            return true;
        }
        if (a == null || a2 == null || a.length != a2.length)
        {
            return false;
        }
        return ArrayEqualsNode.equals(a, a2, a.length);
    }

    @MethodSubstitution
    public static boolean equals(double[] a, double[] a2)
    {
        if (a == a2)
        {
            return true;
        }
        if (a == null || a2 == null || a.length != a2.length)
        {
            return false;
        }
        return ArrayEqualsNode.equals(a, a2, a.length);
    }
}
