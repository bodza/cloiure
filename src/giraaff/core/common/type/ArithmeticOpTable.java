package giraaff.core.common.type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaKind;

import giraaff.core.common.calc.FloatConvert;
import giraaff.core.common.type.ArithmeticOpTable.BinaryOp.Add;
import giraaff.core.common.type.ArithmeticOpTable.BinaryOp.And;
import giraaff.core.common.type.ArithmeticOpTable.BinaryOp.Div;
import giraaff.core.common.type.ArithmeticOpTable.BinaryOp.Mul;
import giraaff.core.common.type.ArithmeticOpTable.BinaryOp.MulHigh;
import giraaff.core.common.type.ArithmeticOpTable.BinaryOp.Or;
import giraaff.core.common.type.ArithmeticOpTable.BinaryOp.Rem;
import giraaff.core.common.type.ArithmeticOpTable.BinaryOp.Sub;
import giraaff.core.common.type.ArithmeticOpTable.BinaryOp.UMulHigh;
import giraaff.core.common.type.ArithmeticOpTable.BinaryOp.Xor;
import giraaff.core.common.type.ArithmeticOpTable.IntegerConvertOp.Narrow;
import giraaff.core.common.type.ArithmeticOpTable.IntegerConvertOp.SignExtend;
import giraaff.core.common.type.ArithmeticOpTable.IntegerConvertOp.ZeroExtend;
import giraaff.core.common.type.ArithmeticOpTable.ShiftOp.Shl;
import giraaff.core.common.type.ArithmeticOpTable.ShiftOp.Shr;
import giraaff.core.common.type.ArithmeticOpTable.ShiftOp.UShr;
import giraaff.core.common.type.ArithmeticOpTable.UnaryOp.Abs;
import giraaff.core.common.type.ArithmeticOpTable.UnaryOp.Neg;
import giraaff.core.common.type.ArithmeticOpTable.UnaryOp.Not;
import giraaff.core.common.type.ArithmeticOpTable.UnaryOp.Sqrt;

/**
 * Information about arithmetic operations.
 */
// @class ArithmeticOpTable
public final class ArithmeticOpTable
{
    // @field
    private final UnaryOp<Neg> neg;
    // @field
    private final BinaryOp<Add> add;
    // @field
    private final BinaryOp<Sub> sub;

    // @field
    private final BinaryOp<Mul> mul;
    // @field
    private final BinaryOp<MulHigh> mulHigh;
    // @field
    private final BinaryOp<UMulHigh> umulHigh;
    // @field
    private final BinaryOp<Div> div;
    // @field
    private final BinaryOp<Rem> rem;

    // @field
    private final UnaryOp<Not> not;
    // @field
    private final BinaryOp<And> and;
    // @field
    private final BinaryOp<Or> or;
    // @field
    private final BinaryOp<Xor> xor;

    // @field
    private final ShiftOp<Shl> shl;
    // @field
    private final ShiftOp<Shr> shr;
    // @field
    private final ShiftOp<UShr> ushr;

    // @field
    private final UnaryOp<Abs> abs;
    // @field
    private final UnaryOp<Sqrt> sqrt;

    // @field
    private final IntegerConvertOp<ZeroExtend> zeroExtend;
    // @field
    private final IntegerConvertOp<SignExtend> signExtend;
    // @field
    private final IntegerConvertOp<Narrow> narrow;

    // @field
    private final FloatConvertOp[] floatConvert;
    // @field
    private final int hash;

    public static ArithmeticOpTable forStamp(Stamp __s)
    {
        if (__s instanceof ArithmeticStamp)
        {
            return ((ArithmeticStamp) __s).getOps();
        }
        else
        {
            return EMPTY;
        }
    }

    public BinaryOp<?>[] getBinaryOps()
    {
        return new BinaryOp<?>[] { add, sub, mul, mulHigh, umulHigh, div, rem, and, or, xor };
    }

    public UnaryOp<?>[] getUnaryOps()
    {
        return new UnaryOp<?>[] { neg, not, abs, sqrt };
    }

