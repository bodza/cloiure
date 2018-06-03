package giraaff.hotspot.replacements;

import giraaff.api.directives.GraalDirectives;
import giraaff.api.replacements.ClassSubstitution;
import giraaff.api.replacements.MethodSubstitution;
import giraaff.hotspot.HotSpotBackend;
import giraaff.hotspot.replacements.HotSpotReplacementsUtil;

@ClassSubstitution(className = "java.math.BigInteger", optional = true)
// @class BigIntegerSubstitutions
public final class BigIntegerSubstitutions
{
    // @cons
    private BigIntegerSubstitutions()
    {
        super();
    }

    @MethodSubstitution(isStatic = false)
    static int[] multiplyToLen(@SuppressWarnings("unused") Object __receiver, int[] __x, int __xlen, int[] __y, int __ylen, int[] __zIn)
    {
        return multiplyToLenStatic(__x, __xlen, __y, __ylen, __zIn);
    }

    @MethodSubstitution(isStatic = true)
    static int[] multiplyToLenStatic(int[] __x, int __xlen, int[] __y, int __ylen, int[] __zIn)
    {
        int[] __zResult = __zIn;
        int __zLen;
        if (__zResult == null || __zResult.length < (__xlen + __ylen))
        {
            __zLen = __xlen + __ylen;
            __zResult = new int[__xlen + __ylen];
        }
        else
        {
            __zLen = __zIn.length;
        }
        HotSpotBackend.multiplyToLenStub(HotSpotReplacementsUtil.arrayStart(__x), __xlen, HotSpotReplacementsUtil.arrayStart(__y), __ylen, HotSpotReplacementsUtil.arrayStart(__zResult), __zLen);
        return __zResult;
    }

    @MethodSubstitution(isStatic = true)
    static int mulAdd(int[] __out, int[] __in, int __offset, int __len, int __k)
    {
        int[] __outNonNull = GraalDirectives.guardingNonNull(__out);
        int __newOffset = __outNonNull.length - __offset;
        return HotSpotBackend.mulAddStub(HotSpotReplacementsUtil.arrayStart(__outNonNull), HotSpotReplacementsUtil.arrayStart(__in), __newOffset, __len, __k);
    }

    @MethodSubstitution(isStatic = true)
    static int implMulAdd(int[] __out, int[] __in, int __offset, int __len, int __k)
    {
        int[] __outNonNull = GraalDirectives.guardingNonNull(__out);
        int __newOffset = __outNonNull.length - __offset;
        return HotSpotBackend.mulAddStub(HotSpotReplacementsUtil.arrayStart(__outNonNull), HotSpotReplacementsUtil.arrayStart(__in), __newOffset, __len, __k);
    }

    @MethodSubstitution(isStatic = true)
    static int[] implMontgomeryMultiply(int[] __a, int[] __b, int[] __n, int __len, long __inv, int[] __product)
    {
        HotSpotBackend.implMontgomeryMultiply(HotSpotReplacementsUtil.arrayStart(__a), HotSpotReplacementsUtil.arrayStart(__b), HotSpotReplacementsUtil.arrayStart(__n), __len, __inv, HotSpotReplacementsUtil.arrayStart(__product));
        return __product;
    }

    @MethodSubstitution(isStatic = true)
    static int[] implMontgomerySquare(int[] __a, int[] __n, int __len, long __inv, int[] __product)
    {
        HotSpotBackend.implMontgomerySquare(HotSpotReplacementsUtil.arrayStart(__a), HotSpotReplacementsUtil.arrayStart(__n), __len, __inv, HotSpotReplacementsUtil.arrayStart(__product));
        return __product;
    }

    @MethodSubstitution(isStatic = true)
    static int[] implSquareToLen(int[] __x, int __len, int[] __z, int __zLen)
    {
        HotSpotBackend.implSquareToLen(HotSpotReplacementsUtil.arrayStart(__x), __len, HotSpotReplacementsUtil.arrayStart(__z), __zLen);
        return __z;
    }
}
