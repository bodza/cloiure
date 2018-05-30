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
    private final UnaryOp<Neg> neg;
    private final BinaryOp<Add> add;
    private final BinaryOp<Sub> sub;

    private final BinaryOp<Mul> mul;
    private final BinaryOp<MulHigh> mulHigh;
    private final BinaryOp<UMulHigh> umulHigh;
    private final BinaryOp<Div> div;
    private final BinaryOp<Rem> rem;

    private final UnaryOp<Not> not;
    private final BinaryOp<And> and;
    private final BinaryOp<Or> or;
    private final BinaryOp<Xor> xor;

    private final ShiftOp<Shl> shl;
    private final ShiftOp<Shr> shr;
    private final ShiftOp<UShr> ushr;

    private final UnaryOp<Abs> abs;
    private final UnaryOp<Sqrt> sqrt;

    private final IntegerConvertOp<ZeroExtend> zeroExtend;
    private final IntegerConvertOp<SignExtend> signExtend;
    private final IntegerConvertOp<Narrow> narrow;

    private final FloatConvertOp[] floatConvert;
    private final int hash;

    public static ArithmeticOpTable forStamp(Stamp s)
    {
        if (s instanceof ArithmeticStamp)
        {
            return ((ArithmeticStamp) s).getOps();
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

    private static <T> T wrapIfNonNull(Function<T, T> wrapper, T obj)
    {
        if (obj == null)
        {
            return null;
        }
        else
        {
            return wrapper.apply(obj);
        }
    }

    /**
     * Filters {@code inputs} with {@code predicate}, applies {@code mapper} and adds them in the
     * array provided by {@code arrayGenerator}.
     *
     * @return the array provided by {@code arrayGenerator}.
     */
    private static <T, R> R[] filterAndMapToArray(T[] inputs, Predicate<? super T> predicate, Function<? super T, ? extends R> mapper, IntFunction<R[]> arrayGenerator)
    {
        List<R> resultList = new ArrayList<>();
        for (T t : inputs)
        {
            if (predicate.test(t))
            {
                resultList.add(mapper.apply(t));
            }
        }
        return resultList.toArray(arrayGenerator.apply(resultList.size()));
    }

    public static ArithmeticOpTable wrap(ArithmeticOpWrapper wrapper, ArithmeticOpTable inner)
    {
        UnaryOp<Neg> neg = wrapIfNonNull(wrapper::wrapUnaryOp, inner.getNeg());
        BinaryOp<Add> add = wrapIfNonNull(wrapper::wrapBinaryOp, inner.getAdd());
        BinaryOp<Sub> sub = wrapIfNonNull(wrapper::wrapBinaryOp, inner.getSub());

        BinaryOp<Mul> mul = wrapIfNonNull(wrapper::wrapBinaryOp, inner.getMul());
        BinaryOp<MulHigh> mulHigh = wrapIfNonNull(wrapper::wrapBinaryOp, inner.getMulHigh());
        BinaryOp<UMulHigh> umulHigh = wrapIfNonNull(wrapper::wrapBinaryOp, inner.getUMulHigh());
        BinaryOp<Div> div = wrapIfNonNull(wrapper::wrapBinaryOp, inner.getDiv());
        BinaryOp<Rem> rem = wrapIfNonNull(wrapper::wrapBinaryOp, inner.getRem());

        UnaryOp<Not> not = wrapIfNonNull(wrapper::wrapUnaryOp, inner.getNot());
        BinaryOp<And> and = wrapIfNonNull(wrapper::wrapBinaryOp, inner.getAnd());
        BinaryOp<Or> or = wrapIfNonNull(wrapper::wrapBinaryOp, inner.getOr());
        BinaryOp<Xor> xor = wrapIfNonNull(wrapper::wrapBinaryOp, inner.getXor());

        ShiftOp<Shl> shl = wrapIfNonNull(wrapper::wrapShiftOp, inner.getShl());
        ShiftOp<Shr> shr = wrapIfNonNull(wrapper::wrapShiftOp, inner.getShr());
        ShiftOp<UShr> ushr = wrapIfNonNull(wrapper::wrapShiftOp, inner.getUShr());

        UnaryOp<Abs> abs = wrapIfNonNull(wrapper::wrapUnaryOp, inner.getAbs());
        UnaryOp<Sqrt> sqrt = wrapIfNonNull(wrapper::wrapUnaryOp, inner.getSqrt());

        IntegerConvertOp<ZeroExtend> zeroExtend = wrapIfNonNull(wrapper::wrapIntegerConvertOp, inner.getZeroExtend());
        IntegerConvertOp<SignExtend> signExtend = wrapIfNonNull(wrapper::wrapIntegerConvertOp, inner.getSignExtend());
        IntegerConvertOp<Narrow> narrow = wrapIfNonNull(wrapper::wrapIntegerConvertOp, inner.getNarrow());

        FloatConvertOp[] floatConvert = filterAndMapToArray(inner.floatConvert, Objects::nonNull, wrapper::wrapFloatConvertOp, FloatConvertOp[]::new);
        return new ArithmeticOpTable(neg, add, sub, mul, mulHigh, umulHigh, div, rem, not, and, or, xor, shl, shr, ushr, abs, sqrt, zeroExtend, signExtend, narrow, floatConvert);
    }

    // @cons
    protected ArithmeticOpTable(UnaryOp<Neg> neg, BinaryOp<Add> add, BinaryOp<Sub> sub, BinaryOp<Mul> mul, BinaryOp<MulHigh> mulHigh, BinaryOp<UMulHigh> umulHigh, BinaryOp<Div> div, BinaryOp<Rem> rem, UnaryOp<Not> not, BinaryOp<And> and, BinaryOp<Or> or, BinaryOp<Xor> xor, ShiftOp<Shl> shl, ShiftOp<Shr> shr, ShiftOp<UShr> ushr, UnaryOp<Abs> abs, UnaryOp<Sqrt> sqrt, IntegerConvertOp<ZeroExtend> zeroExtend, IntegerConvertOp<SignExtend> signExtend, IntegerConvertOp<Narrow> narrow, FloatConvertOp... floatConvert)
    {
        super();
        this.neg = neg;
        this.add = add;
        this.sub = sub;
        this.mul = mul;
        this.mulHigh = mulHigh;
        this.umulHigh = umulHigh;
        this.div = div;
        this.rem = rem;
        this.not = not;
        this.and = and;
        this.or = or;
        this.xor = xor;
        this.shl = shl;
        this.shr = shr;
        this.ushr = ushr;
        this.abs = abs;
        this.sqrt = sqrt;
        this.zeroExtend = zeroExtend;
        this.signExtend = signExtend;
        this.narrow = narrow;
        this.floatConvert = new FloatConvertOp[FloatConvert.values().length];
        for (FloatConvertOp op : floatConvert)
        {
            this.floatConvert[op.getFloatConvert().ordinal()] = op;
        }

        this.hash = Objects.hash(neg, add, sub, mul, div, rem, not, and, or, xor, shl, shr, ushr, abs, sqrt, zeroExtend, signExtend, narrow);
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
    public FloatConvertOp getFloatConvert(FloatConvert op)
    {
        return floatConvert[op.ordinal()];
    }

    private boolean opsEquals(ArithmeticOpTable that)
    {
        return Objects.equals(neg, that.neg) &&
               Objects.equals(add, that.add) &&
               Objects.equals(sub, that.sub) &&
               Objects.equals(mul, that.mul) &&
               Objects.equals(mulHigh, that.mulHigh) &&
               Objects.equals(umulHigh, that.umulHigh) &&
               Objects.equals(div, that.div) &&
               Objects.equals(rem, that.rem) &&
               Objects.equals(not, that.not) &&
               Objects.equals(and, that.and) &&
               Objects.equals(or, that.or) &&
               Objects.equals(xor, that.xor) &&
               Objects.equals(shl, that.shl) &&
               Objects.equals(shr, that.shr) &&
               Objects.equals(ushr, that.ushr) &&
               Objects.equals(abs, that.abs) &&
               Objects.equals(sqrt, that.sqrt) &&
               Objects.equals(zeroExtend, that.zeroExtend) &&
               Objects.equals(signExtend, that.signExtend) &&
               Objects.equals(narrow, that.narrow);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        ArithmeticOpTable that = (ArithmeticOpTable) obj;
        if (opsEquals(that))
        {
            if (Arrays.equals(this.floatConvert, that.floatConvert))
            {
                return true;
            }
        }
        return false;
    }

    // @class ArithmeticOpTable.Op
    public abstract static class Op
    {
        private final String operator;

        // @cons
        protected Op(String operator)
        {
            super();
            this.operator = operator;
        }

        @Override
        public int hashCode()
        {
            return operator.hashCode();
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }
            if (obj == null)
            {
                return false;
            }
            if (getClass() != obj.getClass())
            {
                return false;
            }
            Op that = (Op) obj;
            if (operator.equals(that.operator))
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
        protected UnaryOp(String operation)
        {
            super(operation);
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
            protected Add(boolean associative, boolean commutative)
            {
                super("+", associative, commutative);
            }
        }

        // @class ArithmeticOpTable.BinaryOp.Sub
        public abstract static class Sub extends BinaryOp<Sub>
        {
            // @cons
            protected Sub(boolean associative, boolean commutative)
            {
                super("-", associative, commutative);
            }
        }

        // @class ArithmeticOpTable.BinaryOp.Mul
        public abstract static class Mul extends BinaryOp<Mul>
        {
            // @cons
            protected Mul(boolean associative, boolean commutative)
            {
                super("*", associative, commutative);
            }
        }

        // @class ArithmeticOpTable.BinaryOp.MulHigh
        public abstract static class MulHigh extends BinaryOp<MulHigh>
        {
            // @cons
            protected MulHigh(boolean associative, boolean commutative)
            {
                super("*H", associative, commutative);
            }
        }

        // @class ArithmeticOpTable.BinaryOp.UMulHigh
        public abstract static class UMulHigh extends BinaryOp<UMulHigh>
        {
            // @cons
            protected UMulHigh(boolean associative, boolean commutative)
            {
                super("|*H|", associative, commutative);
            }
        }

        // @class ArithmeticOpTable.BinaryOp.Div
        public abstract static class Div extends BinaryOp<Div>
        {
            // @cons
            protected Div(boolean associative, boolean commutative)
            {
                super("/", associative, commutative);
            }
        }

        // @class ArithmeticOpTable.BinaryOp.Rem
        public abstract static class Rem extends BinaryOp<Rem>
        {
            // @cons
            protected Rem(boolean associative, boolean commutative)
            {
                super("%", associative, commutative);
            }
        }

        // @class ArithmeticOpTable.BinaryOp.And
        public abstract static class And extends BinaryOp<And>
        {
            // @cons
            protected And(boolean associative, boolean commutative)
            {
                super("&", associative, commutative);
            }
        }

        // @class ArithmeticOpTable.BinaryOp.Or
        public abstract static class Or extends BinaryOp<Or>
        {
            // @cons
            protected Or(boolean associative, boolean commutative)
            {
                super("|", associative, commutative);
            }
        }

        // @class ArithmeticOpTable.BinaryOp.Xor
        public abstract static class Xor extends BinaryOp<Xor>
        {
            // @cons
            protected Xor(boolean associative, boolean commutative)
            {
                super("^", associative, commutative);
            }
        }

        private final boolean associative;
        private final boolean commutative;

        // @cons
        protected BinaryOp(String operation, boolean associative, boolean commutative)
        {
            super(operation);
            this.associative = associative;
            this.commutative = commutative;
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
        public boolean isNeutral(Constant n)
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
        public Constant getZero(Stamp stamp)
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
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + (associative ? 1231 : 1237);
            result = prime * result + (commutative ? 1231 : 1237);
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }
            if (!super.equals(obj))
            {
                return false;
            }
            if (getClass() != obj.getClass())
            {
                return false;
            }
            BinaryOp<?> that = (BinaryOp<?>) obj;
            if (associative != that.associative)
            {
                return false;
            }
            if (commutative != that.commutative)
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
        protected ShiftOp(String operation)
        {
            super(operation);
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
        private final FloatConvert op;

        // @cons
        protected FloatConvertOp(FloatConvert op)
        {
            super(op.name());
            this.op = op;
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
            final int prime = 31;
            return prime * super.hashCode() + op.hashCode();
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }
            if (!super.equals(obj))
            {
                return false;
            }
            if (getClass() != obj.getClass())
            {
                return false;
            }
            FloatConvertOp that = (FloatConvertOp) obj;
            if (op != that.op)
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
            public Stamp invertStamp(int inputBits, int resultBits, Stamp outStamp)
            {
                return null;
            }
        }

        // @cons
        protected IntegerConvertOp(String op)
        {
            super(op);
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