    public ShiftOp<?>[] getShiftOps()
    {
        return new ShiftOp<?>[] { shl, shr, ushr };
    }

    public IntegerConvertOp<?>[] getIntegerConvertOps()
    {
        return new IntegerConvertOp<?>[] { zeroExtend, signExtend, narrow };
    }

    // @def
    public static final ArithmeticOpTable EMPTY = new ArithmeticOpTable(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

    // @iface ArithmeticOpTable.ArithmeticOpWrapper
    public interface ArithmeticOpWrapper
    {
        <OP> UnaryOp<OP> wrapUnaryOp(UnaryOp<OP> op);

        <OP> BinaryOp<OP> wrapBinaryOp(BinaryOp<OP> op);

        <OP> ShiftOp<OP> wrapShiftOp(ShiftOp<OP> op);

        <OP> IntegerConvertOp<OP> wrapIntegerConvertOp(IntegerConvertOp<OP> op);

        FloatConvertOp wrapFloatConvertOp(FloatConvertOp op);
    }

    private static <T> T wrapIfNonNull(Function<T, T> __wrapper, T __obj)
    {
        if (__obj == null)
        {
            return null;
        }
        else
        {
            return __wrapper.apply(__obj);
        }
    }

    /**
     * Filters {@code inputs} with {@code predicate}, applies {@code mapper} and adds them in the
     * array provided by {@code arrayGenerator}.
     *
     * @return the array provided by {@code arrayGenerator}.
     */
    private static <T, R> R[] filterAndMapToArray(T[] __inputs, Predicate<? super T> __predicate, Function<? super T, ? extends R> __mapper, IntFunction<R[]> __arrayGenerator)
    {
        List<R> __resultList = new ArrayList<>();
        for (T __t : __inputs)
        {
            if (__predicate.test(__t))
            {
                __resultList.add(__mapper.apply(__t));
            }
        }
        return __resultList.toArray(__arrayGenerator.apply(__resultList.size()));
    }

    public static ArithmeticOpTable wrap(ArithmeticOpWrapper __wrapper, ArithmeticOpTable __inner)
    {
        UnaryOp<Neg> __neg = wrapIfNonNull(__wrapper::wrapUnaryOp, __inner.getNeg());
        BinaryOp<Add> __add = wrapIfNonNull(__wrapper::wrapBinaryOp, __inner.getAdd());
        BinaryOp<Sub> __sub = wrapIfNonNull(__wrapper::wrapBinaryOp, __inner.getSub());

        BinaryOp<Mul> __mul = wrapIfNonNull(__wrapper::wrapBinaryOp, __inner.getMul());
        BinaryOp<MulHigh> __mulHigh = wrapIfNonNull(__wrapper::wrapBinaryOp, __inner.getMulHigh());
        BinaryOp<UMulHigh> __umulHigh = wrapIfNonNull(__wrapper::wrapBinaryOp, __inner.getUMulHigh());
        BinaryOp<Div> __div = wrapIfNonNull(__wrapper::wrapBinaryOp, __inner.getDiv());
        BinaryOp<Rem> __rem = wrapIfNonNull(__wrapper::wrapBinaryOp, __inner.getRem());

        UnaryOp<Not> __not = wrapIfNonNull(__wrapper::wrapUnaryOp, __inner.getNot());
        BinaryOp<And> __and = wrapIfNonNull(__wrapper::wrapBinaryOp, __inner.getAnd());
        BinaryOp<Or> __or = wrapIfNonNull(__wrapper::wrapBinaryOp, __inner.getOr());
        BinaryOp<Xor> __xor = wrapIfNonNull(__wrapper::wrapBinaryOp, __inner.getXor());

        ShiftOp<Shl> __shl = wrapIfNonNull(__wrapper::wrapShiftOp, __inner.getShl());
        ShiftOp<Shr> __shr = wrapIfNonNull(__wrapper::wrapShiftOp, __inner.getShr());
        ShiftOp<UShr> __ushr = wrapIfNonNull(__wrapper::wrapShiftOp, __inner.getUShr());

        UnaryOp<Abs> __abs = wrapIfNonNull(__wrapper::wrapUnaryOp, __inner.getAbs());
        UnaryOp<Sqrt> __sqrt = wrapIfNonNull(__wrapper::wrapUnaryOp, __inner.getSqrt());

        IntegerConvertOp<ZeroExtend> __zeroExtend = wrapIfNonNull(__wrapper::wrapIntegerConvertOp, __inner.getZeroExtend());
        IntegerConvertOp<SignExtend> __signExtend = wrapIfNonNull(__wrapper::wrapIntegerConvertOp, __inner.getSignExtend());
        IntegerConvertOp<Narrow> __narrow = wrapIfNonNull(__wrapper::wrapIntegerConvertOp, __inner.getNarrow());

        FloatConvertOp[] __floatConvert = filterAndMapToArray(__inner.floatConvert, Objects::nonNull, __wrapper::wrapFloatConvertOp, FloatConvertOp[]::new);
        return new ArithmeticOpTable(__neg, __add, __sub, __mul, __mulHigh, __umulHigh, __div, __rem, __not, __and, __or, __xor, __shl, __shr, __ushr, __abs, __sqrt, __zeroExtend, __signExtend, __narrow, __floatConvert);
    }

    // @cons
    protected ArithmeticOpTable(UnaryOp<Neg> __neg, BinaryOp<Add> __add, BinaryOp<Sub> __sub, BinaryOp<Mul> __mul, BinaryOp<MulHigh> __mulHigh, BinaryOp<UMulHigh> __umulHigh, BinaryOp<Div> __div, BinaryOp<Rem> __rem, UnaryOp<Not> __not, BinaryOp<And> __and, BinaryOp<Or> __or, BinaryOp<Xor> __xor, ShiftOp<Shl> __shl, ShiftOp<Shr> __shr, ShiftOp<UShr> __ushr, UnaryOp<Abs> __abs, UnaryOp<Sqrt> __sqrt, IntegerConvertOp<ZeroExtend> __zeroExtend, IntegerConvertOp<SignExtend> __signExtend, IntegerConvertOp<Narrow> __narrow, FloatConvertOp... __floatConvert)
    {
        super();
        this.neg = __neg;
        this.add = __add;
        this.sub = __sub;
        this.mul = __mul;
        this.mulHigh = __mulHigh;
        this.umulHigh = __umulHigh;
        this.div = __div;
        this.rem = __rem;
        this.not = __not;
        this.and = __and;
        this.or = __or;
        this.xor = __xor;
        this.shl = __shl;
        this.shr = __shr;
        this.ushr = __ushr;
        this.abs = __abs;
        this.sqrt = __sqrt;
        this.zeroExtend = __zeroExtend;
        this.signExtend = __signExtend;
        this.narrow = __narrow;
        this.floatConvert = new FloatConvertOp[FloatConvert.values().length];
        for (FloatConvertOp __op : __floatConvert)
        {
            this.floatConvert[__op.getFloatConvert().ordinal()] = __op;
        }

        this.hash = Objects.hash(__neg, __add, __sub, __mul, __div, __rem, __not, __and, __or, __xor, __shl, __shr, __ushr, __abs, __sqrt, __zeroExtend, __signExtend, __narrow);
    }

    @Override
    public int hashCode()
    {
        return hash;
    }

    /**
     * Describes the unary negation operation.
     */
    public UnaryOp<Neg> getNeg()
    {
        return neg;
    }

    /**
     * Describes the addition operation.
     */
    public BinaryOp<Add> getAdd()
    {
        return add;
    }

    /**
     * Describes the subtraction operation.
     */
    public BinaryOp<Sub> getSub()
    {
        return sub;
    }

    /**
     * Describes the multiplication operation.
     */
    public BinaryOp<Mul> getMul()
    {
        return mul;
    }

    /**
     * Describes a signed operation that multiples the upper 32-bits of two long values.
     */
    public BinaryOp<MulHigh> getMulHigh()
    {
        return mulHigh;
    }

    /**
     * Describes an unsigned operation that multiples the upper 32-bits of two long values.
     */
    public BinaryOp<UMulHigh> getUMulHigh()
    {
        return umulHigh;
    }

    /**
     * Describes the division operation.
     */
    public BinaryOp<Div> getDiv()
    {
        return div;
    }

    /**
     * Describes the remainder operation.
     */
    public BinaryOp<Rem> getRem()
    {
        return rem;
    }

    /**
     * Describes the bitwise not operation.
     */
    public UnaryOp<Not> getNot()
    {
        return not;
    }

    /**
     * Describes the bitwise and operation.
     */
    public BinaryOp<And> getAnd()
    {
        return and;
    }

    /**
     * Describes the bitwise or operation.
     */
    public BinaryOp<Or> getOr()
    {
        return or;
    }

    /**
     * Describes the bitwise xor operation.
     */
    public BinaryOp<Xor> getXor()
    {
        return xor;
    }

    /**
     * Describes the shift left operation.
     */
    public ShiftOp<Shl> getShl()
    {
        return shl;
    }

    /**
     * Describes the signed shift right operation.
     */
    public ShiftOp<Shr> getShr()
    {
        return shr;
    }

    /**
     * Describes the unsigned shift right operation.
     */
    public ShiftOp<UShr> getUShr()
    {
        return ushr;
    }

    /**
     * Describes the absolute value operation.
     */
    public UnaryOp<Abs> getAbs()
    {
        return abs;
    }

    /**
     * Describes the square root operation.
     */
    public UnaryOp<Sqrt> getSqrt()
    {
        return sqrt;
    }

    /**
     * Describes the zero extend conversion.
     */
    public IntegerConvertOp<ZeroExtend> getZeroExtend()
    {
        return zeroExtend;
    }

    /**
     * Describes the sign extend conversion.
     */
    public IntegerConvertOp<SignExtend> getSignExtend()
    {
        return signExtend;
    }

    /**
     * Describes the narrowing conversion.
     */
    public IntegerConvertOp<Narrow> getNarrow()
    {
        return narrow;
    }

    /**
     * Describes integer/float/double conversions.
     */
    public FloatConvertOp getFloatConvert(FloatConvert __op)
    {
        return floatConvert[__op.ordinal()];
    }

    private boolean opsEquals(ArithmeticOpTable __that)
    {
        return Objects.equals(neg, __that.neg) &&
               Objects.equals(add, __that.add) &&
               Objects.equals(sub, __that.sub) &&
               Objects.equals(mul, __that.mul) &&
               Objects.equals(mulHigh, __that.mulHigh) &&
               Objects.equals(umulHigh, __that.umulHigh) &&
               Objects.equals(div, __that.div) &&
               Objects.equals(rem, __that.rem) &&
               Objects.equals(not, __that.not) &&
               Objects.equals(and, __that.and) &&
               Objects.equals(or, __that.or) &&
               Objects.equals(xor, __that.xor) &&
               Objects.equals(shl, __that.shl) &&
               Objects.equals(shr, __that.shr) &&
               Objects.equals(ushr, __that.ushr) &&
               Objects.equals(abs, __that.abs) &&
               Objects.equals(sqrt, __that.sqrt) &&
               Objects.equals(zeroExtend, __that.zeroExtend) &&
               Objects.equals(signExtend, __that.signExtend) &&
               Objects.equals(narrow, __that.narrow);
    }

    @Override
    public boolean equals(Object __obj)
    {
        if (this == __obj)
        {
            return true;
        }
        if (__obj == null)
        {
            return false;
        }
        if (getClass() != __obj.getClass())
        {
            return false;
        }
        ArithmeticOpTable __that = (ArithmeticOpTable) __obj;
        if (opsEquals(__that))
        {
            if (Arrays.equals(this.floatConvert, __that.floatConvert))
            {
                return true;
            }
        }
        return false;
    }

    // @class ArithmeticOpTable.Op
    public abstract static class Op
    {
        // @field
        private final String operator;

        // @cons
        protected Op(String __operator)
        {
            super();
            this.operator = __operator;
        }

        @Override
        public int hashCode()
        {
            return operator.hashCode();
        }

        @Override
        public boolean equals(Object __obj)
        {
            if (this == __obj)
            {
                return true;
            }
            if (__obj == null)
            {
                return false;
            }
            if (getClass() != __obj.getClass())
            {
                return false;
            }
            Op __that = (Op) __obj;
            if (operator.equals(__that.operator))
            {
                return true;
            }
            return true;
        }
    }

    /**
     * Describes a unary arithmetic operation.
     */
    // @class ArithmeticOpTable.UnaryOp
    public abstract static class UnaryOp<T> extends Op
    {
        // @class ArithmeticOpTable.UnaryOp.Neg
        public abstract static class Neg extends UnaryOp<Neg>
        {
            // @cons
            protected Neg()
            {
                super("-");
            }
        }

        // @class ArithmeticOpTable.UnaryOp.Not
        public abstract static class Not extends UnaryOp<Not>
        {
            // @cons
            protected Not()
            {
                super("~");
            }
        }

        // @class ArithmeticOpTable.UnaryOp.Abs
        public abstract static class Abs extends UnaryOp<Abs>
        {
            // @cons
            protected Abs()
            {
                super("ABS");
            }
        }

        // @class ArithmeticOpTable.UnaryOp.Sqrt
        public abstract static class Sqrt extends UnaryOp<Sqrt>
        {
            // @cons
            protected Sqrt()
            {
                super("SQRT");
            }
        }

        // @cons
        protected UnaryOp(String __operation)
        {
            super(__operation);
        }

        /**
         * Apply the operation to a {@link Constant}.
         */
        public abstract Constant foldConstant(Constant value);

        /**
         * Apply the operation to a {@link Stamp}.
         */
        public abstract Stamp foldStamp(Stamp stamp);

        public UnaryOp<T> unwrap()
        {
            return this;
        }
    }

    /**
     * Describes a binary arithmetic operation.
     */
    // @class ArithmeticOpTable.BinaryOp
    public abstract static class BinaryOp<T> extends Op
    {
        // @class ArithmeticOpTable.BinaryOp.Add
        public abstract static class Add extends BinaryOp<Add>
        {
            // @cons
            protected Add(boolean __associative, boolean __commutative)
            {
                super("+", __associative, __commutative);
            }
        }

        // @class ArithmeticOpTable.BinaryOp.Sub
        public abstract static class Sub extends BinaryOp<Sub>
        {
            // @cons
            protected Sub(boolean __associative, boolean __commutative)
            {
                super("-", __associative, __commutative);
            }
        }

        // @class ArithmeticOpTable.BinaryOp.Mul
        public abstract static class Mul extends BinaryOp<Mul>
        {
            // @cons
            protected Mul(boolean __associative, boolean __commutative)
            {
                super("*", __associative, __commutative);
            }
        }

        // @class ArithmeticOpTable.BinaryOp.MulHigh
        public abstract static class MulHigh extends BinaryOp<MulHigh>
        {
            // @cons
            protected MulHigh(boolean __associative, boolean __commutative)
            {
                super("*H", __associative, __commutative);
            }
        }

        // @class ArithmeticOpTable.BinaryOp.UMulHigh
        public abstract static class UMulHigh extends BinaryOp<UMulHigh>
        {
            // @cons
            protected UMulHigh(boolean __associative, boolean __commutative)
            {
                super("|*H|", __associative, __commutative);
            }
        }

        // @class ArithmeticOpTable.BinaryOp.Div
        public abstract static class Div extends BinaryOp<Div>
        {
            // @cons
            protected Div(boolean __associative, boolean __commutative)
            {
                super("/", __associative, __commutative);
            }
        }

        // @class ArithmeticOpTable.BinaryOp.Rem
        public abstract static class Rem extends BinaryOp<Rem>
        {
            // @cons
            protected Rem(boolean __associative, boolean __commutative)
            {
                super("%", __associative, __commutative);
            }
        }

        // @class ArithmeticOpTable.BinaryOp.And
        public abstract static class And extends BinaryOp<And>
        {
            // @cons
            protected And(boolean __associative, boolean __commutative)
            {
                super("&", __associative, __commutative);
            }
        }

        // @class ArithmeticOpTable.BinaryOp.Or
        public abstract static class Or extends BinaryOp<Or>
        {
            // @cons
            protected Or(boolean __associative, boolean __commutative)
            {
                super("|", __associative, __commutative);
            }
        }

        // @class ArithmeticOpTable.BinaryOp.Xor
        public abstract static class Xor extends BinaryOp<Xor>
        {
            // @cons
            protected Xor(boolean __associative, boolean __commutative)
            {
                super("^", __associative, __commutative);
            }
        }

        // @field
        private final boolean associative;
        // @field
        private final boolean commutative;

        // @cons
        protected BinaryOp(String __operation, boolean __associative, boolean __commutative)
        {
            super(__operation);
            this.associative = __associative;
            this.commutative = __commutative;
        }

        /**
         * Applies this operation to {@code a} and {@code b}.
         *
         * @return the result of applying this operation or {@code null} if applying it would raise
         *         an exception (e.g. {@link ArithmeticException} for dividing by 0)
         */
        public abstract Constant foldConstant(Constant a, Constant b);

        /**
         * Apply the operation to two {@linkplain Stamp Stamps}.
         */
        public abstract Stamp foldStamp(Stamp a, Stamp b);

        /**
         * Checks whether this operation is associative. An operation is associative when
         * {@code (a . b) . c == a . (b . c)} for all a, b, c. Note that you still have to be
         * careful with inverses. For example the integer subtraction operation will report {@code true}
         * here, since you can still reassociate as long as the correct negations are inserted.
         */
        public final boolean isAssociative()
        {
            return associative;
        }

        /**
         * Checks whether this operation is commutative. An operation is commutative when
         * {@code a . b == b . a} for all a, b.
         */
        public final boolean isCommutative()
        {
            return commutative;
        }

        /**
         * Check whether a {@link Constant} is a neutral element for this operation. A neutral
         * element is any element {@code n} where {@code a . n == a} for all a.
         *
         * @param n the {@link Constant} that should be tested
         * @return true iff for all {@code a}: {@code a . n == a}
         */
        public boolean isNeutral(Constant __n)
        {
            return false;
        }

        /**
         * Check whether this operation has a zero {@code z == a . a} for each a. Examples of
         * operations having such an element are subtraction and exclusive-or. Note that this may be
         * different from the numbers tested by {@link #isNeutral}.
         *
         * @param stamp a {@link Stamp}
         * @return a unique {@code z} such that {@code z == a . a} for each {@code a} in
         *         {@code stamp} if it exists, otherwise {@code null}
         */
        public Constant getZero(Stamp __stamp)
        {
            return null;
        }

        public BinaryOp<T> unwrap()
        {
            return this;
        }

        @Override
        public int hashCode()
        {
            final int __prime = 31;
            int __result = super.hashCode();
            __result = __prime * __result + (associative ? 1231 : 1237);
            __result = __prime * __result + (commutative ? 1231 : 1237);
            return __result;
        }

        @Override
        public boolean equals(Object __obj)
        {
            if (this == __obj)
            {
                return true;
            }
            if (!super.equals(__obj))
            {
                return false;
            }
            if (getClass() != __obj.getClass())
            {
                return false;
            }
            BinaryOp<?> __that = (BinaryOp<?>) __obj;
            if (associative != __that.associative)
            {
                return false;
            }
            if (commutative != __that.commutative)
            {
                return false;
            }
            return true;
        }
    }

    /**
     * Describes a shift operation. The right argument of a shift operation always has kind
     * {@link JavaKind#Int}.
     */
    // @class ArithmeticOpTable.ShiftOp
    public abstract static class ShiftOp<OP> extends Op
    {
        // @class ArithmeticOpTable.ShiftOp.Shl
        public abstract static class Shl extends ShiftOp<Shl>
        {
            // @cons
            public Shl()
            {
                super("<<");
            }
        }

        // @class ArithmeticOpTable.ShiftOp.Shr
        public abstract static class Shr extends ShiftOp<Shr>
        {
            // @cons
            public Shr()
            {
                super(">>");
            }
        }

        // @class ArithmeticOpTable.ShiftOp.UShr
        public abstract static class UShr extends ShiftOp<UShr>
        {
            // @cons
            public UShr()
            {
                super(">>>");
            }
        }

        // @cons
        protected ShiftOp(String __operation)
        {
            super(__operation);
        }

        /**
         * Apply the shift to a constant.
         */
        public abstract Constant foldConstant(Constant c, int amount);

        /**
         * Apply the shift to a stamp.
         */
        public abstract Stamp foldStamp(Stamp s, IntegerStamp amount);

        /**
         * Get the shift amount mask for a given result stamp.
         */
        public abstract int getShiftAmountMask(Stamp s);
    }

    // @class ArithmeticOpTable.FloatConvertOp
    public abstract static class FloatConvertOp extends UnaryOp<FloatConvertOp>
    {
        // @field
        private final FloatConvert op;

        // @cons
        protected FloatConvertOp(FloatConvert __op)
        {
            super(__op.name());
            this.op = __op;
        }

        public FloatConvert getFloatConvert()
        {
            return op;
        }

        @Override
        public FloatConvertOp unwrap()
        {
            return this;
        }

        @Override
        public int hashCode()
        {
            final int __prime = 31;
            return __prime * super.hashCode() + op.hashCode();
        }

        @Override
        public boolean equals(Object __obj)
        {
            if (this == __obj)
            {
                return true;
            }
            if (!super.equals(__obj))
            {
                return false;
            }
            if (getClass() != __obj.getClass())
            {
                return false;
            }
            FloatConvertOp __that = (FloatConvertOp) __obj;
            if (op != __that.op)
            {
                return false;
            }
            return true;
        }
    }

    // @class ArithmeticOpTable.IntegerConvertOp
    public abstract static class IntegerConvertOp<T> extends Op
    {
        // @class ArithmeticOpTable.IntegerConvertOp.ZeroExtend
        public abstract static class ZeroExtend extends IntegerConvertOp<ZeroExtend>
        {
            // @cons
            protected ZeroExtend()
            {
                super("ZeroExtend");
            }
        }

        // @class ArithmeticOpTable.IntegerConvertOp.SignExtend
        public abstract static class SignExtend extends IntegerConvertOp<SignExtend>
        {
            // @cons
            protected SignExtend()
            {
                super("SignExtend");
            }
        }

        // @class ArithmeticOpTable.IntegerConvertOp.Narrow
        public abstract static class Narrow extends IntegerConvertOp<Narrow>
        {
            // @cons
            protected Narrow()
            {
                super("Narrow");
            }

            @Override
            public Stamp invertStamp(int __inputBits, int __resultBits, Stamp __outStamp)
            {
                return null;
            }
        }

        // @cons
        protected IntegerConvertOp(String __op)
        {
            super(__op);
        }

        public abstract Constant foldConstant(int inputBits, int resultBits, Constant value);

        public abstract Stamp foldStamp(int inputBits, int resultBits, Stamp stamp);

        public IntegerConvertOp<T> unwrap()
        {
            return this;
        }

        /**
         * Computes the stamp of the input for the given output stamp.
         */
        public abstract Stamp invertStamp(int inputBits, int resultBits, Stamp outStamp);
    }
}
