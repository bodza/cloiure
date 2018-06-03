package giraaff.core.common.type;

import java.nio.ByteBuffer;
import java.util.function.DoubleBinaryOperator;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.PrimitiveConstant;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.meta.SerializableConstant;

import giraaff.core.common.LIRKind;
import giraaff.core.common.calc.FloatConvert;
import giraaff.core.common.spi.LIRKindTool;
import giraaff.core.common.type.ArithmeticOpTable.BinaryOp;
import giraaff.core.common.type.ArithmeticOpTable.FloatConvertOp;
import giraaff.core.common.type.ArithmeticOpTable.UnaryOp;
import giraaff.util.GraalError;

// @class FloatStamp
public final class FloatStamp extends PrimitiveStamp
{
    // @field
    private final double lowerBound;
    // @field
    private final double upperBound;
    // @field
    private final boolean nonNaN;

    // @cons
    protected FloatStamp(int __bits)
    {
        this(__bits, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false);
    }

    // @cons
    public FloatStamp(int __bits, double __lowerBound, double __upperBound, boolean __nonNaN)
    {
        super(__bits, OPS);
        this.lowerBound = __lowerBound;
        this.upperBound = __upperBound;
        this.nonNaN = __nonNaN;
    }

    @Override
    public Stamp unrestricted()
    {
        return new FloatStamp(getBits());
    }

    @Override
    public Stamp empty()
    {
        return new FloatStamp(getBits(), Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, true);
    }

    @Override
    public Stamp constant(Constant __c, MetaAccessProvider __meta)
    {
        JavaConstant __jc = (JavaConstant) __c;
        return StampFactory.forConstant(__jc);
    }

