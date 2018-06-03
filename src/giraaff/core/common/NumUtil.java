package giraaff.core.common;

import jdk.vm.ci.code.CodeUtil;

///
// A collection of static utility functions that check ranges of numbers.
///
// @class NumUtil
public final class NumUtil
{
    // @cons
    private NumUtil()
    {
        super();
    }

    public static boolean isShiftCount(int __x)
    {
        return 0 <= __x && __x < 32;
    }

    ///
    // Determines if a given {@code int} value is the range of unsigned byte values.
    ///
    public static boolean isUByte(int __x)
    {
        return (__x & 0xff) == __x;
    }

    ///
    // Determines if a given {@code int} value is the range of signed byte values.
    ///
    public static boolean isByte(int __x)
    {
        return (byte) __x == __x;
    }

    ///
    // Determines if a given {@code long} value is the range of unsigned byte values.
    ///
    public static boolean isUByte(long __x)
    {
        return (__x & 0xffL) == __x;
    }

    ///
    // Determines if a given {@code long} value is the range of signed byte values.
    ///
    public static boolean isByte(long __l)
    {
        return (byte) __l == __l;
    }

    ///
    // Determines if a given {@code long} value is the range of unsigned int values.
    ///
    public static boolean isUInt(long __x)
    {
        return (__x & 0xffffffffL) == __x;
    }

    ///
    // Determines if a given {@code long} value is the range of signed int values.
    ///
    public static boolean isInt(long __l)
    {
        return (int) __l == __l;
    }

    ///
    // Determines if a given {@code int} value is the range of signed short values.
    ///
    public static boolean isShort(int __x)
    {
        return (short) __x == __x;
    }

    ///
    // Determines if a given {@code long} value is the range of signed short values.
    ///
    public static boolean isShort(long __x)
    {
        return (short) __x == __x;
    }

    public static boolean isUShort(int __s)
    {
        return __s == (__s & 0xFFFF);
    }

    public static boolean isUShort(long __s)
    {
        return __s == (__s & 0xFFFF);
    }

    public static boolean is32bit(long __x)
    {
        return -0x80000000L <= __x && __x < 0x80000000L;
    }

    public static short safeToShort(int __v)
    {
        return (short) __v;
    }

    public static int roundUp(int __number, int __mod)
    {
        return ((__number + __mod - 1) / __mod) * __mod;
    }

    public static long roundUp(long __number, long __mod)
    {
        return ((__number + __mod - 1L) / __mod) * __mod;
    }

    public static int roundDown(int __number, int __mod)
    {
        return __number / __mod * __mod;
    }

    public static long roundDown(long __number, long __mod)
    {
        return __number / __mod * __mod;
    }

    public static int log2Ceil(int __val)
    {
        int __x = 1;
        int __log2 = 0;
        while (__x < __val)
        {
            __log2++;
            __x *= 2;
        }
        return __log2;
    }

    public static boolean isUnsignedNbit(int __n, int __value)
    {
        return 32 - Integer.numberOfLeadingZeros(__value) <= __n;
    }

    public static boolean isUnsignedNbit(int __n, long __value)
    {
        return 64 - Long.numberOfLeadingZeros(__value) <= __n;
    }

    public static boolean isSignedNbit(int __n, int __value)
    {
        int __min = -(1 << (__n - 1));
        int __max = (1 << (__n - 1)) - 1;
        return __value >= __min && __value <= __max;
    }

    public static boolean isSignedNbit(int __n, long __value)
    {
        long __min = -(1L << (__n - 1));
        long __max = (1L << (__n - 1)) - 1;
        return __value >= __min && __value <= __max;
    }

    ///
    // @param n Number of bits that should be set to 1. Must be between 0 and 32 (inclusive).
    // @return A number with n bits set to 1.
    ///
    public static int getNbitNumberInt(int __n)
    {
        if (__n < 32)
        {
            return (1 << __n) - 1;
        }
        else
        {
            return 0xFFFFFFFF;
        }
    }

    ///
    // @param n Number of bits that should be set to 1. Must be between 0 and 64 (inclusive).
    // @return A number with n bits set to 1.
    ///
    public static long getNbitNumberLong(int __n)
    {
        if (__n < 64)
        {
            return (1L << __n) - 1;
        }
        else
        {
            return 0xFFFFFFFFFFFFFFFFL;
        }
    }

    ///
    // Get the minimum value representable in a {@code bits} bit signed integer.
    ///
    public static long minValue(int __bits)
    {
        return CodeUtil.minValue(__bits);
    }

    ///
    // Get the maximum value representable in a {@code bits} bit signed integer.
    ///
    public static long maxValue(int __bits)
    {
        return CodeUtil.maxValue(__bits);
    }

    ///
    // Get the maximum value representable in a {@code bits} bit unsigned integer.
    ///
    public static long maxValueUnsigned(int __bits)
    {
        return getNbitNumberLong(__bits);
    }

    public static long maxUnsigned(long __a, long __b)
    {
        if (Long.compareUnsigned(__a, __b) > 0)
        {
            return __b;
        }
        return __a;
    }

    public static long minUnsigned(long __a, long __b)
    {
        if (Long.compareUnsigned(__a, __b) > 0)
        {
            return __a;
        }
        return __b;
    }

    public static boolean sameSign(long __a, long __b)
    {
        return __a < 0 == __b < 0;
    }
}
