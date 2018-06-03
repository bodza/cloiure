package giraaff.core.common.util;

/**
 * Provides low-level value checks and conversion for signed and unsigned values of size 1, 2, and 4 bytes.
 */
// @class TypeConversion
public final class TypeConversion
{
    // @cons
    private TypeConversion()
    {
        super();
    }

    public static boolean isS1(long __value)
    {
        return __value >= Byte.MIN_VALUE && __value <= Byte.MAX_VALUE;
    }

    public static boolean isU1(long __value)
    {
        return __value >= 0 && __value <= 0xFF;
    }

    public static boolean isS2(long __value)
    {
        return __value >= Short.MIN_VALUE && __value <= Short.MAX_VALUE;
    }

    public static boolean isU2(long __value)
    {
        return __value >= 0 && __value <= 0xFFFF;
    }

    public static boolean isS4(long __value)
    {
        return __value >= Integer.MIN_VALUE && __value <= Integer.MAX_VALUE;
    }

    public static boolean isU4(long __value)
    {
        return __value >= 0 && __value <= 0xFFFFFFFFL;
    }

    public static byte asS1(long __value)
    {
        return (byte) __value;
    }

    public static byte asU1(long __value)
    {
        return (byte) __value;
    }

    public static short asS2(long __value)
    {
        return (short) __value;
    }

    public static short asU2(long __value)
    {
        return (short) __value;
    }

    public static int asS4(long __value)
    {
        return (int) __value;
    }

    public static int asU4(long __value)
    {
        return (int) __value;
    }
}