    @Override
    public SerializableConstant deserialize(ByteBuffer __buffer)
    {
        switch (getBits())
        {
            case 32:
                return JavaConstant.forFloat(__buffer.getFloat());
            case 64:
                return JavaConstant.forDouble(__buffer.getDouble());
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    @Override
    public boolean hasValues()
    {
        return lowerBound <= upperBound || !nonNaN;
    }

    @Override
    public JavaKind getStackKind()
    {
        if (getBits() > 32)
        {
            return JavaKind.Double;
        }
        else
        {
            return JavaKind.Float;
        }
    }

    @Override
    public LIRKind getLIRKind(LIRKindTool __tool)
    {
        return __tool.getFloatingKind(getBits());
    }

    @Override
    public ResolvedJavaType javaType(MetaAccessProvider __metaAccess)
    {
        switch (getBits())
        {
            case 32:
                return __metaAccess.lookupJavaType(Float.TYPE);
            case 64:
                return __metaAccess.lookupJavaType(Double.TYPE);
            default:
                throw GraalError.shouldNotReachHere();
        }
    }

    /**
     * The (inclusive) lower bound on the value described by this stamp.
     */
    public double lowerBound()
    {
        return lowerBound;
    }

    /**
     * The (inclusive) upper bound on the value described by this stamp.
     */
    public double upperBound()
    {
        return upperBound;
    }

    /**
     * Returns true if NaN is non included in the value described by this stamp.
     */
    public boolean isNonNaN()
    {
        return nonNaN;
    }

    /**
     * Returns true if this stamp represents the NaN value.
     */
    public boolean isNaN()
    {
        return Double.isNaN(lowerBound);
    }

    @Override
    public boolean isUnrestricted()
    {
        return lowerBound == Double.NEGATIVE_INFINITY && upperBound == Double.POSITIVE_INFINITY && !nonNaN;
    }

    public boolean contains(double __value)
    {
        if (Double.isNaN(__value))
        {
            return !nonNaN;
        }
        else
        {
            /*
             * Don't use Double.compare for checking the bounds as -0.0 isn't correctly tracked, so
             * the presence of 0.0 means -0.0 might also exist in the range.
             */
            return __value >= lowerBound && __value <= upperBound;
        }
    }

    private static double meetBounds(double __a, double __b, DoubleBinaryOperator __op)
    {
        if (Double.isNaN(__a))
        {
            return __b;
        }
        else if (Double.isNaN(__b))
        {
            return __a;
        }
        else
        {
            return __op.applyAsDouble(__a, __b);
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
            return this;
        }
        if (__otherStamp.isEmpty())
        {
            return __otherStamp;
        }
        FloatStamp __other = (FloatStamp) __otherStamp;
        double __meetUpperBound = meetBounds(upperBound, __other.upperBound, Math::max);
        double __meetLowerBound = meetBounds(lowerBound, __other.lowerBound, Math::min);
        boolean __meetNonNaN = nonNaN && __other.nonNaN;
        if (Double.compare(__meetLowerBound, lowerBound) == 0 && Double.compare(__meetUpperBound, upperBound) == 0 && __meetNonNaN == nonNaN)
        {
            return this;
        }
        else if (Double.compare(__meetLowerBound, __other.lowerBound) == 0 && Double.compare(__meetUpperBound, __other.upperBound) == 0 && __meetNonNaN == __other.nonNaN)
        {
            return __other;
        }
        else
        {
            return new FloatStamp(getBits(), __meetLowerBound, __meetUpperBound, __meetNonNaN);
        }
    }

    @Override
    public Stamp join(Stamp __otherStamp)
    {
        if (__otherStamp == this)
        {
            return this;
        }
        FloatStamp __other = (FloatStamp) __otherStamp;
        double __joinUpperBound = Math.min(upperBound, __other.upperBound);
        double __joinLowerBound = Math.max(lowerBound, __other.lowerBound);
        boolean __joinNonNaN = nonNaN || __other.nonNaN;
        if (Double.compare(__joinLowerBound, lowerBound) == 0 && Double.compare(__joinUpperBound, upperBound) == 0 && __joinNonNaN == nonNaN)
        {
            return this;
        }
        else if (Double.compare(__joinLowerBound, __other.lowerBound) == 0 && Double.compare(__joinUpperBound, __other.upperBound) == 0 && __joinNonNaN == __other.nonNaN)
        {
            return __other;
        }
        else
        {
            return new FloatStamp(getBits(), __joinLowerBound, __joinUpperBound, __joinNonNaN);
        }
    }

    @Override
    public int hashCode()
    {
        final int __prime = 31;
        int __result = 1;
        long __temp;
        __result = __prime * __result + super.hashCode();
        __temp = Double.doubleToLongBits(lowerBound);
        __result = __prime * __result + (int) (__temp ^ (__temp >>> 32));
        __result = __prime * __result + (nonNaN ? 1231 : 1237);
        __temp = Double.doubleToLongBits(upperBound);
        __result = __prime * __result + (int) (__temp ^ (__temp >>> 32));
        return __result;
    }

    @Override
    public boolean isCompatible(Stamp __stamp)
    {
        if (this == __stamp)
        {
            return true;
        }
        if (__stamp instanceof FloatStamp)
        {
            FloatStamp __other = (FloatStamp) __stamp;
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
            return __prim.getJavaKind().isNumericFloat();
        }
        return false;
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
        FloatStamp __other = (FloatStamp) __obj;
        if (Double.doubleToLongBits(lowerBound) != Double.doubleToLongBits(__other.lowerBound))
        {
            return false;
        }
        if (Double.doubleToLongBits(upperBound) != Double.doubleToLongBits(__other.upperBound))
        {
            return false;
        }
        if (nonNaN != __other.nonNaN)
        {
            return false;
        }
        return super.equals(__other);
    }

    @Override
    public JavaConstant asConstant()
    {
        if (isConstant())
        {
            switch (getBits())
            {
                case 32:
                    return JavaConstant.forFloat((float) lowerBound);
                case 64:
                    return JavaConstant.forDouble(lowerBound);
            }
        }
        return null;
    }

    private boolean isConstant()
    {
        /*
         * There are many forms of NaNs and any operations on them can silently convert them into
         * the canonical NaN.
         */
        return (Double.compare(lowerBound, upperBound) == 0 && nonNaN);
    }

    private static FloatStamp stampForConstant(Constant __constant)
    {
        FloatStamp __result;
        PrimitiveConstant __value = (PrimitiveConstant) __constant;
        switch (__value.getJavaKind())
        {
            case Float:
                if (Float.isNaN(__value.asFloat()))
                {
                    __result = new FloatStamp(32, Double.NaN, Double.NaN, false);
                }
                else
                {
                    __result = new FloatStamp(32, __value.asFloat(), __value.asFloat(), !Float.isNaN(__value.asFloat()));
                }
                break;
            case Double:
                if (Double.isNaN(__value.asDouble()))
                {
                    __result = new FloatStamp(64, Double.NaN, Double.NaN, false);
                }
                else
                {
                    __result = new FloatStamp(64, __value.asDouble(), __value.asDouble(), !Double.isNaN(__value.asDouble()));
                }
                break;
            default:
                throw GraalError.shouldNotReachHere();
        }
        if (__result.isConstant())
        {
            return __result;
        }
        return null;
    }

    private static Stamp maybeFoldConstant(UnaryOp<?> __op, FloatStamp __stamp)
    {
        if (__stamp.isConstant())
        {
            JavaConstant __constant = __stamp.asConstant();
            Constant __folded = __op.foldConstant(__constant);
            if (__folded != null)
            {
                return FloatStamp.stampForConstant(__folded);
            }
        }
        return null;
    }

    private static Stamp maybeFoldConstant(BinaryOp<?> __op, FloatStamp __stamp1, FloatStamp __stamp2)
    {
        if (__stamp1.isConstant() && __stamp2.isConstant())
        {
            JavaConstant __constant1 = __stamp1.asConstant();
            JavaConstant __constant2 = __stamp2.asConstant();
            Constant __folded = __op.foldConstant(__constant1, __constant2);
            if (__folded != null)
            {
                FloatStamp __stamp = stampForConstant(__folded);
                if (__stamp != null && __stamp.isConstant())
                {
                    return __stamp;
                }
            }
        }
        return null;
    }

    // @def
    public static final ArithmeticOpTable OPS = new ArithmeticOpTable(
        // @closure
        new UnaryOp.Neg()
        {
            @Override
            public Constant foldConstant(Constant __c)
            {
                PrimitiveConstant __value = (PrimitiveConstant) __c;
                switch (__value.getJavaKind())
                {
                    case Float:
                        return JavaConstant.forFloat(-__value.asFloat());
                    case Double:
                        return JavaConstant.forDouble(-__value.asDouble());
                    default:
                        throw GraalError.shouldNotReachHere();
                }
            }

            @Override
            public Stamp foldStamp(Stamp __s)
            {
                if (__s.isEmpty())
                {
                    return __s;
                }
                FloatStamp __stamp = (FloatStamp) __s;
                Stamp __folded = maybeFoldConstant(this, __stamp);
                if (__folded != null)
                {
                    return __folded;
                }
                return new FloatStamp(__stamp.getBits(), -__stamp.upperBound(), -__stamp.lowerBound(), __stamp.isNonNaN());
            }
        },

        // @closure
        new BinaryOp.Add(false, true)
        {
            @Override
            public Constant foldConstant(Constant __const1, Constant __const2)
            {
                PrimitiveConstant __a = (PrimitiveConstant) __const1;
                PrimitiveConstant __b = (PrimitiveConstant) __const2;
                switch (__a.getJavaKind())
                {
                    case Float:
                        return JavaConstant.forFloat(__a.asFloat() + __b.asFloat());
                    case Double:
                        return JavaConstant.forDouble(__a.asDouble() + __b.asDouble());
                    default:
                        throw GraalError.shouldNotReachHere();
                }
            }

            @Override
            public Stamp foldStamp(Stamp __s1, Stamp __s2)
            {
                if (__s1.isEmpty())
                {
                    return __s1;
                }
                if (__s2.isEmpty())
                {
                    return __s2;
                }
                FloatStamp __stamp1 = (FloatStamp) __s1;
                FloatStamp __stamp2 = (FloatStamp) __s2;
                Stamp __folded = maybeFoldConstant(this, __stamp1, __stamp2);
                if (__folded != null)
                {
                    return __folded;
                }
                return __stamp1.unrestricted();
            }

            @Override
            public boolean isNeutral(Constant __value)
            {
                PrimitiveConstant __n = (PrimitiveConstant) __value;
                switch (__n.getJavaKind())
                {
                    case Float:
                        return Float.compare(__n.asFloat(), -0.0f) == 0;
                    case Double:
                        return Double.compare(__n.asDouble(), -0.0) == 0;
                    default:
                        throw GraalError.shouldNotReachHere();
                }
            }
        },

        // @closure
        new BinaryOp.Sub(false, false)
        {
            @Override
            public Constant foldConstant(Constant __const1, Constant __const2)
            {
                PrimitiveConstant __a = (PrimitiveConstant) __const1;
                PrimitiveConstant __b = (PrimitiveConstant) __const2;
                switch (__a.getJavaKind())
                {
                    case Float:
                        return JavaConstant.forFloat(__a.asFloat() - __b.asFloat());
                    case Double:
                        return JavaConstant.forDouble(__a.asDouble() - __b.asDouble());
                    default:
                        throw GraalError.shouldNotReachHere();
                }
            }

            @Override
            public Stamp foldStamp(Stamp __s1, Stamp __s2)
            {
                if (__s1.isEmpty())
                {
                    return __s1;
                }
                if (__s2.isEmpty())
                {
                    return __s2;
                }
                FloatStamp __stamp1 = (FloatStamp) __s1;
                FloatStamp __stamp2 = (FloatStamp) __s2;
                Stamp __folded = maybeFoldConstant(this, __stamp1, __stamp2);
                if (__folded != null)
                {
                    return __folded;
                }
                return __stamp1.unrestricted();
            }

            @Override
            public boolean isNeutral(Constant __value)
            {
                PrimitiveConstant __n = (PrimitiveConstant) __value;
                switch (__n.getJavaKind())
                {
                    case Float:
                        return Float.compare(__n.asFloat(), 0.0f) == 0;
                    case Double:
                        return Double.compare(__n.asDouble(), 0.0) == 0;
                    default:
                        throw GraalError.shouldNotReachHere();
                }
            }
        },

        // @closure
        new BinaryOp.Mul(false, true)
        {
            @Override
            public Constant foldConstant(Constant __const1, Constant __const2)
            {
                PrimitiveConstant __a = (PrimitiveConstant) __const1;
                PrimitiveConstant __b = (PrimitiveConstant) __const2;
                switch (__a.getJavaKind())
                {
                    case Float:
                        return JavaConstant.forFloat(__a.asFloat() * __b.asFloat());
                    case Double:
                        return JavaConstant.forDouble(__a.asDouble() * __b.asDouble());
                    default:
                        throw GraalError.shouldNotReachHere();
                }
            }

            @Override
            public Stamp foldStamp(Stamp __s1, Stamp __s2)
            {
                if (__s1.isEmpty())
                {
                    return __s1;
                }
                if (__s2.isEmpty())
                {
                    return __s2;
                }
                FloatStamp __stamp1 = (FloatStamp) __s1;
                FloatStamp __stamp2 = (FloatStamp) __s2;
                Stamp __folded = maybeFoldConstant(this, __stamp1, __stamp2);
                if (__folded != null)
                {
                    return __folded;
                }
                return __stamp1.unrestricted();
            }

            @Override
            public boolean isNeutral(Constant __value)
            {
                PrimitiveConstant __n = (PrimitiveConstant) __value;
                switch (__n.getJavaKind())
                {
                    case Float:
                        return Float.compare(__n.asFloat(), 1.0f) == 0;
                    case Double:
                        return Double.compare(__n.asDouble(), 1.0) == 0;
                    default:
                        throw GraalError.shouldNotReachHere();
                }
            }
        },

        null, null,

        // @closure
        new BinaryOp.Div(false, false)
        {
            @Override
            public Constant foldConstant(Constant __const1, Constant __const2)
            {
                PrimitiveConstant __a = (PrimitiveConstant) __const1;
                PrimitiveConstant __b = (PrimitiveConstant) __const2;
                switch (__a.getJavaKind())
                {
                    case Float:
                    {
                        float __floatDivisor = __b.asFloat();
                        return (__floatDivisor == 0) ? null : JavaConstant.forFloat(__a.asFloat() / __floatDivisor);
                    }
                    case Double:
                    {
                        double __doubleDivisor = __b.asDouble();
                        return (__doubleDivisor == 0) ? null : JavaConstant.forDouble(__a.asDouble() / __doubleDivisor);
                    }
                    default:
                        throw GraalError.shouldNotReachHere();
                }
            }

            @Override
            public Stamp foldStamp(Stamp __s1, Stamp __s2)
            {
                if (__s1.isEmpty())
                {
                    return __s1;
                }
                if (__s2.isEmpty())
                {
                    return __s2;
                }
                FloatStamp __stamp1 = (FloatStamp) __s1;
                FloatStamp __stamp2 = (FloatStamp) __s2;
                Stamp __folded = maybeFoldConstant(this, __stamp1, __stamp2);
                if (__folded != null)
                {
                    return __folded;
                }
                return __stamp1.unrestricted();
            }

            @Override
            public boolean isNeutral(Constant __value)
            {
                PrimitiveConstant __n = (PrimitiveConstant) __value;
                switch (__n.getJavaKind())
                {
                    case Float:
                        return Float.compare(__n.asFloat(), 1.0f) == 0;
                    case Double:
                        return Double.compare(__n.asDouble(), 1.0) == 0;
                    default:
                        throw GraalError.shouldNotReachHere();
                }
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
                switch (__a.getJavaKind())
                {
                    case Float:
                        return JavaConstant.forFloat(__a.asFloat() % __b.asFloat());
                    case Double:
                        return JavaConstant.forDouble(__a.asDouble() % __b.asDouble());
                    default:
                        throw GraalError.shouldNotReachHere();
                }
            }

            @Override
            public Stamp foldStamp(Stamp __s1, Stamp __s2)
            {
                if (__s1.isEmpty())
                {
                    return __s1;
                }
                if (__s2.isEmpty())
                {
                    return __s2;
                }
                FloatStamp __stamp1 = (FloatStamp) __s1;
                FloatStamp __stamp2 = (FloatStamp) __s2;
                Stamp __folded = maybeFoldConstant(this, __stamp1, __stamp2);
                if (__folded != null)
                {
                    return __folded;
                }
                return __stamp1.unrestricted();
            }
        },

        // @closure
        new UnaryOp.Not()
        {
            @Override
            public Constant foldConstant(Constant __c)
            {
                PrimitiveConstant __value = (PrimitiveConstant) __c;
                switch (__value.getJavaKind())
                {
                    case Float:
                    {
                        int __f = Float.floatToRawIntBits(__value.asFloat());
                        return JavaConstant.forFloat(Float.intBitsToFloat(~__f));
                    }
                    case Double:
                    {
                        long __d = Double.doubleToRawLongBits(__value.asDouble());
                        return JavaConstant.forDouble(Double.longBitsToDouble(~__d));
                    }
                    default:
                        throw GraalError.shouldNotReachHere();
                }
            }

            @Override
            public Stamp foldStamp(Stamp __s)
            {
                if (__s.isEmpty())
                {
                    return __s;
                }
                FloatStamp __stamp = (FloatStamp) __s;
                JavaConstant __constant = __stamp.asConstant();
                if (__constant != null)
                {
                    Constant __folded = foldConstant(__constant);
                    if (__folded != null)
                    {
                        FloatStamp __result = stampForConstant(__folded);
                        if (__result != null && __result.isConstant())
                        {
                            return __result;
                        }
                    }
                }
                return __s.unrestricted();
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
                switch (__a.getJavaKind())
                {
                    case Float:
                    {
                        int __fa = Float.floatToRawIntBits(__a.asFloat());
                        int __fb = Float.floatToRawIntBits(__b.asFloat());
                        return JavaConstant.forFloat(Float.intBitsToFloat(__fa & __fb));
                    }
                    case Double:
                    {
                        long __da = Double.doubleToRawLongBits(__a.asDouble());
                        long __db = Double.doubleToRawLongBits(__b.asDouble());
                        return JavaConstant.forDouble(Double.longBitsToDouble(__da & __db));
                    }
                    default:
                        throw GraalError.shouldNotReachHere();
                }
            }

            @Override
            public Stamp foldStamp(Stamp __s1, Stamp __s2)
            {
                if (__s1.isEmpty())
                {
                    return __s1;
                }
                if (__s2.isEmpty())
                {
                    return __s2;
                }
                FloatStamp __stamp1 = (FloatStamp) __s1;
                FloatStamp __stamp2 = (FloatStamp) __s2;
                Stamp __folded = maybeFoldConstant(this, __stamp1, __stamp2);
                if (__folded != null)
                {
                    return __folded;
                }
                return __stamp1.unrestricted();
            }

            @Override
            public boolean isNeutral(Constant __n)
            {
                PrimitiveConstant __value = (PrimitiveConstant) __n;
                switch (__value.getJavaKind())
                {
                    case Float:
                        return Float.floatToRawIntBits(__value.asFloat()) == 0xFFFFFFFF;
                    case Double:
                        return Double.doubleToRawLongBits(__value.asDouble()) == 0xFFFFFFFFFFFFFFFFL;
                    default:
                        throw GraalError.shouldNotReachHere();
                }
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
                switch (__a.getJavaKind())
                {
                    case Float:
                    {
                        int __fa = Float.floatToRawIntBits(__a.asFloat());
                        int __fb = Float.floatToRawIntBits(__b.asFloat());
                        float __floatOr = Float.intBitsToFloat(__fa | __fb);
                        return JavaConstant.forFloat(__floatOr);
                    }
                    case Double:
                    {
                        long __da = Double.doubleToRawLongBits(__a.asDouble());
                        long __db = Double.doubleToRawLongBits(__b.asDouble());
                        return JavaConstant.forDouble(Double.longBitsToDouble(__da | __db));
                    }
                    default:
                        throw GraalError.shouldNotReachHere();
                }
            }

            @Override
            public Stamp foldStamp(Stamp __s1, Stamp __s2)
            {
                if (__s1.isEmpty())
                {
                    return __s1;
                }
                if (__s2.isEmpty())
                {
                    return __s2;
                }
                FloatStamp __stamp1 = (FloatStamp) __s1;
                FloatStamp __stamp2 = (FloatStamp) __s2;
                Stamp __folded = maybeFoldConstant(this, __stamp1, __stamp2);
                if (__folded != null)
                {
                    return __folded;
                }
                return __stamp1.unrestricted();
            }

            @Override
            public boolean isNeutral(Constant __n)
            {
                PrimitiveConstant __value = (PrimitiveConstant) __n;
                switch (__value.getJavaKind())
                {
                    case Float:
                        return Float.floatToRawIntBits(__value.asFloat()) == 0;
                    case Double:
                        return Double.doubleToRawLongBits(__value.asDouble()) == 0L;
                    default:
                        throw GraalError.shouldNotReachHere();
                }
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
                switch (__a.getJavaKind())
                {
                    case Float:
                    {
                        int __fa = Float.floatToRawIntBits(__a.asFloat());
                        int __fb = Float.floatToRawIntBits(__b.asFloat());
                        return JavaConstant.forFloat(Float.intBitsToFloat(__fa ^ __fb));
                    }
                    case Double:
                    {
                        long __da = Double.doubleToRawLongBits(__a.asDouble());
                        long __db = Double.doubleToRawLongBits(__b.asDouble());
                        return JavaConstant.forDouble(Double.longBitsToDouble(__da ^ __db));
                    }
                    default:
                        throw GraalError.shouldNotReachHere();
                }
            }

            @Override
            public Stamp foldStamp(Stamp __s1, Stamp __s2)
            {
                if (__s1.isEmpty())
                {
                    return __s1;
                }
                if (__s2.isEmpty())
                {
                    return __s2;
                }
                FloatStamp __stamp1 = (FloatStamp) __s1;
                FloatStamp __stamp2 = (FloatStamp) __s2;
                Stamp __folded = maybeFoldConstant(this, __stamp1, __stamp2);
                if (__folded != null)
                {
                    return __folded;
                }
                return __stamp1.unrestricted();
            }

            @Override
            public boolean isNeutral(Constant __n)
            {
                PrimitiveConstant __value = (PrimitiveConstant) __n;
                switch (__value.getJavaKind())
                {
                    case Float:
                        return Float.floatToRawIntBits(__value.asFloat()) == 0;
                    case Double:
                        return Double.doubleToRawLongBits(__value.asDouble()) == 0L;
                    default:
                        throw GraalError.shouldNotReachHere();
                }
            }
        },

        null, null, null,

        // @closure
        new UnaryOp.Abs()
        {
            @Override
            public Constant foldConstant(Constant __c)
            {
                PrimitiveConstant __value = (PrimitiveConstant) __c;
                switch (__value.getJavaKind())
                {
                    case Float:
                        return JavaConstant.forFloat(Math.abs(__value.asFloat()));
                    case Double:
                        return JavaConstant.forDouble(Math.abs(__value.asDouble()));
                    default:
                        throw GraalError.shouldNotReachHere();
                }
            }

            @Override
            public Stamp foldStamp(Stamp __s)
            {
                if (__s.isEmpty())
                {
                    return __s;
                }
                FloatStamp __stamp = (FloatStamp) __s;
                Stamp __folded = maybeFoldConstant(this, __stamp);
                if (__folded != null)
                {
                    return __folded;
                }
                if (__stamp.isNaN())
                {
                    return __stamp;
                }
                return new FloatStamp(__stamp.getBits(), 0, Math.max(-__stamp.lowerBound(), __stamp.upperBound()), __stamp.isNonNaN());
            }
        },

        // @closure
        new UnaryOp.Sqrt()
        {
            @Override
            public Constant foldConstant(Constant __c)
            {
                PrimitiveConstant __value = (PrimitiveConstant) __c;
                switch (__value.getJavaKind())
                {
                    case Float:
                        return JavaConstant.forFloat((float) Math.sqrt(__value.asFloat()));
                    case Double:
                        return JavaConstant.forDouble(Math.sqrt(__value.asDouble()));
                    default:
                        throw GraalError.shouldNotReachHere();
                }
            }

            @Override
            public Stamp foldStamp(Stamp __s)
            {
                if (__s.isEmpty())
                {
                    return __s;
                }
                FloatStamp __stamp = (FloatStamp) __s;
                Stamp __folded = maybeFoldConstant(this, __stamp);
                if (__folded != null)
                {
                    return __folded;
                }
                return __s.unrestricted();
            }
        },

        null, null, null,

        // @closure
        new FloatConvertOp(FloatConvert.F2I)
        {
            @Override
            public Constant foldConstant(Constant __c)
            {
                PrimitiveConstant __value = (PrimitiveConstant) __c;
                return JavaConstant.forInt((int) __value.asFloat());
            }

            @Override
            public Stamp foldStamp(Stamp __stamp)
            {
                if (__stamp.isEmpty())
                {
                    return StampFactory.empty(JavaKind.Int);
                }
                FloatStamp __floatStamp = (FloatStamp) __stamp;
                boolean __mustHaveZero = !__floatStamp.isNonNaN();
                int __lowerBound = (int) __floatStamp.lowerBound();
                int __upperBound = (int) __floatStamp.upperBound();
                if (__mustHaveZero)
                {
                    if (__lowerBound > 0)
                    {
                        __lowerBound = 0;
                    }
                    else if (__upperBound < 0)
                    {
                        __upperBound = 0;
                    }
                }
                return StampFactory.forInteger(JavaKind.Int, __lowerBound, __upperBound);
            }
        },

        // @closure
        new FloatConvertOp(FloatConvert.F2L)
        {
            @Override
            public Constant foldConstant(Constant __c)
            {
                PrimitiveConstant __value = (PrimitiveConstant) __c;
                return JavaConstant.forLong((long) __value.asFloat());
            }

            @Override
            public Stamp foldStamp(Stamp __stamp)
            {
                if (__stamp.isEmpty())
                {
                    return StampFactory.empty(JavaKind.Long);
                }
                FloatStamp __floatStamp = (FloatStamp) __stamp;
                boolean __mustHaveZero = !__floatStamp.isNonNaN();
                long __lowerBound = (long) __floatStamp.lowerBound();
                long __upperBound = (long) __floatStamp.upperBound();
                if (__mustHaveZero)
                {
                    if (__lowerBound > 0)
                    {
                        __lowerBound = 0;
                    }
                    else if (__upperBound < 0)
                    {
                        __upperBound = 0;
                    }
                }
                return StampFactory.forInteger(JavaKind.Long, __lowerBound, __upperBound);
            }
        },

        // @closure
        new FloatConvertOp(FloatConvert.D2I)
        {
            @Override
            public Constant foldConstant(Constant __c)
            {
                PrimitiveConstant __value = (PrimitiveConstant) __c;
                return JavaConstant.forInt((int) __value.asDouble());
            }

            @Override
            public Stamp foldStamp(Stamp __stamp)
            {
                if (__stamp.isEmpty())
                {
                    return StampFactory.empty(JavaKind.Int);
                }
                FloatStamp __floatStamp = (FloatStamp) __stamp;
                boolean __mustHaveZero = !__floatStamp.isNonNaN();
                int __lowerBound = (int) __floatStamp.lowerBound();
                int __upperBound = (int) __floatStamp.upperBound();
                if (__mustHaveZero)
                {
                    if (__lowerBound > 0)
                    {
                        __lowerBound = 0;
                    }
                    else if (__upperBound < 0)
                    {
                        __upperBound = 0;
                    }
                }
                return StampFactory.forInteger(JavaKind.Int, __lowerBound, __upperBound);
            }
        },

        // @closure
        new FloatConvertOp(FloatConvert.D2L)
        {
            @Override
            public Constant foldConstant(Constant __c)
            {
                PrimitiveConstant __value = (PrimitiveConstant) __c;
                return JavaConstant.forLong((long) __value.asDouble());
            }

            @Override
            public Stamp foldStamp(Stamp __stamp)
            {
                if (__stamp.isEmpty())
                {
                    return StampFactory.empty(JavaKind.Long);
                }
                FloatStamp __floatStamp = (FloatStamp) __stamp;
                boolean __mustHaveZero = !__floatStamp.isNonNaN();
                long __lowerBound = (long) __floatStamp.lowerBound();
                long __upperBound = (long) __floatStamp.upperBound();
                if (__mustHaveZero)
                {
                    if (__lowerBound > 0)
                    {
                        __lowerBound = 0;
                    }
                    else if (__upperBound < 0)
                    {
                        __upperBound = 0;
                    }
                }
                return StampFactory.forInteger(JavaKind.Long, __lowerBound, __upperBound);
            }
        },

        // @closure
        new FloatConvertOp(FloatConvert.F2D)
        {
            @Override
            public Constant foldConstant(Constant __c)
            {
                PrimitiveConstant __value = (PrimitiveConstant) __c;
                return JavaConstant.forDouble(__value.asFloat());
            }

            @Override
            public Stamp foldStamp(Stamp __stamp)
            {
                if (__stamp.isEmpty())
                {
                    return StampFactory.empty(JavaKind.Double);
                }
                FloatStamp __floatStamp = (FloatStamp) __stamp;
                return StampFactory.forFloat(JavaKind.Double, __floatStamp.lowerBound(), __floatStamp.upperBound(), __floatStamp.isNonNaN());
            }
        },

        // @closure
        new FloatConvertOp(FloatConvert.D2F)
        {
            @Override
            public Constant foldConstant(Constant __c)
            {
                PrimitiveConstant __value = (PrimitiveConstant) __c;
                return JavaConstant.forFloat((float) __value.asDouble());
            }

            @Override
            public Stamp foldStamp(Stamp __stamp)
            {
                if (__stamp.isEmpty())
                {
                    return StampFactory.empty(JavaKind.Float);
                }
                FloatStamp __floatStamp = (FloatStamp) __stamp;
                return StampFactory.forFloat(JavaKind.Float, (float) __floatStamp.lowerBound(), (float) __floatStamp.upperBound(), __floatStamp.isNonNaN());
            }
        }
    );
}
