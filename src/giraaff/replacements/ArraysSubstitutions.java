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
    public static boolean equals(boolean[] __a, boolean[] __a2)
    {
        if (__a == __a2)
        {
            return true;
        }
        if (__a == null || __a2 == null || __a.length != __a2.length)
        {
            return false;
        }
        return ArrayEqualsNode.equals(__a, __a2, __a.length);
    }

    @MethodSubstitution
    public static boolean equals(byte[] __a, byte[] __a2)
    {
        if (__a == __a2)
        {
            return true;
        }
        if (__a == null || __a2 == null || __a.length != __a2.length)
        {
            return false;
        }
        return ArrayEqualsNode.equals(__a, __a2, __a.length);
    }

    @MethodSubstitution
    public static boolean equals(char[] __a, char[] __a2)
    {
        if (__a == __a2)
        {
            return true;
        }
        if (__a == null || __a2 == null || __a.length != __a2.length)
        {
            return false;
        }
        return ArrayEqualsNode.equals(__a, __a2, __a.length);
    }

    @MethodSubstitution
    public static boolean equals(short[] __a, short[] __a2)
    {
        if (__a == __a2)
        {
            return true;
        }
        if (__a == null || __a2 == null || __a.length != __a2.length)
        {
            return false;
        }
        return ArrayEqualsNode.equals(__a, __a2, __a.length);
    }

    @MethodSubstitution
    public static boolean equals(int[] __a, int[] __a2)
    {
        if (__a == __a2)
        {
            return true;
        }
        if (__a == null || __a2 == null || __a.length != __a2.length)
        {
            return false;
        }
        return ArrayEqualsNode.equals(__a, __a2, __a.length);
    }

    @MethodSubstitution
    public static boolean equals(long[] __a, long[] __a2)
    {
        if (__a == __a2)
        {
            return true;
        }
        if (__a == null || __a2 == null || __a.length != __a2.length)
        {
            return false;
        }
        return ArrayEqualsNode.equals(__a, __a2, __a.length);
    }

    @MethodSubstitution
    public static boolean equals(float[] __a, float[] __a2)
    {
        if (__a == __a2)
        {
            return true;
        }
        if (__a == null || __a2 == null || __a.length != __a2.length)
        {
            return false;
        }
        return ArrayEqualsNode.equals(__a, __a2, __a.length);
    }

    @MethodSubstitution
    public static boolean equals(double[] __a, double[] __a2)
    {
        if (__a == __a2)
        {
            return true;
        }
        if (__a == null || __a2 == null || __a.length != __a2.length)
        {
            return false;
        }
        return ArrayEqualsNode.equals(__a, __a2, __a.length);
    }
}
