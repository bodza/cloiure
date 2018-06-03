package giraaff.core.common.type;

import java.nio.ByteBuffer;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.PrimitiveConstant;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.meta.SerializableConstant;

import giraaff.core.common.LIRKind;
import giraaff.core.common.NumUtil;
import giraaff.core.common.calc.FloatConvert;
import giraaff.core.common.spi.LIRKindTool;
import giraaff.core.common.type.ArithmeticOpTable.BinaryOp;
import giraaff.core.common.type.ArithmeticOpTable.FloatConvertOp;
import giraaff.core.common.type.ArithmeticOpTable.IntegerConvertOp;
import giraaff.core.common.type.ArithmeticOpTable.ShiftOp;
import giraaff.core.common.type.ArithmeticOpTable.UnaryOp;
import giraaff.util.GraalError;

/**
 * Describes the possible values of a node that produces an int or long result.
 *
 * The description consists of (inclusive) lower and upper bounds and up (may be set) and down
 * (always set) bit-masks.
 */
// @class IntegerStamp
public final class IntegerStamp extends PrimitiveStamp
{
    // @field
    private final long lowerBound;
    // @field
    private final long upperBound;
    // @field
    private final long downMask;
    // @field
    private final long upMask;

    // @cons
    private IntegerStamp(int __bits, long __lowerBound, long __upperBound, long __downMask, long __upMask)
    {
        super(__bits, OPS);

        this.lowerBound = __lowerBound;
        this.upperBound = __upperBound;
        this.downMask = __downMask;
        this.upMask = __upMask;
    }

    public static IntegerStamp create(int __bits, long __lowerBoundInput, long __upperBoundInput)
    {
        return create(__bits, __lowerBoundInput, __upperBoundInput, 0, CodeUtil.mask(__bits));
    }

    public static IntegerStamp create(int __bits, long __lowerBoundInput, long __upperBoundInput, long __downMask, long __upMask)
    {
        // Set lower bound, use masks to make it more precise.
        long __minValue = minValueForMasks(__bits, __downMask, __upMask);
        long __lowerBoundTmp = Math.max(__lowerBoundInput, __minValue);

        // Set upper bound, use masks to make it more precise.
        long __maxValue = maxValueForMasks(__bits, __downMask, __upMask);
        long __upperBoundTmp = Math.min(__upperBoundInput, __maxValue);

        // Assign masks now with the bounds in mind.
        final long __boundedDownMask;
        final long __boundedUpMask;
        long __defaultMask = CodeUtil.mask(__bits);
        if (__lowerBoundTmp == __upperBoundTmp)
        {
            __boundedDownMask = __lowerBoundTmp;
            __boundedUpMask = __lowerBoundTmp;
        }
        else if (__lowerBoundTmp >= 0)
        {
            int __upperBoundLeadingZeros = Long.numberOfLeadingZeros(__upperBoundTmp);
            long __differentBits = __lowerBoundTmp ^ __upperBoundTmp;
            int __sameBitCount = Long.numberOfLeadingZeros(__differentBits << __upperBoundLeadingZeros);

            __boundedUpMask = __upperBoundTmp | -1L >>> (__upperBoundLeadingZeros + __sameBitCount);
            __boundedDownMask = __upperBoundTmp & ~(-1L >>> (__upperBoundLeadingZeros + __sameBitCount));
        }
        else
        {
            if (__upperBoundTmp >= 0)
            {
                __boundedUpMask = __defaultMask;
                __boundedDownMask = 0;
            }
            else
            {
                int __lowerBoundLeadingOnes = Long.numberOfLeadingZeros(~__lowerBoundTmp);
                long __differentBits = __lowerBoundTmp ^ __upperBoundTmp;
                int __sameBitCount = Long.numberOfLeadingZeros(__differentBits << __lowerBoundLeadingOnes);

                __boundedUpMask = __lowerBoundTmp | -1L >>> (__lowerBoundLeadingOnes + __sameBitCount) | ~(-1L >>> __lowerBoundLeadingOnes);
                __boundedDownMask = __lowerBoundTmp & ~(-1L >>> (__lowerBoundLeadingOnes + __sameBitCount)) | ~(-1L >>> __lowerBoundLeadingOnes);
            }
        }

        return new IntegerStamp(__bits, __lowerBoundTmp, __upperBoundTmp, __defaultMask & (__downMask | __boundedDownMask), __defaultMask & __upMask & __boundedUpMask);
    }

    private static long significantBit(long __bits, long __value)
    {
        return (__value >>> (__bits - 1)) & 1;
    }

    private static long minValueForMasks(int __bits, long __downMask, long __upMask)
    {
        if (significantBit(__bits, __upMask) == 0)
        {
            // Value is always positive. Minimum value always positive.
            return __downMask;
        }
        else
        {
            // Value can be positive or negative. Minimum value always negative.
            return __downMask | (-1L << (__bits - 1));
        }
    }

    private static long maxValueForMasks(int __bits, long __downMask, long __upMask)
    {
        if (significantBit(__bits, __downMask) == 1)
        {
            // Value is always negative. Maximum value always negative.
            return CodeUtil.signExtend(__upMask, __bits);
        }
        else
        {
            // Value can be positive or negative. Maximum value always positive.
            return __upMask & (CodeUtil.mask(__bits) >>> 1);
        }
    }

    public static IntegerStamp stampForMask(int __bits, long __downMask, long __upMask)
    {
        return new IntegerStamp(__bits, minValueForMasks(__bits, __downMask, __upMask), maxValueForMasks(__bits, __downMask, __upMask), __downMask, __upMask);
    }

    @Override
    public IntegerStamp unrestricted()
    {
        return new IntegerStamp(getBits(), CodeUtil.minValue(getBits()), CodeUtil.maxValue(getBits()), 0, CodeUtil.mask(getBits()));
    }

    @Override
    public IntegerStamp empty()
    {
        return new IntegerStamp(getBits(), CodeUtil.maxValue(getBits()), CodeUtil.minValue(getBits()), CodeUtil.mask(getBits()), 0);
    }

    @Override
    public Stamp constant(Constant __c, MetaAccessProvider __meta)
    {
        if (__c instanceof PrimitiveConstant)
        {
            long __value = ((PrimitiveConstant) __c).asLong();
            return StampFactory.forInteger(getBits(), __value, __value);
        }
        return this;
    }

