package giraaff.core.common.calc;

///
// Utilities for unsigned comparisons. All methods have correct, but slow, standard Java
// implementations so that they can be used with compilers not supporting the intrinsics.
///
// @class UnsignedMath
public final class UnsignedMath
{
    // @cons
    private UnsignedMath()
    {
        super();
    }

    ///
    // Unsigned comparison aboveThan for two numbers.
    ///
    public static boolean aboveThan(int __a, int __b)
    {
        return Integer.compareUnsigned(__a, __b) > 0;
    }

    ///
    // Unsigned comparison aboveOrEqual for two numbers.
    ///
    public static boolean aboveOrEqual(int __a, int __b)
    {
        return Integer.compareUnsigned(__a, __b) >= 0;
    }

    ///
    // Unsigned comparison belowThan for two numbers.
    ///
    public static boolean belowThan(int __a, int __b)
    {
        return Integer.compareUnsigned(__a, __b) < 0;
    }

    ///
    // Unsigned comparison belowOrEqual for two numbers.
    ///
    public static boolean belowOrEqual(int __a, int __b)
    {
        return Integer.compareUnsigned(__a, __b) <= 0;
    }

    ///
    // Unsigned comparison aboveThan for two numbers.
    ///
    public static boolean aboveThan(long __a, long __b)
    {
        return Long.compareUnsigned(__a, __b) > 0;
    }

    ///
    // Unsigned comparison aboveOrEqual for two numbers.
    ///
    public static boolean aboveOrEqual(long __a, long __b)
    {
        return Long.compareUnsigned(__a, __b) >= 0;
    }

    ///
    // Unsigned comparison belowThan for two numbers.
    ///
    public static boolean belowThan(long __a, long __b)
    {
        return Long.compareUnsigned(__a, __b) < 0;
    }

    ///
    // Unsigned comparison belowOrEqual for two numbers.
    ///
    public static boolean belowOrEqual(long __a, long __b)
    {
        return Long.compareUnsigned(__a, __b) <= 0;
    }
}
