package graalvm.compiler.hotspot.replacements;

import graalvm.compiler.api.directives.GraalDirectives;
import graalvm.compiler.api.replacements.ClassSubstitution;
import graalvm.compiler.api.replacements.MethodSubstitution;
import graalvm.compiler.hotspot.HotSpotBackend;
import graalvm.compiler.hotspot.replacements.HotSpotReplacementsUtil;

@ClassSubstitution(className = "java.math.BigInteger", optional = true)
public class BigIntegerSubstitutions
{
    @MethodSubstitution(isStatic = false)
    static int[] multiplyToLen(@SuppressWarnings("unused") Object receiver, int[] x, int xlen, int[] y, int ylen, int[] zIn)
    {
        return multiplyToLenStatic(x, xlen, y, ylen, zIn);
    }

    @MethodSubstitution(isStatic = true)
    static int[] multiplyToLenStatic(int[] x, int xlen, int[] y, int ylen, int[] zIn)
    {
        int[] zResult = zIn;
        int zLen;
        if (zResult == null || zResult.length < (xlen + ylen))
        {
            zLen = xlen + ylen;
            zResult = new int[xlen + ylen];
        }
        else
        {
            zLen = zIn.length;
        }
        HotSpotBackend.multiplyToLenStub(HotSpotReplacementsUtil.arrayStart(x), xlen, HotSpotReplacementsUtil.arrayStart(y), ylen, HotSpotReplacementsUtil.arrayStart(zResult), zLen);
        return zResult;
    }

    @MethodSubstitution(isStatic = true)
    static int mulAdd(int[] out, int[] in, int offset, int len, int k)
    {
        int[] outNonNull = GraalDirectives.guardingNonNull(out);
        int newOffset = outNonNull.length - offset;
        return HotSpotBackend.mulAddStub(HotSpotReplacementsUtil.arrayStart(outNonNull), HotSpotReplacementsUtil.arrayStart(in), newOffset, len, k);
    }

    @MethodSubstitution(isStatic = true)
    static int implMulAdd(int[] out, int[] in, int offset, int len, int k)
    {
        int[] outNonNull = GraalDirectives.guardingNonNull(out);
        int newOffset = outNonNull.length - offset;
        return HotSpotBackend.mulAddStub(HotSpotReplacementsUtil.arrayStart(outNonNull), HotSpotReplacementsUtil.arrayStart(in), newOffset, len, k);
    }

    @MethodSubstitution(isStatic = true)
    static int[] implMontgomeryMultiply(int[] a, int[] b, int[] n, int len, long inv, int[] product)
    {
        HotSpotBackend.implMontgomeryMultiply(HotSpotReplacementsUtil.arrayStart(a), HotSpotReplacementsUtil.arrayStart(b), HotSpotReplacementsUtil.arrayStart(n), len, inv, HotSpotReplacementsUtil.arrayStart(product));
        return product;
    }

    @MethodSubstitution(isStatic = true)
    static int[] implMontgomerySquare(int[] a, int[] n, int len, long inv, int[] product)
    {
        HotSpotBackend.implMontgomerySquare(HotSpotReplacementsUtil.arrayStart(a), HotSpotReplacementsUtil.arrayStart(n), len, inv, HotSpotReplacementsUtil.arrayStart(product));
        return product;
    }

    @MethodSubstitution(isStatic = true)
    static int[] implSquareToLen(int[] x, int len, int[] z, int zLen)
    {
        HotSpotBackend.implSquareToLen(HotSpotReplacementsUtil.arrayStart(x), len, HotSpotReplacementsUtil.arrayStart(z), zLen);
        return z;
    }
}