    @Override
    public SerializableConstant deserialize(ByteBuffer __buffer)
    {
        switch (getBits())
        {
            case 1:
                return JavaConstant.forBoolean(__buffer.get() != 0);
            case 8:
                return JavaConstant.forByte(__buffer.get());
            case 16:
                return JavaConstant.forShort(__buffer.getShort());
            case 32:
                return JavaConstant.forInt(__buffer.getInt());
            case 64:
                return JavaConstant.forLong(__buffer.getLong());
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public boolean hasValues()
    {
        return lowerBound <= upperBound;
    }

    @Override
    public JavaKind getStackKind()
    {
        if (getBits() > 32)
        {
            return JavaKind.Long;
        }
        else
        {
            return JavaKind.Int;
        }
    }

    @Override
    public LIRKind getLIRKind(LIRKindTool __tool)
    {
        return __tool.getIntegerKind(getBits());
    }

    @Override
    public ResolvedJavaType javaType(MetaAccessProvider __metaAccess)
    {
        switch (getBits())
        {
            case 1:
                return __metaAccess.lookupJavaType(Boolean.TYPE);
            case 8:
                return __metaAccess.lookupJavaType(Byte.TYPE);
            case 16:
                return __metaAccess.lookupJavaType(Short.TYPE);
            case 32:
                return __metaAccess.lookupJavaType(Integer.TYPE);
            case 64:
                return __metaAccess.lookupJavaType(Long.TYPE);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    /**
     * The signed inclusive lower bound on the value described by this stamp.
     */
    public long lowerBound()
    {
        return lowerBound;
    }

    /**
     * The signed inclusive upper bound on the value described by this stamp.
     */
    public long upperBound()
    {
        return upperBound;
    }

    /**
     * This bit-mask describes the bits that are always set in the value described by this stamp.
     */
    public long downMask()
    {
        return downMask;
    }

    /**
     * This bit-mask describes the bits that can be set in the value described by this stamp.
     */
    public long upMask()
    {
        return upMask;
    }

    @Override
    public boolean isUnrestricted()
    {
        return lowerBound == CodeUtil.minValue(getBits()) && upperBound == CodeUtil.maxValue(getBits()) && downMask == 0 && upMask == CodeUtil.mask(getBits());
    }

    public boolean contains(long __value)
    {
        return __value >= lowerBound && __value <= upperBound && (__value & downMask) == downMask && (__value & upMask) == (__value & CodeUtil.mask(getBits()));
    }

    public boolean isPositive()
    {
        return lowerBound() >= 0;
    }

    public boolean isNegative()
    {
        return upperBound() <= 0;
    }

    public boolean isStrictlyPositive()
    {
        return lowerBound() > 0;
    }

    public boolean isStrictlyNegative()
    {
        return upperBound() < 0;
    }

    public boolean canBePositive()
    {
        return upperBound() > 0;
    }

    public boolean canBeNegative()
    {
        return lowerBound() < 0;
    }

    private IntegerStamp createStamp(IntegerStamp __other, long __newUpperBound, long __newLowerBound, long __newDownMask, long __newUpMask)
    {
        if (__newLowerBound > __newUpperBound || (__newDownMask & (~__newUpMask)) != 0 || (__newUpMask == 0 && (__newLowerBound > 0 || __newUpperBound < 0)))
        {
            return empty();
        }
        else if (__newLowerBound == lowerBound && __newUpperBound == upperBound && __newDownMask == downMask && __newUpMask == upMask)
        {
            return this;
        }
        else if (__newLowerBound == __other.lowerBound && __newUpperBound == __other.upperBound && __newDownMask == __other.downMask && __newUpMask == __other.upMask)
        {
            return __other;
        }
        else
        {
            return IntegerStamp.create(getBits(), __newLowerBound, __newUpperBound, __newDownMask, __newUpMask);
        }
    }

    @Override
    public Stamp meet(Stamp __otherStamp)
    {
        if (__otherStamp == this)
        {
            return this;
        }
        if (isEmpty())
        {
            return __otherStamp;
        }
        if (__otherStamp.isEmpty())
        {
            return this;
        }
        IntegerStamp __other = (IntegerStamp) __otherStamp;
        return createStamp(__other, Math.max(upperBound, __other.upperBound), Math.min(lowerBound, __other.lowerBound), downMask & __other.downMask, upMask | __other.upMask);
    }

    @Override
    public IntegerStamp join(Stamp __otherStamp)
    {
        if (__otherStamp == this)
        {
            return this;
        }
        IntegerStamp __other = (IntegerStamp) __otherStamp;
        long __newDownMask = downMask | __other.downMask;
        long __newLowerBound = Math.max(lowerBound, __other.lowerBound);
        long __newUpperBound = Math.min(upperBound, __other.upperBound);
        long __newUpMask = upMask & __other.upMask;
        return createStamp(__other, __newUpperBound, __newLowerBound, __newDownMask, __newUpMask);
    }

    @Override
    public boolean isCompatible(Stamp __stamp)
    {
        if (this == __stamp)
        {
            return true;
        }
        if (__stamp instanceof IntegerStamp)
        {
            IntegerStamp __other = (IntegerStamp) __stamp;
            return getBits() == __other.getBits();
        }
        return false;
    }

    @Override
    public boolean isCompatible(Constant __constant)
    {
        if (__constant instanceof PrimitiveConstant)
        {
            PrimitiveConstant __prim = (PrimitiveConstant) __constant;
            return __prim.getJavaKind().isNumericInteger();
        }
        return false;
    }

    public long unsignedUpperBound()
    {
        if (sameSignBounds())
        {
            return CodeUtil.zeroExtend(upperBound(), getBits());
        }
        return NumUtil.maxValueUnsigned(getBits());
    }

    public long unsignedLowerBound()
    {
        if (sameSignBounds())
        {
            return CodeUtil.zeroExtend(lowerBound(), getBits());
        }
        return 0;
    }

    private boolean sameSignBounds()
    {
        return NumUtil.sameSign(lowerBound, upperBound);
    }

    @Override
    public int hashCode()
    {
        final int __prime = 31;
        int __result = 1;
        __result = __prime * __result + super.hashCode();
        __result = __prime * __result + (int) (lowerBound ^ (lowerBound >>> 32));
        __result = __prime * __result + (int) (upperBound ^ (upperBound >>> 32));
        __result = __prime * __result + (int) (downMask ^ (downMask >>> 32));
        __result = __prime * __result + (int) (upMask ^ (upMask >>> 32));
        return __result;
    }

    @Override
    public boolean equals(Object __obj)
    {
        if (this == __obj)
        {
            return true;
        }
        if (__obj == null || getClass() != __obj.getClass() || !super.equals(__obj))
        {
            return false;
        }
        IntegerStamp __other = (IntegerStamp) __obj;
        if (lowerBound != __other.lowerBound || upperBound != __other.upperBound || downMask != __other.downMask || upMask != __other.upMask)
        {
            return false;
        }
        return super.equals(__other);
    }

    private static long upMaskFor(int __bits, long __lowerBound, long __upperBound)
    {
        long __mask = __lowerBound | __upperBound;
        if (__mask == 0)
        {
            return 0;
        }
        else
        {
            return ((-1L) >>> Long.numberOfLeadingZeros(__mask)) & CodeUtil.mask(__bits);
        }
    }

    /**
     * Checks if the 2 stamps represent values of the same sign. Returns true if the two stamps are
     * both positive of null or if they are both strictly negative
     *
     * @return true if the two stamps are both positive of null or if they are both strictly negative
     */
    public static boolean sameSign(IntegerStamp __s1, IntegerStamp __s2)
    {
        return __s1.isPositive() && __s2.isPositive() || __s1.isStrictlyNegative() && __s2.isStrictlyNegative();
    }

    @Override
    public JavaConstant asConstant()
    {
        if (lowerBound == upperBound)
        {
            switch (getBits())
            {
                case 1:
                    return JavaConstant.forBoolean(lowerBound != 0);
                case 8:
                    return JavaConstant.forByte((byte) lowerBound);
                case 16:
                    return JavaConstant.forShort((short) lowerBound);
                case 32:
                    return JavaConstant.forInt((int) lowerBound);
                case 64:
                    return JavaConstant.forLong(lowerBound);
            }
        }
        return null;
    }

    public static boolean addCanOverflow(IntegerStamp __a, IntegerStamp __b)
    {
        return addOverflowsPositively(__a.upperBound(), __b.upperBound(), __a.getBits()) || addOverflowsNegatively(__a.lowerBound(), __b.lowerBound(), __a.getBits());
    }

    public static boolean addOverflowsPositively(long __x, long __y, int __bits)
    {
        long __result = __x + __y;
        if (__bits == 64)
        {
            return (~__x & ~__y & __result) < 0;
        }
        else
        {
            return __result > CodeUtil.maxValue(__bits);
        }
    }

    public static boolean addOverflowsNegatively(long __x, long __y, int __bits)
    {
        long __result = __x + __y;
        if (__bits == 64)
        {
            return (__x & __y & ~__result) < 0;
        }
        else
        {
            return __result < CodeUtil.minValue(__bits);
        }
    }

    public static long carryBits(long __x, long __y)
    {
        return (__x + __y) ^ __x ^ __y;
    }

    private static long saturate(long __v, int __bits)
    {
        if (__bits < 64)
        {
            long __max = CodeUtil.maxValue(__bits);
            if (__v > __max)
            {
                return __max;
            }
            long __min = CodeUtil.minValue(__bits);
            if (__v < __min)
            {
                return __min;
            }
        }
        return __v;
    }

    public static boolean multiplicationOverflows(long __a, long __b, int __bits)
    {
        long __result = __a * __b;
        // result is positive if the sign is the same
        boolean __positive = (__a >= 0 && __b >= 0) || (__a < 0 && __b < 0);
        if (__bits == 64)
        {
            if (__a > 0 && __b > 0)
            {
                return __a > 0x7FFFFFFF_FFFFFFFFL / __b;
            }
            else if (__a > 0 && __b <= 0)
            {
                return __b < 0x80000000_00000000L / __a;
            }
            else if (__a <= 0 && __b > 0)
            {
                return __a < 0x80000000_00000000L / __b;
            }
            else
            {
                // a<=0 && b<=0
                return __a != 0 && __b < 0x7FFFFFFF_FFFFFFFFL / __a;
            }
        }
        else
        {
            if (__positive)
            {
                return __result > CodeUtil.maxValue(__bits);
            }
            else
            {
                return __result < CodeUtil.minValue(__bits);
            }
        }
    }

    public static boolean multiplicationCanOverflow(IntegerStamp __a, IntegerStamp __b)
    {
        // see IntegerStamp#foldStamp for details
        if (__a.upMask() == 0)
        {
            return false;
        }
        else if (__b.upMask() == 0)
        {
            return false;
        }
        if (__a.isUnrestricted())
        {
            return true;
        }
        if (__b.isUnrestricted())
        {
            return true;
        }
        int __bits = __a.getBits();
        long __minNegA = __a.lowerBound();
        long __maxNegA = Math.min(0, __a.upperBound());
        long __minPosA = Math.max(0, __a.lowerBound());
        long __maxPosA = __a.upperBound();

        long __minNegB = __b.lowerBound();
        long __maxNegB = Math.min(0, __b.upperBound());
        long __minPosB = Math.max(0, __b.lowerBound());
        long __maxPosB = __b.upperBound();

        boolean __mayOverflow = false;
        if (__a.canBePositive())
        {
            if (__b.canBePositive())
            {
                __mayOverflow |= IntegerStamp.multiplicationOverflows(__maxPosA, __maxPosB, __bits);
                __mayOverflow |= IntegerStamp.multiplicationOverflows(__minPosA, __minPosB, __bits);
            }
            if (__b.canBeNegative())
            {
                __mayOverflow |= IntegerStamp.multiplicationOverflows(__minPosA, __maxNegB, __bits);
                __mayOverflow |= IntegerStamp.multiplicationOverflows(__maxPosA, __minNegB, __bits);
            }
        }
        if (__a.canBeNegative())
        {
            if (__b.canBePositive())
            {
                __mayOverflow |= IntegerStamp.multiplicationOverflows(__maxNegA, __minPosB, __bits);
                __mayOverflow |= IntegerStamp.multiplicationOverflows(__minNegA, __maxPosB, __bits);
            }
            if (__b.canBeNegative())
            {
                __mayOverflow |= IntegerStamp.multiplicationOverflows(__minNegA, __minNegB, __bits);
                __mayOverflow |= IntegerStamp.multiplicationOverflows(__maxNegA, __maxNegB, __bits);
            }
        }
        return __mayOverflow;
    }

    public static boolean subtractionCanOverflow(IntegerStamp __x, IntegerStamp __y)
    {
        return subtractionOverflows(__x.lowerBound(), __y.upperBound(), __x.getBits()) || subtractionOverflows(__x.upperBound(), __y.lowerBound(), __x.getBits());
    }

    public static boolean subtractionOverflows(long __x, long __y, int __bits)
    {
        long __result = __x - __y;
        if (__bits == 64)
        {
            return (((__x ^ __y) & (__x ^ __result)) < 0);
        }
        return __result < CodeUtil.minValue(__bits) || __result > CodeUtil.maxValue(__bits);
    }

    // @def
    public static final ArithmeticOpTable OPS = new ArithmeticOpTable(
        // @closure
        new UnaryOp.Neg()
        {
            @Override
            public Constant foldConstant(Constant __value)
            {
                PrimitiveConstant __c = (PrimitiveConstant) __value;
                return JavaConstant.forIntegerKind(__c.getJavaKind(), -__c.asLong());
            }

            @Override
            public Stamp foldStamp(Stamp __s)
            {
                if (__s.isEmpty())
                {
                    return __s;
                }
                IntegerStamp __stamp = (IntegerStamp) __s;
                int __bits = __stamp.getBits();
                if (__stamp.lowerBound == __stamp.upperBound)
                {
                    long __value = CodeUtil.convert(-__stamp.lowerBound(), __stamp.getBits(), false);
                    return StampFactory.forInteger(__stamp.getBits(), __value, __value);
                }
                if (__stamp.lowerBound() != CodeUtil.minValue(__bits))
                {
                    // TODO check if the mask calculation is correct
                    return StampFactory.forInteger(__bits, -__stamp.upperBound(), -__stamp.lowerBound());
                }
                else
                {
                    return __stamp.unrestricted();
                }
            }
        },

        // @closure
        new BinaryOp.Add(true, true)
        {
            @Override
            public Constant foldConstant(Constant __const1, Constant __const2)
            {
                PrimitiveConstant __a = (PrimitiveConstant) __const1;
                PrimitiveConstant __b = (PrimitiveConstant) __const2;
                return JavaConstant.forIntegerKind(__a.getJavaKind(), __a.asLong() + __b.asLong());
            }

            @Override
            public Stamp foldStamp(Stamp __stamp1, Stamp __stamp2)
            {
                if (__stamp1.isEmpty())
                {
                    return __stamp1;
                }
                if (__stamp2.isEmpty())
                {
                    return __stamp2;
                }
                IntegerStamp __a = (IntegerStamp) __stamp1;
                IntegerStamp __b = (IntegerStamp) __stamp2;

                int __bits = __a.getBits();

                if (__a.lowerBound == __a.upperBound && __b.lowerBound == __b.upperBound)
                {
                    long __value = CodeUtil.convert(__a.lowerBound() + __b.lowerBound(), __a.getBits(), false);
                    return StampFactory.forInteger(__a.getBits(), __value, __value);
                }

                if (__a.isUnrestricted())
                {
                    return __a;
                }
                else if (__b.isUnrestricted())
                {
                    return __b;
                }
                long __defaultMask = CodeUtil.mask(__bits);
                long __variableBits = (__a.downMask() ^ __a.upMask()) | (__b.downMask() ^ __b.upMask());
                long __variableBitsWithCarry = __variableBits | (carryBits(__a.downMask(), __b.downMask()) ^ carryBits(__a.upMask(), __b.upMask()));
                long __newDownMask = (__a.downMask() + __b.downMask()) & ~__variableBitsWithCarry;
                long __newUpMask = (__a.downMask() + __b.downMask()) | __variableBitsWithCarry;

                __newDownMask &= __defaultMask;
                __newUpMask &= __defaultMask;

                long __newLowerBound;
                long __newUpperBound;
                boolean __lowerOverflowsPositively = addOverflowsPositively(__a.lowerBound(), __b.lowerBound(), __bits);
                boolean __upperOverflowsPositively = addOverflowsPositively(__a.upperBound(), __b.upperBound(), __bits);
                boolean __lowerOverflowsNegatively = addOverflowsNegatively(__a.lowerBound(), __b.lowerBound(), __bits);
                boolean __upperOverflowsNegatively = addOverflowsNegatively(__a.upperBound(), __b.upperBound(), __bits);
                if ((__lowerOverflowsNegatively && !__upperOverflowsNegatively) || (!__lowerOverflowsPositively && __upperOverflowsPositively))
                {
                    __newLowerBound = CodeUtil.minValue(__bits);
                    __newUpperBound = CodeUtil.maxValue(__bits);
                }
                else
                {
                    __newLowerBound = CodeUtil.signExtend((__a.lowerBound() + __b.lowerBound()) & __defaultMask, __bits);
                    __newUpperBound = CodeUtil.signExtend((__a.upperBound() + __b.upperBound()) & __defaultMask, __bits);
                }
                IntegerStamp __limit = StampFactory.forInteger(__bits, __newLowerBound, __newUpperBound);
                __newUpMask &= __limit.upMask();
                __newUpperBound = CodeUtil.signExtend(__newUpperBound & __newUpMask, __bits);
                __newDownMask |= __limit.downMask();
                __newLowerBound |= __newDownMask;
                return new IntegerStamp(__bits, __newLowerBound, __newUpperBound, __newDownMask, __newUpMask);
            }

            @Override
            public boolean isNeutral(Constant __value)
            {
                PrimitiveConstant __n = (PrimitiveConstant) __value;
                return __n.asLong() == 0;
            }
        },

        // @closure
        new BinaryOp.Sub(true, false)
        {
            @Override
            public Constant foldConstant(Constant __const1, Constant __const2)
            {
                PrimitiveConstant __a = (PrimitiveConstant) __const1;
                PrimitiveConstant __b = (PrimitiveConstant) __const2;
                return JavaConstant.forIntegerKind(__a.getJavaKind(), __a.asLong() - __b.asLong());
            }

            @Override
            public Stamp foldStamp(Stamp __a, Stamp __b)
            {
                return OPS.getAdd().foldStamp(__a, OPS.getNeg().foldStamp(__b));
            }

            @Override
            public boolean isNeutral(Constant __value)
            {
                PrimitiveConstant __n = (PrimitiveConstant) __value;
                return __n.asLong() == 0;
            }

            @Override
            public Constant getZero(Stamp __s)
            {
                IntegerStamp __stamp = (IntegerStamp) __s;
                return JavaConstant.forPrimitiveInt(__stamp.getBits(), 0);
            }
        },

        // @closure
        new BinaryOp.Mul(true, true)
        {
            @Override
            public Constant foldConstant(Constant __const1, Constant __const2)
            {
                PrimitiveConstant __a = (PrimitiveConstant) __const1;
                PrimitiveConstant __b = (PrimitiveConstant) __const2;
                return JavaConstant.forIntegerKind(__a.getJavaKind(), __a.asLong() * __b.asLong());
            }

            @Override
            public Stamp foldStamp(Stamp __stamp1, Stamp __stamp2)
            {
                if (__stamp1.isEmpty())
                {
                    return __stamp1;
                }
                if (__stamp2.isEmpty())
                {
                    return __stamp2;
                }
                IntegerStamp __a = (IntegerStamp) __stamp1;
                IntegerStamp __b = (IntegerStamp) __stamp2;

                int __bits = __a.getBits();

                if (__a.lowerBound == __a.upperBound && __b.lowerBound == __b.upperBound)
                {
                    long __value = CodeUtil.convert(__a.lowerBound() * __b.lowerBound(), __a.getBits(), false);
                    return StampFactory.forInteger(__a.getBits(), __value, __value);
                }

                // if a==0 or b==0 result of a*b is always 0
                if (__a.upMask() == 0)
                {
                    return __a;
                }
                else if (__b.upMask() == 0)
                {
                    return __b;
                }
                else
                {
                    // if a has the full range or b, the result will also have it
                    if (__a.isUnrestricted())
                    {
                        return __a;
                    }
                    else if (__b.isUnrestricted())
                    {
                        return __b;
                    }
                    // a!=0 && b!=0 holds
                    long __newLowerBound = Long.MAX_VALUE;
                    long __newUpperBound = Long.MIN_VALUE;
                    /*
                     * Based on the signs of the incoming stamps lower and upper bound
                     * of the result of the multiplication may be swapped. LowerBound
                     * can become upper bound if both signs are negative, and so on. To
                     * determine the new values for lower and upper bound we need to
                     * look at the max and min of the cases blow:
                     *
                     * a.lowerBound * b.lowerBound
                     * a.lowerBound * b.upperBound
                     * a.upperBound * b.lowerBound
                     * a.upperBound * b.upperBound
                     *
                     * We are only interested in those cases that are relevant due to
                     * the sign of the involved stamps (whether a stamp includes
                     * negative and / or positive values). Based on the signs, the maximum
                     * or minimum of the above multiplications form the new lower and
                     * upper bounds.
                     *
                     * The table below contains the interesting candidates for lower and
                     * upper bound after multiplication.
                     *
                     * For example if we consider two stamps a & b that both contain
                     * negative and positive values, the product of minNegA * minNegB
                     * (both the smallest negative value for each stamp) can only be the
                     * highest positive number. The other candidates can be computed in
                     * a similar fashion. Some of them can never be a new minimum or
                     * maximum and are therefore excluded.
                     *
                     *          [x................0................y]
                     *          -------------------------------------
                     *          [minNeg     maxNeg minPos     maxPos]
                     *
                     *          where maxNeg = min(0,y) && minPos = max(0,x)
                     *
                     *
                     *                 |minNegA  maxNegA    minPosA  maxPosA
                     *         _______ |____________________________________
                     *         minNegB | MAX        /     :     /      MIN
                     *         maxNegB |  /        MIN    :    MAX      /
                     *                 |------------------+-----------------
                     *         minPosB |  /        MAX    :    MIN      /
                     *         maxPosB | MIN        /     :     /      MAX
                     */
                    // We materialize all factors here. If they are needed, the signs of
                    // the stamp will ensure the correct value is used.
                    long __minNegA = __a.lowerBound();
                    long __maxNegA = Math.min(0, __a.upperBound());
                    long __minPosA = Math.max(0, __a.lowerBound());
                    long __maxPosA = __a.upperBound();

                    long __minNegB = __b.lowerBound();
                    long __maxNegB = Math.min(0, __b.upperBound());
                    long __minPosB = Math.max(0, __b.lowerBound());
                    long __maxPosB = __b.upperBound();

                    // multiplication has shift semantics
                    long __newUpMask = ~CodeUtil.mask(Math.min(64, Long.numberOfTrailingZeros(__a.upMask) + Long.numberOfTrailingZeros(__b.upMask))) & CodeUtil.mask(__bits);

                    if (__a.canBePositive())
                    {
                        if (__b.canBePositive())
                        {
                            if (multiplicationOverflows(__maxPosA, __maxPosB, __bits))
                            {
                                return __a.unrestricted();
                            }
                            long __maxCandidate = __maxPosA * __maxPosB;
                            if (multiplicationOverflows(__minPosA, __minPosB, __bits))
                            {
                                return __a.unrestricted();
                            }
                            long __minCandidate = __minPosA * __minPosB;
                            __newLowerBound = Math.min(__newLowerBound, __minCandidate);
                            __newUpperBound = Math.max(__newUpperBound, __maxCandidate);
                        }
                        if (__b.canBeNegative())
                        {
                            if (multiplicationOverflows(__minPosA, __maxNegB, __bits))
                            {
                                return __a.unrestricted();
                            }
                            long __maxCandidate = __minPosA * __maxNegB;
                            if (multiplicationOverflows(__maxPosA, __minNegB, __bits))
                            {
                                return __a.unrestricted();
                            }
                            long __minCandidate = __maxPosA * __minNegB;
                            __newLowerBound = Math.min(__newLowerBound, __minCandidate);
                            __newUpperBound = Math.max(__newUpperBound, __maxCandidate);
                        }
                    }
                    if (__a.canBeNegative())
                    {
                        if (__b.canBePositive())
                        {
                            if (multiplicationOverflows(__maxNegA, __minPosB, __bits))
                            {
                                return __a.unrestricted();
                            }
                            long __maxCandidate = __maxNegA * __minPosB;
                            if (multiplicationOverflows(__minNegA, __maxPosB, __bits))
                            {
                                return __a.unrestricted();
                            }
                            long __minCandidate = __minNegA * __maxPosB;
                            __newLowerBound = Math.min(__newLowerBound, __minCandidate);
                            __newUpperBound = Math.max(__newUpperBound, __maxCandidate);
                        }
                        if (__b.canBeNegative())
                        {
                            if (multiplicationOverflows(__minNegA, __minNegB, __bits))
                            {
                                return __a.unrestricted();
                            }
                            long __maxCandidate = __minNegA * __minNegB;
                            if (multiplicationOverflows(__maxNegA, __maxNegB, __bits))
                            {
                                return __a.unrestricted();
                            }
                            long __minCandidate = __maxNegA * __maxNegB;
                            __newLowerBound = Math.min(__newLowerBound, __minCandidate);
                            __newUpperBound = Math.max(__newUpperBound, __maxCandidate);
                        }
                    }

                    return StampFactory.forIntegerWithMask(__bits, __newLowerBound, __newUpperBound, 0, __newUpMask);
                }
            }

            @Override
            public boolean isNeutral(Constant __value)
            {
                PrimitiveConstant __n = (PrimitiveConstant) __value;
                return __n.asLong() == 1;
            }
        },

        // @closure
        new BinaryOp.MulHigh(true, true)
        {
            @Override
            public Constant foldConstant(Constant __const1, Constant __const2)
            {
                PrimitiveConstant __a = (PrimitiveConstant) __const1;
                PrimitiveConstant __b = (PrimitiveConstant) __const2;
                return JavaConstant.forIntegerKind(__a.getJavaKind(), multiplyHigh(__a.asLong(), __b.asLong(), __a.getJavaKind()));
            }

            @Override
            public Stamp foldStamp(Stamp __stamp1, Stamp __stamp2)
            {
                if (__stamp1.isEmpty())
                {
                    return __stamp1;
                }
                if (__stamp2.isEmpty())
                {
                    return __stamp2;
                }
                IntegerStamp __a = (IntegerStamp) __stamp1;
                IntegerStamp __b = (IntegerStamp) __stamp2;
                JavaKind __javaKind = __a.getStackKind();

                if (__a.isEmpty() || __b.isEmpty())
                {
                    return __a.empty();
                }
                else if (__a.isUnrestricted() || __b.isUnrestricted())
                {
                    return __a.unrestricted();
                }

                long[] __xExtremes = { __a.lowerBound(), __a.upperBound() };
                long[] __yExtremes = { __b.lowerBound(), __b.upperBound() };
                long __min = Long.MAX_VALUE;
                long __max = Long.MIN_VALUE;
                for (long __x : __xExtremes)
                {
                    for (long __y : __yExtremes)
                    {
                        long __result = multiplyHigh(__x, __y, __javaKind);
                        __min = Math.min(__min, __result);
                        __max = Math.max(__max, __result);
                    }
                }
                return StampFactory.forInteger(__javaKind, __min, __max);
            }

            @Override
            public boolean isNeutral(Constant __value)
            {
                return false;
            }

            private long multiplyHigh(long __x, long __y, JavaKind __javaKind)
            {
                if (__javaKind == JavaKind.Int)
                {
                    return (__x * __y) >> 32;
                }
                else
                {
                    long __x0 = __x & 0xFFFFFFFFL;
                    long __x1 = __x >> 32;

                    long __y0 = __y & 0xFFFFFFFFL;
                    long __y1 = __y >> 32;

                    long __z0 = __x0 * __y0;
                    long __t = __x1 * __y0 + (__z0 >>> 32);
                    long __z1 = __t & 0xFFFFFFFFL;
                    long __z2 = __t >> 32;
                    __z1 += __x0 * __y1;

                    return __x1 * __y1 + __z2 + (__z1 >> 32);
                }
            }
        },

        // @closure
        new BinaryOp.UMulHigh(true, true)
        {
            @Override
            public Constant foldConstant(Constant __const1, Constant __const2)
            {
                PrimitiveConstant __a = (PrimitiveConstant) __const1;
                PrimitiveConstant __b = (PrimitiveConstant) __const2;
                return JavaConstant.forIntegerKind(__a.getJavaKind(), multiplyHighUnsigned(__a.asLong(), __b.asLong(), __a.getJavaKind()));
            }

            @Override
            public Stamp foldStamp(Stamp __stamp1, Stamp __stamp2)
            {
                if (__stamp1.isEmpty())
                {
                    return __stamp1;
                }
                if (__stamp2.isEmpty())
                {
                    return __stamp2;
                }
                IntegerStamp __a = (IntegerStamp) __stamp1;
                IntegerStamp __b = (IntegerStamp) __stamp2;
                JavaKind __javaKind = __a.getStackKind();

                if (__a.isEmpty() || __b.isEmpty())
                {
                    return __a.empty();
                }
                else if (__a.isUnrestricted() || __b.isUnrestricted())
                {
                    return __a.unrestricted();
                }

                // Note that the minima and maxima are calculated using signed min/max
                // functions, while the values themselves are unsigned.
                long[] __xExtremes = getUnsignedExtremes(__a);
                long[] __yExtremes = getUnsignedExtremes(__b);
                long __min = Long.MAX_VALUE;
                long __max = Long.MIN_VALUE;
                for (long __x : __xExtremes)
                {
                    for (long __y : __yExtremes)
                    {
                        long __result = multiplyHighUnsigned(__x, __y, __javaKind);
                        __min = Math.min(__min, __result);
                        __max = Math.max(__max, __result);
                    }
                }

                // if min is negative, then the value can reach into the unsigned range
                if (__min == __max || __min >= 0)
                {
                    return StampFactory.forInteger(__javaKind, __min, __max);
                }
                else
                {
                    return StampFactory.forKind(__javaKind);
                }
            }

            @Override
            public boolean isNeutral(Constant __value)
            {
                return false;
            }

            private long[] getUnsignedExtremes(IntegerStamp __stamp)
            {
                if (__stamp.lowerBound() < 0 && __stamp.upperBound() >= 0)
                {
                    /*
                     * If -1 and 0 are both in the signed range, then we can't say anything
                     * about the unsigned range, so we have to return [0, MAX_UNSIGNED].
                     */
                    return new long[] { 0, -1L };
                }
                else
                {
                    return new long[] { __stamp.lowerBound(), __stamp.upperBound() };
                }
            }

            private long multiplyHighUnsigned(long __x, long __y, JavaKind __javaKind)
            {
                if (__javaKind == JavaKind.Int)
                {
                    long __xl = __x & 0xFFFFFFFFL;
                    long __yl = __y & 0xFFFFFFFFL;
                    long __r = __xl * __yl;
                    return (int) (__r >>> 32);
                }
                else
                {
                    long __x0 = __x & 0xFFFFFFFFL;
                    long __x1 = __x >>> 32;

                    long __y0 = __y & 0xFFFFFFFFL;
                    long __y1 = __y >>> 32;

                    long __z0 = __x0 * __y0;
                    long __t = __x1 * __y0 + (__z0 >>> 32);
                    long __z1 = __t & 0xFFFFFFFFL;
                    long __z2 = __t >>> 32;
                    __z1 += __x0 * __y1;

                    return __x1 * __y1 + __z2 + (__z1 >>> 32);
                }
            }
        },

        // @closure
        new BinaryOp.Div(true, false)
        {
            @Override
            public Constant foldConstant(Constant __const1, Constant __const2)
            {
                PrimitiveConstant __a = (PrimitiveConstant) __const1;
                PrimitiveConstant __b = (PrimitiveConstant) __const2;
                if (__b.asLong() == 0)
                {
                    return null;
                }
                return JavaConstant.forIntegerKind(__a.getJavaKind(), __a.asLong() / __b.asLong());
            }

            @Override
            public Stamp foldStamp(Stamp __stamp1, Stamp __stamp2)
            {
                if (__stamp1.isEmpty())
                {
                    return __stamp1;
                }
                if (__stamp2.isEmpty())
                {
                    return __stamp2;
                }
                IntegerStamp __a = (IntegerStamp) __stamp1;
                IntegerStamp __b = (IntegerStamp) __stamp2;
                if (__a.lowerBound == __a.upperBound && __b.lowerBound == __b.upperBound && __b.lowerBound != 0)
                {
                    long __value = CodeUtil.convert(__a.lowerBound() / __b.lowerBound(), __a.getBits(), false);
                    return StampFactory.forInteger(__a.getBits(), __value, __value);
                }
                else if (__b.isStrictlyPositive())
                {
                    long __newLowerBound = __a.lowerBound() < 0 ? __a.lowerBound() / __b.lowerBound() : __a.lowerBound() / __b.upperBound();
                    long __newUpperBound = __a.upperBound() < 0 ? __a.upperBound() / __b.upperBound() : __a.upperBound() / __b.lowerBound();
                    return StampFactory.forInteger(__a.getBits(), __newLowerBound, __newUpperBound);
                }
                else
                {
                    return __a.unrestricted();
                }
            }

            @Override
            public boolean isNeutral(Constant __value)
            {
                PrimitiveConstant __n = (PrimitiveConstant) __value;
                return __n.asLong() == 1;
            }
        },

        // @closure
        new BinaryOp.Rem(false, false)
        {
            @Override
            public Constant foldConstant(Constant __const1, Constant __const2)
            {
                PrimitiveConstant __a = (PrimitiveConstant) __const1;
                PrimitiveConstant __b = (PrimitiveConstant) __const2;
                if (__b.asLong() == 0)
                {
                    return null;
                }
                return JavaConstant.forIntegerKind(__a.getJavaKind(), __a.asLong() % __b.asLong());
            }

            @Override
            public Stamp foldStamp(Stamp __stamp1, Stamp __stamp2)
            {
                if (__stamp1.isEmpty())
                {
                    return __stamp1;
                }
                if (__stamp2.isEmpty())
                {
                    return __stamp2;
                }
                IntegerStamp __a = (IntegerStamp) __stamp1;
                IntegerStamp __b = (IntegerStamp) __stamp2;

                if (__a.lowerBound == __a.upperBound && __b.lowerBound == __b.upperBound && __b.lowerBound != 0)
                {
                    long __value = CodeUtil.convert(__a.lowerBound() % __b.lowerBound(), __a.getBits(), false);
                    return StampFactory.forInteger(__a.getBits(), __value, __value);
                }

                // zero is always possible
                long __newLowerBound = Math.min(__a.lowerBound(), 0);
                long __newUpperBound = Math.max(__a.upperBound(), 0);

                // the maximum absolute value of the result, derived from b
                long __magnitude;
                if (__b.lowerBound() == CodeUtil.minValue(__b.getBits()))
                {
                    // Math.abs(...) - 1 does not work in a case
                    __magnitude = CodeUtil.maxValue(__b.getBits());
                }
                else
                {
                    __magnitude = Math.max(Math.abs(__b.lowerBound()), Math.abs(__b.upperBound())) - 1;
                }
                __newLowerBound = Math.max(__newLowerBound, -__magnitude);
                __newUpperBound = Math.min(__newUpperBound, __magnitude);

                return StampFactory.forInteger(__a.getBits(), __newLowerBound, __newUpperBound);
            }
        },

        // @closure
        new UnaryOp.Not()
        {
            @Override
            public Constant foldConstant(Constant __c)
            {
                PrimitiveConstant __value = (PrimitiveConstant) __c;
                return JavaConstant.forIntegerKind(__value.getJavaKind(), ~__value.asLong());
            }

            @Override
            public Stamp foldStamp(Stamp __stamp)
            {
                if (__stamp.isEmpty())
                {
                    return __stamp;
                }
                IntegerStamp __integerStamp = (IntegerStamp) __stamp;
                int __bits = __integerStamp.getBits();
                long __defaultMask = CodeUtil.mask(__bits);
                return new IntegerStamp(__bits, ~__integerStamp.upperBound(), ~__integerStamp.lowerBound(), (~__integerStamp.upMask()) & __defaultMask, (~__integerStamp.downMask()) & __defaultMask);
            }
        },

        // @closure
        new BinaryOp.And(true, true)
        {
            @Override
            public Constant foldConstant(Constant __const1, Constant __const2)
            {
                PrimitiveConstant __a = (PrimitiveConstant) __const1;
                PrimitiveConstant __b = (PrimitiveConstant) __const2;
                return JavaConstant.forIntegerKind(__a.getJavaKind(), __a.asLong() & __b.asLong());
            }

            @Override
            public Stamp foldStamp(Stamp __stamp1, Stamp __stamp2)
            {
                if (__stamp1.isEmpty())
                {
                    return __stamp1;
                }
                if (__stamp2.isEmpty())
                {
                    return __stamp2;
                }
                IntegerStamp __a = (IntegerStamp) __stamp1;
                IntegerStamp __b = (IntegerStamp) __stamp2;
                return stampForMask(__a.getBits(), __a.downMask() & __b.downMask(), __a.upMask() & __b.upMask());
            }

            @Override
            public boolean isNeutral(Constant __value)
            {
                PrimitiveConstant __n = (PrimitiveConstant) __value;
                int __bits = __n.getJavaKind().getBitCount();
                long __mask = CodeUtil.mask(__bits);
                return (__n.asLong() & __mask) == __mask;
            }
        },

        // @closure
        new BinaryOp.Or(true, true)
        {
            @Override
            public Constant foldConstant(Constant __const1, Constant __const2)
            {
                PrimitiveConstant __a = (PrimitiveConstant) __const1;
                PrimitiveConstant __b = (PrimitiveConstant) __const2;
                return JavaConstant.forIntegerKind(__a.getJavaKind(), __a.asLong() | __b.asLong());
            }

            @Override
            public Stamp foldStamp(Stamp __stamp1, Stamp __stamp2)
            {
                if (__stamp1.isEmpty())
                {
                    return __stamp1;
                }
                if (__stamp2.isEmpty())
                {
                    return __stamp2;
                }
                IntegerStamp __a = (IntegerStamp) __stamp1;
                IntegerStamp __b = (IntegerStamp) __stamp2;
                return stampForMask(__a.getBits(), __a.downMask() | __b.downMask(), __a.upMask() | __b.upMask());
            }

            @Override
            public boolean isNeutral(Constant __value)
            {
                PrimitiveConstant __n = (PrimitiveConstant) __value;
                return __n.asLong() == 0;
            }
        },

        // @closure
        new BinaryOp.Xor(true, true)
        {
            @Override
            public Constant foldConstant(Constant __const1, Constant __const2)
            {
                PrimitiveConstant __a = (PrimitiveConstant) __const1;
                PrimitiveConstant __b = (PrimitiveConstant) __const2;
                return JavaConstant.forIntegerKind(__a.getJavaKind(), __a.asLong() ^ __b.asLong());
            }

            @Override
            public Stamp foldStamp(Stamp __stamp1, Stamp __stamp2)
            {
                if (__stamp1.isEmpty())
                {
                    return __stamp1;
                }
                if (__stamp2.isEmpty())
                {
                    return __stamp2;
                }
                IntegerStamp __a = (IntegerStamp) __stamp1;
                IntegerStamp __b = (IntegerStamp) __stamp2;

                long __variableBits = (__a.downMask() ^ __a.upMask()) | (__b.downMask() ^ __b.upMask());
                long __newDownMask = (__a.downMask() ^ __b.downMask()) & ~__variableBits;
                long __newUpMask = (__a.downMask() ^ __b.downMask()) | __variableBits;
                return stampForMask(__a.getBits(), __newDownMask, __newUpMask);
            }

            @Override
            public boolean isNeutral(Constant __value)
            {
                PrimitiveConstant __n = (PrimitiveConstant) __value;
                return __n.asLong() == 0;
            }

            @Override
            public Constant getZero(Stamp __s)
            {
                IntegerStamp __stamp = (IntegerStamp) __s;
                return JavaConstant.forPrimitiveInt(__stamp.getBits(), 0);
            }
        },

        // @closure
        new ShiftOp.Shl()
        {
            @Override
            public Constant foldConstant(Constant __value, int __amount)
            {
                PrimitiveConstant __c = (PrimitiveConstant) __value;
                switch (__c.getJavaKind())
                {
                    case Int:
                        return JavaConstant.forInt(__c.asInt() << __amount);
                    case Long:
                        return JavaConstant.forLong(__c.asLong() << __amount);
                    default:
                        throw GraalError.shouldNotReachHere();
                }
            }

            @Override
            public Stamp foldStamp(Stamp __stamp, IntegerStamp __shift)
            {
                IntegerStamp __value = (IntegerStamp) __stamp;
                int __bits = __value.getBits();
                if (__value.isEmpty())
                {
                    return __value;
                }
                else if (__shift.isEmpty())
                {
                    return StampFactory.forInteger(__bits).empty();
                }
                else if (__value.upMask() == 0)
                {
                    return __value;
                }

                int __shiftMask = getShiftAmountMask(__stamp);
                int __shiftBits = Integer.bitCount(__shiftMask);
                if (__shift.lowerBound() == __shift.upperBound())
                {
                    int __shiftAmount = (int) (__shift.lowerBound() & __shiftMask);
                    if (__shiftAmount == 0)
                    {
                        return __value;
                    }
                    // the mask of bits that will be lost or shifted into the sign bit
                    long __removedBits = -1L << (__bits - __shiftAmount - 1);
                    if ((__value.lowerBound() & __removedBits) == 0 && (__value.upperBound() & __removedBits) == 0)
                    {
                        // use a better stamp if neither lower nor upper bound can lose bits
                        return new IntegerStamp(__bits, __value.lowerBound() << __shiftAmount, __value.upperBound() << __shiftAmount, __value.downMask() << __shiftAmount, __value.upMask() << __shiftAmount);
                    }
                }
                if ((__shift.lowerBound() >>> __shiftBits) == (__shift.upperBound() >>> __shiftBits))
                {
                    long __defaultMask = CodeUtil.mask(__bits);
                    long __downMask = __defaultMask;
                    long __upMask = 0;
                    for (long __i = __shift.lowerBound(); __i <= __shift.upperBound(); __i++)
                    {
                        if (__shift.contains(__i))
                        {
                            __downMask &= __value.downMask() << (__i & __shiftMask);
                            __upMask |= __value.upMask() << (__i & __shiftMask);
                        }
                    }
                    return IntegerStamp.stampForMask(__bits, __downMask, __upMask & __defaultMask);
                }
                return __value.unrestricted();
            }

            @Override
            public int getShiftAmountMask(Stamp __s)
            {
                IntegerStamp __stamp = (IntegerStamp) __s;
                return __stamp.getBits() - 1;
            }
        },

        // @closure
        new ShiftOp.Shr()
        {
            @Override
            public Constant foldConstant(Constant __value, int __amount)
            {
                PrimitiveConstant __c = (PrimitiveConstant) __value;
                switch (__c.getJavaKind())
                {
                    case Int:
                        return JavaConstant.forInt(__c.asInt() >> __amount);
                    case Long:
                        return JavaConstant.forLong(__c.asLong() >> __amount);
                    default:
                        throw GraalError.shouldNotReachHere();
                }
            }

            @Override
            public Stamp foldStamp(Stamp __stamp, IntegerStamp __shift)
            {
                IntegerStamp __value = (IntegerStamp) __stamp;
                int __bits = __value.getBits();
                if (__value.isEmpty())
                {
                    return __value;
                }
                else if (__shift.isEmpty())
                {
                    return StampFactory.forInteger(__bits).empty();
                }
                else if (__shift.lowerBound() == __shift.upperBound())
                {
                    long __shiftCount = __shift.lowerBound() & getShiftAmountMask(__stamp);
                    if (__shiftCount == 0)
                    {
                        return __stamp;
                    }

                    int __extraBits = 64 - __bits;
                    long __defaultMask = CodeUtil.mask(__bits);
                    // shifting back and forth performs sign extension
                    long __downMask = (__value.downMask() << __extraBits) >> (__shiftCount + __extraBits) & __defaultMask;
                    long __upMask = (__value.upMask() << __extraBits) >> (__shiftCount + __extraBits) & __defaultMask;
                    return new IntegerStamp(__bits, __value.lowerBound() >> __shiftCount, __value.upperBound() >> __shiftCount, __downMask, __upMask);
                }
                long __mask = IntegerStamp.upMaskFor(__bits, __value.lowerBound(), __value.upperBound());
                return IntegerStamp.stampForMask(__bits, 0, __mask);
            }

            @Override
            public int getShiftAmountMask(Stamp __s)
            {
                IntegerStamp __stamp = (IntegerStamp) __s;
                return __stamp.getBits() - 1;
            }
        },

        // @closure
        new ShiftOp.UShr()
        {
            @Override
            public Constant foldConstant(Constant __value, int __amount)
            {
                PrimitiveConstant __c = (PrimitiveConstant) __value;
                switch (__c.getJavaKind())
                {
                    case Int:
                        return JavaConstant.forInt(__c.asInt() >>> __amount);
                    case Long:
                        return JavaConstant.forLong(__c.asLong() >>> __amount);
                    default:
                        throw GraalError.shouldNotReachHere();
                }
            }

            @Override
            public Stamp foldStamp(Stamp __stamp, IntegerStamp __shift)
            {
                IntegerStamp __value = (IntegerStamp) __stamp;
                int __bits = __value.getBits();
                if (__value.isEmpty())
                {
                    return __value;
                }
                else if (__shift.isEmpty())
                {
                    return StampFactory.forInteger(__bits).empty();
                }

                if (__shift.lowerBound() == __shift.upperBound())
                {
                    long __shiftCount = __shift.lowerBound() & getShiftAmountMask(__stamp);
                    if (__shiftCount == 0)
                    {
                        return __stamp;
                    }

                    long __downMask = __value.downMask() >>> __shiftCount;
                    long __upMask = __value.upMask() >>> __shiftCount;
                    if (__value.lowerBound() < 0)
                    {
                        return new IntegerStamp(__bits, __downMask, __upMask, __downMask, __upMask);
                    }
                    else
                    {
                        return new IntegerStamp(__bits, __value.lowerBound() >>> __shiftCount, __value.upperBound() >>> __shiftCount, __downMask, __upMask);
                    }
                }
                long __mask = IntegerStamp.upMaskFor(__bits, __value.lowerBound(), __value.upperBound());
                return IntegerStamp.stampForMask(__bits, 0, __mask);
            }

            @Override
            public int getShiftAmountMask(Stamp __s)
            {
                IntegerStamp __stamp = (IntegerStamp) __s;
                return __stamp.getBits() - 1;
            }
        },

        // @closure
        new UnaryOp.Abs()
        {
            @Override
            public Constant foldConstant(Constant __value)
            {
                PrimitiveConstant __c = (PrimitiveConstant) __value;
                return JavaConstant.forIntegerKind(__c.getJavaKind(), Math.abs(__c.asLong()));
            }

            @Override
            public Stamp foldStamp(Stamp __input)
            {
                if (__input.isEmpty())
                {
                    return __input;
                }
                IntegerStamp __stamp = (IntegerStamp) __input;
                int __bits = __stamp.getBits();
                if (__stamp.lowerBound == __stamp.upperBound)
                {
                    long __value = CodeUtil.convert(Math.abs(__stamp.lowerBound()), __stamp.getBits(), false);
                    return StampFactory.forInteger(__stamp.getBits(), __value, __value);
                }
                if (__stamp.lowerBound() == CodeUtil.minValue(__bits))
                {
                    return __input.unrestricted();
                }
                else
                {
                    long __limit = Math.max(-__stamp.lowerBound(), __stamp.upperBound());
                    return StampFactory.forInteger(__bits, 0, __limit);
                }
            }
        },

        null,

        // @closure
        new IntegerConvertOp.ZeroExtend()
        {
            @Override
            public Constant foldConstant(int __inputBits, int __resultBits, Constant __c)
            {
                PrimitiveConstant __value = (PrimitiveConstant) __c;
                return JavaConstant.forPrimitiveInt(__resultBits, CodeUtil.zeroExtend(__value.asLong(), __inputBits));
            }

            @Override
            public Stamp foldStamp(int __inputBits, int __resultBits, Stamp __input)
            {
                if (__input.isEmpty())
                {
                    return StampFactory.forInteger(__resultBits).empty();
                }
                IntegerStamp __stamp = (IntegerStamp) __input;

                if (__inputBits == __resultBits)
                {
                    return __input;
                }

                if (__input.isEmpty())
                {
                    return StampFactory.forInteger(__resultBits).empty();
                }

                long __downMask = CodeUtil.zeroExtend(__stamp.downMask(), __inputBits);
                long __upMask = CodeUtil.zeroExtend(__stamp.upMask(), __inputBits);
                long __lowerBound = __stamp.unsignedLowerBound();
                long __upperBound = __stamp.unsignedUpperBound();
                return IntegerStamp.create(__resultBits, __lowerBound, __upperBound, __downMask, __upMask);
            }

            @Override
            public Stamp invertStamp(int __inputBits, int __resultBits, Stamp __outStamp)
            {
                IntegerStamp __stamp = (IntegerStamp) __outStamp;
                if (__stamp.isEmpty())
                {
                    return StampFactory.forInteger(__inputBits).empty();
                }
                return StampFactory.forUnsignedInteger(__inputBits, __stamp.lowerBound(), __stamp.upperBound(), __stamp.downMask(), __stamp.upMask());
            }
        },

        // @closure
        new IntegerConvertOp.SignExtend()
        {
            @Override
            public Constant foldConstant(int __inputBits, int __resultBits, Constant __c)
            {
                PrimitiveConstant __value = (PrimitiveConstant) __c;
                return JavaConstant.forPrimitiveInt(__resultBits, CodeUtil.signExtend(__value.asLong(), __inputBits));
            }

            @Override
            public Stamp foldStamp(int __inputBits, int __resultBits, Stamp __input)
            {
                if (__input.isEmpty())
                {
                    return StampFactory.forInteger(__resultBits).empty();
                }
                IntegerStamp __stamp = (IntegerStamp) __input;

                long __defaultMask = CodeUtil.mask(__resultBits);
                long __downMask = CodeUtil.signExtend(__stamp.downMask(), __inputBits) & __defaultMask;
                long __upMask = CodeUtil.signExtend(__stamp.upMask(), __inputBits) & __defaultMask;

                return new IntegerStamp(__resultBits, __stamp.lowerBound(), __stamp.upperBound(), __downMask, __upMask);
            }

            @Override
            public Stamp invertStamp(int __inputBits, int __resultBits, Stamp __outStamp)
            {
                if (__outStamp.isEmpty())
                {
                    return StampFactory.forInteger(__inputBits).empty();
                }
                IntegerStamp __stamp = (IntegerStamp) __outStamp;
                long __mask = CodeUtil.mask(__inputBits);
                return StampFactory.forIntegerWithMask(__inputBits, __stamp.lowerBound(), __stamp.upperBound(), __stamp.downMask() & __mask, __stamp.upMask() & __mask);
            }
        },

        // @closure
        new IntegerConvertOp.Narrow()
        {
            @Override
            public Constant foldConstant(int __inputBits, int __resultBits, Constant __c)
            {
                PrimitiveConstant __value = (PrimitiveConstant) __c;
                return JavaConstant.forPrimitiveInt(__resultBits, CodeUtil.narrow(__value.asLong(), __resultBits));
            }

            @Override
            public Stamp foldStamp(int __inputBits, int __resultBits, Stamp __input)
            {
                if (__input.isEmpty())
                {
                    return StampFactory.forInteger(__resultBits).empty();
                }
                IntegerStamp __stamp = (IntegerStamp) __input;
                if (__resultBits == __inputBits)
                {
                    return __stamp;
                }

                final long __upperBound;
                if (__stamp.lowerBound() < CodeUtil.minValue(__resultBits))
                {
                    __upperBound = CodeUtil.maxValue(__resultBits);
                }
                else
                {
                    __upperBound = saturate(__stamp.upperBound(), __resultBits);
                }
                final long __lowerBound;
                if (__stamp.upperBound() > CodeUtil.maxValue(__resultBits))
                {
                    __lowerBound = CodeUtil.minValue(__resultBits);
                }
                else
                {
                    __lowerBound = saturate(__stamp.lowerBound(), __resultBits);
                }

                long __defaultMask = CodeUtil.mask(__resultBits);
                long __newDownMask = __stamp.downMask() & __defaultMask;
                long __newUpMask = __stamp.upMask() & __defaultMask;
                long __newLowerBound = CodeUtil.signExtend((__lowerBound | __newDownMask) & __newUpMask, __resultBits);
                long __newUpperBound = CodeUtil.signExtend((__upperBound | __newDownMask) & __newUpMask, __resultBits);
                return new IntegerStamp(__resultBits, __newLowerBound, __newUpperBound, __newDownMask, __newUpMask);
            }
        },

        // @closure
        new FloatConvertOp(FloatConvert.I2F)
        {
            @Override
            public Constant foldConstant(Constant __c)
            {
                PrimitiveConstant __value = (PrimitiveConstant) __c;
                return JavaConstant.forFloat(__value.asInt());
            }

            @Override
            public Stamp foldStamp(Stamp __input)
            {
                if (__input.isEmpty())
                {
                    return StampFactory.empty(JavaKind.Float);
                }
                IntegerStamp __stamp = (IntegerStamp) __input;
                float __lowerBound = __stamp.lowerBound();
                float __upperBound = __stamp.upperBound();
                return StampFactory.forFloat(JavaKind.Float, __lowerBound, __upperBound, true);
            }
        },

        // @closure
        new FloatConvertOp(FloatConvert.L2F)
        {
            @Override
            public Constant foldConstant(Constant __c)
            {
                PrimitiveConstant __value = (PrimitiveConstant) __c;
                return JavaConstant.forFloat(__value.asLong());
            }

            @Override
            public Stamp foldStamp(Stamp __input)
            {
                if (__input.isEmpty())
                {
                    return StampFactory.empty(JavaKind.Float);
                }
                IntegerStamp __stamp = (IntegerStamp) __input;
                float __lowerBound = __stamp.lowerBound();
                float __upperBound = __stamp.upperBound();
                return StampFactory.forFloat(JavaKind.Float, __lowerBound, __upperBound, true);
            }
        },

        // @closure
        new FloatConvertOp(FloatConvert.I2D)
        {
            @Override
            public Constant foldConstant(Constant __c)
            {
                PrimitiveConstant __value = (PrimitiveConstant) __c;
                return JavaConstant.forDouble(__value.asInt());
            }

            @Override
            public Stamp foldStamp(Stamp __input)
            {
                if (__input.isEmpty())
                {
                    return StampFactory.empty(JavaKind.Double);
                }
                IntegerStamp __stamp = (IntegerStamp) __input;
                double __lowerBound = __stamp.lowerBound();
                double __upperBound = __stamp.upperBound();
                return StampFactory.forFloat(JavaKind.Double, __lowerBound, __upperBound, true);
            }
        },

        // @closure
        new FloatConvertOp(FloatConvert.L2D)
        {
            @Override
            public Constant foldConstant(Constant __c)
            {
                PrimitiveConstant __value = (PrimitiveConstant) __c;
                return JavaConstant.forDouble(__value.asLong());
            }

            @Override
            public Stamp foldStamp(Stamp __input)
            {
                if (__input.isEmpty())
                {
                    return StampFactory.empty(JavaKind.Double);
                }
                IntegerStamp __stamp = (IntegerStamp) __input;
                double __lowerBound = __stamp.lowerBound();
                double __upperBound = __stamp.upperBound();
                return StampFactory.forFloat(JavaKind.Double, __lowerBound, __upperBound, true);
            }
        }
    );
}
