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

import giraaff.core.common.type.ArithmeticOpTable;

///
// Information about arithmetic operations.
///
// @class ArithmeticOpTable
public final class ArithmeticOpTable
{
    // @field
    private final ArithmeticOpTable.UnaryOp<ArithmeticOpTable.UnaryOp.Neg> ___neg;
    // @field
    private final ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Add> ___add;
    // @field
    private final ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Sub> ___sub;

    // @field
    private final ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Mul> ___mul;
    // @field
    private final ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.MulHigh> ___mulHigh;
    // @field
    private final ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.UMulHigh> ___umulHigh;
    // @field
    private final ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Div> ___div;
    // @field
    private final ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Rem> ___rem;

    // @field
    private final ArithmeticOpTable.UnaryOp<ArithmeticOpTable.UnaryOp.Not> ___not;
    // @field
    private final ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.And> ___and;
    // @field
    private final ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Or> ___or;
    // @field
    private final ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Xor> ___xor;

    // @field
    private final ArithmeticOpTable.ShiftOp<ArithmeticOpTable.ShiftOp.Shl> ___shl;
    // @field
    private final ArithmeticOpTable.ShiftOp<ArithmeticOpTable.ShiftOp.Shr> ___shr;
    // @field
    private final ArithmeticOpTable.ShiftOp<ArithmeticOpTable.ShiftOp.UShr> ___ushr;

    // @field
    private final ArithmeticOpTable.UnaryOp<ArithmeticOpTable.UnaryOp.Abs> ___abs;

    // @field
    private final ArithmeticOpTable.IntegerConvertOp<ArithmeticOpTable.IntegerConvertOp.ZeroExtend> ___zeroExtend;
    // @field
    private final ArithmeticOpTable.IntegerConvertOp<ArithmeticOpTable.IntegerConvertOp.SignExtend> ___signExtend;
    // @field
    private final ArithmeticOpTable.IntegerConvertOp<ArithmeticOpTable.IntegerConvertOp.Narrow> ___narrow;

    // @field
    private final int ___hash;

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

    public ArithmeticOpTable.BinaryOp<?>[] getBinaryOps()
    {
        return new ArithmeticOpTable.BinaryOp<?>[] { this.___add, this.___sub, this.___mul, this.___mulHigh, this.___umulHigh, this.___div, this.___rem, this.___and, this.___or, this.___xor };
    }

    public ArithmeticOpTable.UnaryOp<?>[] getUnaryOps()
    {
        return new ArithmeticOpTable.UnaryOp<?>[] { this.___neg, this.___not, this.___abs };
    }

    public ArithmeticOpTable.ShiftOp<?>[] getShiftOps()
    {
        return new ArithmeticOpTable.ShiftOp<?>[] { this.___shl, this.___shr, this.___ushr };
    }

    public ArithmeticOpTable.IntegerConvertOp<?>[] getIntegerConvertOps()
    {
        return new ArithmeticOpTable.IntegerConvertOp<?>[] { this.___zeroExtend, this.___signExtend, this.___narrow };
    }

    // @def
    public static final ArithmeticOpTable EMPTY = new ArithmeticOpTable(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

    // @iface ArithmeticOpTable.ArithmeticOpWrapper
    public interface ArithmeticOpWrapper
    {
        <OP> ArithmeticOpTable.UnaryOp<OP> wrapUnaryOp(ArithmeticOpTable.UnaryOp<OP> __op);

        <OP> ArithmeticOpTable.BinaryOp<OP> wrapBinaryOp(ArithmeticOpTable.BinaryOp<OP> __op);

        <OP> ArithmeticOpTable.ShiftOp<OP> wrapShiftOp(ArithmeticOpTable.ShiftOp<OP> __op);

        <OP> ArithmeticOpTable.IntegerConvertOp<OP> wrapIntegerConvertOp(ArithmeticOpTable.IntegerConvertOp<OP> __op);
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

    public static ArithmeticOpTable wrap(ArithmeticOpTable.ArithmeticOpWrapper __wrapper, ArithmeticOpTable __inner)
    {
        ArithmeticOpTable.UnaryOp<ArithmeticOpTable.UnaryOp.Neg> __neg = wrapIfNonNull(__wrapper::wrapUnaryOp, __inner.getNeg());
        ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Add> __add = wrapIfNonNull(__wrapper::wrapBinaryOp, __inner.getAdd());
        ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Sub> __sub = wrapIfNonNull(__wrapper::wrapBinaryOp, __inner.getSub());

        ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Mul> __mul = wrapIfNonNull(__wrapper::wrapBinaryOp, __inner.getMul());
        ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.MulHigh> __mulHigh = wrapIfNonNull(__wrapper::wrapBinaryOp, __inner.getMulHigh());
        ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.UMulHigh> __umulHigh = wrapIfNonNull(__wrapper::wrapBinaryOp, __inner.getUMulHigh());
        ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Div> __div = wrapIfNonNull(__wrapper::wrapBinaryOp, __inner.getDiv());
        ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Rem> __rem = wrapIfNonNull(__wrapper::wrapBinaryOp, __inner.getRem());

        ArithmeticOpTable.UnaryOp<ArithmeticOpTable.UnaryOp.Not> __not = wrapIfNonNull(__wrapper::wrapUnaryOp, __inner.getNot());
        ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.And> __and = wrapIfNonNull(__wrapper::wrapBinaryOp, __inner.getAnd());
        ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Or> __or = wrapIfNonNull(__wrapper::wrapBinaryOp, __inner.getOr());
        ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Xor> __xor = wrapIfNonNull(__wrapper::wrapBinaryOp, __inner.getXor());

        ArithmeticOpTable.ShiftOp<ArithmeticOpTable.ShiftOp.Shl> __shl = wrapIfNonNull(__wrapper::wrapShiftOp, __inner.getShl());
        ArithmeticOpTable.ShiftOp<ArithmeticOpTable.ShiftOp.Shr> __shr = wrapIfNonNull(__wrapper::wrapShiftOp, __inner.getShr());
        ArithmeticOpTable.ShiftOp<ArithmeticOpTable.ShiftOp.UShr> __ushr = wrapIfNonNull(__wrapper::wrapShiftOp, __inner.getUShr());

        ArithmeticOpTable.UnaryOp<ArithmeticOpTable.UnaryOp.Abs> __abs = wrapIfNonNull(__wrapper::wrapUnaryOp, __inner.getAbs());

        ArithmeticOpTable.IntegerConvertOp<ArithmeticOpTable.IntegerConvertOp.ZeroExtend> __zeroExtend = wrapIfNonNull(__wrapper::wrapIntegerConvertOp, __inner.getZeroExtend());
        ArithmeticOpTable.IntegerConvertOp<ArithmeticOpTable.IntegerConvertOp.SignExtend> __signExtend = wrapIfNonNull(__wrapper::wrapIntegerConvertOp, __inner.getSignExtend());
        ArithmeticOpTable.IntegerConvertOp<ArithmeticOpTable.IntegerConvertOp.Narrow> __narrow = wrapIfNonNull(__wrapper::wrapIntegerConvertOp, __inner.getNarrow());

        return new ArithmeticOpTable(__neg, __add, __sub, __mul, __mulHigh, __umulHigh, __div, __rem, __not, __and, __or, __xor, __shl, __shr, __ushr, __abs, __zeroExtend, __signExtend, __narrow);
    }

    // @cons ArithmeticOpTable
    protected ArithmeticOpTable(ArithmeticOpTable.UnaryOp<ArithmeticOpTable.UnaryOp.Neg> __neg, ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Add> __add, ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Sub> __sub, ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Mul> __mul, ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.MulHigh> __mulHigh, ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.UMulHigh> __umulHigh, ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Div> __div, ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Rem> __rem, ArithmeticOpTable.UnaryOp<ArithmeticOpTable.UnaryOp.Not> __not, ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.And> __and, ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Or> __or, ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Xor> __xor, ArithmeticOpTable.ShiftOp<ArithmeticOpTable.ShiftOp.Shl> __shl, ArithmeticOpTable.ShiftOp<ArithmeticOpTable.ShiftOp.Shr> __shr, ArithmeticOpTable.ShiftOp<ArithmeticOpTable.ShiftOp.UShr> __ushr, ArithmeticOpTable.UnaryOp<ArithmeticOpTable.UnaryOp.Abs> __abs, ArithmeticOpTable.IntegerConvertOp<ArithmeticOpTable.IntegerConvertOp.ZeroExtend> __zeroExtend, ArithmeticOpTable.IntegerConvertOp<ArithmeticOpTable.IntegerConvertOp.SignExtend> __signExtend, ArithmeticOpTable.IntegerConvertOp<ArithmeticOpTable.IntegerConvertOp.Narrow> __narrow)
    {
        super();
        this.___neg = __neg;
        this.___add = __add;
        this.___sub = __sub;
        this.___mul = __mul;
        this.___mulHigh = __mulHigh;
        this.___umulHigh = __umulHigh;
        this.___div = __div;
        this.___rem = __rem;
        this.___not = __not;
        this.___and = __and;
        this.___or = __or;
        this.___xor = __xor;
        this.___shl = __shl;
        this.___shr = __shr;
        this.___ushr = __ushr;
        this.___abs = __abs;
        this.___zeroExtend = __zeroExtend;
        this.___signExtend = __signExtend;
        this.___narrow = __narrow;

        this.___hash = Objects.hash(__neg, __add, __sub, __mul, __div, __rem, __not, __and, __or, __xor, __shl, __shr, __ushr, __abs, __zeroExtend, __signExtend, __narrow);
    }

    @Override
    public int hashCode()
    {
        return this.___hash;
    }

    ///
    // Describes the unary negation operation.
    ///
    public ArithmeticOpTable.UnaryOp<ArithmeticOpTable.UnaryOp.Neg> getNeg()
    {
        return this.___neg;
    }

    ///
    // Describes the addition operation.
    ///
    public ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Add> getAdd()
    {
        return this.___add;
    }

    ///
    // Describes the subtraction operation.
    ///
    public ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Sub> getSub()
    {
        return this.___sub;
    }

    ///
    // Describes the multiplication operation.
    ///
    public ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Mul> getMul()
    {
        return this.___mul;
    }

    ///
    // Describes a signed operation that multiples the upper 32-bits of two long values.
    ///
    public ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.MulHigh> getMulHigh()
    {
        return this.___mulHigh;
    }

    ///
    // Describes an unsigned operation that multiples the upper 32-bits of two long values.
    ///
    public ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.UMulHigh> getUMulHigh()
    {
        return this.___umulHigh;
    }

    ///
    // Describes the division operation.
    ///
    public ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Div> getDiv()
    {
        return this.___div;
    }

    ///
    // Describes the remainder operation.
    ///
    public ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Rem> getRem()
    {
        return this.___rem;
    }

    ///
    // Describes the bitwise not operation.
    ///
    public ArithmeticOpTable.UnaryOp<ArithmeticOpTable.UnaryOp.Not> getNot()
    {
        return this.___not;
    }

    ///
    // Describes the bitwise and operation.
    ///
    public ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.And> getAnd()
    {
        return this.___and;
    }

    ///
    // Describes the bitwise or operation.
    ///
    public ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Or> getOr()
    {
        return this.___or;
    }

    ///
    // Describes the bitwise xor operation.
    ///
    public ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Xor> getXor()
    {
        return this.___xor;
    }

    ///
    // Describes the shift left operation.
    ///
    public ArithmeticOpTable.ShiftOp<ArithmeticOpTable.ShiftOp.Shl> getShl()
    {
        return this.___shl;
    }

    ///
    // Describes the signed shift right operation.
    ///
    public ArithmeticOpTable.ShiftOp<ArithmeticOpTable.ShiftOp.Shr> getShr()
    {
        return this.___shr;
    }

    ///
    // Describes the unsigned shift right operation.
    ///
    public ArithmeticOpTable.ShiftOp<ArithmeticOpTable.ShiftOp.UShr> getUShr()
    {
        return this.___ushr;
    }

    ///
    // Describes the absolute value operation.
    ///
    public ArithmeticOpTable.UnaryOp<ArithmeticOpTable.UnaryOp.Abs> getAbs()
    {
        return this.___abs;
    }

    ///
    // Describes the zero extend conversion.
    ///
    public ArithmeticOpTable.IntegerConvertOp<ArithmeticOpTable.IntegerConvertOp.ZeroExtend> getZeroExtend()
    {
        return this.___zeroExtend;
    }

    ///
    // Describes the sign extend conversion.
    ///
    public ArithmeticOpTable.IntegerConvertOp<ArithmeticOpTable.IntegerConvertOp.SignExtend> getSignExtend()
    {
        return this.___signExtend;
    }

    ///
    // Describes the narrowing conversion.
    ///
    public ArithmeticOpTable.IntegerConvertOp<ArithmeticOpTable.IntegerConvertOp.Narrow> getNarrow()
    {
        return this.___narrow;
    }

    private boolean opsEquals(ArithmeticOpTable __that)
    {
        return Objects.equals(this.___neg, __that.___neg) &&
               Objects.equals(this.___add, __that.___add) &&
               Objects.equals(this.___sub, __that.___sub) &&
               Objects.equals(this.___mul, __that.___mul) &&
               Objects.equals(this.___mulHigh, __that.___mulHigh) &&
               Objects.equals(this.___umulHigh, __that.___umulHigh) &&
               Objects.equals(this.___div, __that.___div) &&
               Objects.equals(this.___rem, __that.___rem) &&
               Objects.equals(this.___not, __that.___not) &&
               Objects.equals(this.___and, __that.___and) &&
               Objects.equals(this.___or, __that.___or) &&
               Objects.equals(this.___xor, __that.___xor) &&
               Objects.equals(this.___shl, __that.___shl) &&
               Objects.equals(this.___shr, __that.___shr) &&
               Objects.equals(this.___ushr, __that.___ushr) &&
               Objects.equals(this.___abs, __that.___abs) &&
               Objects.equals(this.___zeroExtend, __that.___zeroExtend) &&
               Objects.equals(this.___signExtend, __that.___signExtend) &&
               Objects.equals(this.___narrow, __that.___narrow);
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
            return true;
        }
        return false;
    }

    // @class ArithmeticOpTable.ArithmeticOp
    public abstract static class ArithmeticOp
    {
        // @field
        private final String ___operator;

        // @cons ArithmeticOpTable.ArithmeticOp
        protected ArithmeticOp(String __operator)
        {
            super();
            this.___operator = __operator;
        }

        @Override
        public int hashCode()
        {
            return this.___operator.hashCode();
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
            ArithmeticOpTable.ArithmeticOp __that = (ArithmeticOpTable.ArithmeticOp) __obj;
            if (this.___operator.equals(__that.___operator))
            {
                return true;
            }
            return true;
        }
    }

    ///
    // Describes a unary arithmetic operation.
    ///
    // @class ArithmeticOpTable.UnaryOp
    public abstract static class UnaryOp<T> extends ArithmeticOpTable.ArithmeticOp
    {
        // @class ArithmeticOpTable.UnaryOp.Neg
        public abstract static class Neg extends ArithmeticOpTable.UnaryOp<ArithmeticOpTable.UnaryOp.Neg>
        {
            // @cons ArithmeticOpTable.UnaryOp.Neg
            protected Neg()
            {
                super("-");
            }
        }

        // @class ArithmeticOpTable.UnaryOp.Not
        public abstract static class Not extends ArithmeticOpTable.UnaryOp<ArithmeticOpTable.UnaryOp.Not>
        {
            // @cons ArithmeticOpTable.UnaryOp.Not
            protected Not()
            {
                super("~");
            }
        }

        // @class ArithmeticOpTable.UnaryOp.Abs
        public abstract static class Abs extends ArithmeticOpTable.UnaryOp<ArithmeticOpTable.UnaryOp.Abs>
        {
            // @cons ArithmeticOpTable.UnaryOp.Abs
            protected Abs()
            {
                super("ABS");
            }
        }

        // @cons ArithmeticOpTable.UnaryOp
        protected UnaryOp(String __operation)
        {
            super(__operation);
        }

        ///
        // Apply the operation to a {@link Constant}.
        ///
        public abstract Constant foldConstant(Constant __value);

        ///
        // Apply the operation to a {@link Stamp}.
        ///
        public abstract Stamp foldStamp(Stamp __stamp);

        public ArithmeticOpTable.UnaryOp<T> unwrap()
        {
            return this;
        }
    }

    ///
    // Describes a binary arithmetic operation.
    ///
    // @class ArithmeticOpTable.BinaryOp
    public abstract static class BinaryOp<T> extends ArithmeticOpTable.ArithmeticOp
    {
        // @class ArithmeticOpTable.BinaryOp.Add
        public abstract static class Add extends ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Add>
        {
            // @cons ArithmeticOpTable.BinaryOp.Add
            protected Add(boolean __associative, boolean __commutative)
            {
                super("+", __associative, __commutative);
            }
        }

        // @class ArithmeticOpTable.BinaryOp.Sub
        public abstract static class Sub extends ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Sub>
        {
            // @cons ArithmeticOpTable.BinaryOp.Sub
            protected Sub(boolean __associative, boolean __commutative)
            {
                super("-", __associative, __commutative);
            }
        }

        // @class ArithmeticOpTable.BinaryOp.Mul
        public abstract static class Mul extends ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Mul>
        {
            // @cons ArithmeticOpTable.BinaryOp.Mul
            protected Mul(boolean __associative, boolean __commutative)
            {
                super("*", __associative, __commutative);
            }
        }

        // @class ArithmeticOpTable.BinaryOp.MulHigh
        public abstract static class MulHigh extends ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.MulHigh>
        {
            // @cons ArithmeticOpTable.BinaryOp.MulHigh
            protected MulHigh(boolean __associative, boolean __commutative)
            {
                super("*H", __associative, __commutative);
            }
        }

        // @class ArithmeticOpTable.BinaryOp.UMulHigh
        public abstract static class UMulHigh extends ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.UMulHigh>
        {
            // @cons ArithmeticOpTable.BinaryOp.UMulHigh
            protected UMulHigh(boolean __associative, boolean __commutative)
            {
                super("|*H|", __associative, __commutative);
            }
        }

        // @class ArithmeticOpTable.BinaryOp.Div
        public abstract static class Div extends ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Div>
        {
            // @cons ArithmeticOpTable.BinaryOp.Div
            protected Div(boolean __associative, boolean __commutative)
            {
                super("/", __associative, __commutative);
            }
        }

        // @class ArithmeticOpTable.BinaryOp.Rem
        public abstract static class Rem extends ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Rem>
        {
            // @cons ArithmeticOpTable.BinaryOp.Rem
            protected Rem(boolean __associative, boolean __commutative)
            {
                super("%", __associative, __commutative);
            }
        }

        // @class ArithmeticOpTable.BinaryOp.And
        public abstract static class And extends ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.And>
        {
            // @cons ArithmeticOpTable.BinaryOp.And
            protected And(boolean __associative, boolean __commutative)
            {
                super("&", __associative, __commutative);
            }
        }

        // @class ArithmeticOpTable.BinaryOp.Or
        public abstract static class Or extends ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Or>
        {
            // @cons ArithmeticOpTable.BinaryOp.Or
            protected Or(boolean __associative, boolean __commutative)
            {
                super("|", __associative, __commutative);
            }
        }

        // @class ArithmeticOpTable.BinaryOp.Xor
        public abstract static class Xor extends ArithmeticOpTable.BinaryOp<ArithmeticOpTable.BinaryOp.Xor>
        {
            // @cons ArithmeticOpTable.BinaryOp.Xor
            protected Xor(boolean __associative, boolean __commutative)
            {
                super("^", __associative, __commutative);
            }
        }

        // @field
        private final boolean ___associative;
        // @field
        private final boolean ___commutative;

        // @cons ArithmeticOpTable.BinaryOp
        protected BinaryOp(String __operation, boolean __associative, boolean __commutative)
        {
            super(__operation);
            this.___associative = __associative;
            this.___commutative = __commutative;
        }

        ///
        // Applies this operation to {@code a} and {@code b}.
        //
        // @return the result of applying this operation or {@code null} if applying it would raise
        //         an exception (e.g. {@link ArithmeticException} for dividing by 0)
        ///
        public abstract Constant foldConstant(Constant __a, Constant __b);

        ///
        // Apply the operation to two {@linkplain Stamp Stamps}.
        ///
        public abstract Stamp foldStamp(Stamp __a, Stamp __b);

        ///
        // Checks whether this operation is associative. An operation is associative when
        // {@code (a . b) . c == a . (b . c)} for all a, b, c. Note that you still have to be
        // careful with inverses. For example the integer subtraction operation will report {@code true}
        // here, since you can still reassociate as long as the correct negations are inserted.
        ///
        public final boolean isAssociative()
        {
            return this.___associative;
        }

        ///
        // Checks whether this operation is commutative. An operation is commutative when
        // {@code a . b == b . a} for all a, b.
        ///
        public final boolean isCommutative()
        {
            return this.___commutative;
        }

        ///
        // Check whether a {@link Constant} is a neutral element for this operation. A neutral
        // element is any element {@code n} where {@code a . n == a} for all a.
        //
        // @param n the {@link Constant} that should be tested
        // @return true iff for all {@code a}: {@code a . n == a}
        ///
        public boolean isNeutral(Constant __n)
        {
            return false;
        }

        ///
        // Check whether this operation has a zero {@code z == a . a} for each a. Examples of
        // operations having such an element are subtraction and exclusive-or. Note that this may be
        // different from the numbers tested by {@link #isNeutral}.
        //
        // @param stamp a {@link Stamp}
        // @return a unique {@code z} such that {@code z == a . a} for each {@code a} in
        //         {@code stamp} if it exists, otherwise {@code null}
        ///
        public Constant getZero(Stamp __stamp)
        {
            return null;
        }

        public ArithmeticOpTable.BinaryOp<T> unwrap()
        {
            return this;
        }

        @Override
        public int hashCode()
        {
            final int __prime = 31;
            int __result = super.hashCode();
            __result = __prime * __result + (this.___associative ? 1231 : 1237);
            __result = __prime * __result + (this.___commutative ? 1231 : 1237);
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
            ArithmeticOpTable.BinaryOp<?> __that = (ArithmeticOpTable.BinaryOp<?>) __obj;
            if (this.___associative != __that.___associative)
            {
                return false;
            }
            if (this.___commutative != __that.___commutative)
            {
                return false;
            }
            return true;
        }
    }

    ///
    // Describes a shift operation. The right argument of a shift operation always has kind
    // {@link JavaKind#Int}.
    ///
    // @class ArithmeticOpTable.ShiftOp
    public abstract static class ShiftOp<OP> extends ArithmeticOpTable.ArithmeticOp
    {
        // @class ArithmeticOpTable.ShiftOp.Shl
        public abstract static class Shl extends ArithmeticOpTable.ShiftOp<ArithmeticOpTable.ShiftOp.Shl>
        {
            // @cons ArithmeticOpTable.ShiftOp.Shl
            public Shl()
            {
                super("<<");
            }
        }

        // @class ArithmeticOpTable.ShiftOp.Shr
        public abstract static class Shr extends ArithmeticOpTable.ShiftOp<ArithmeticOpTable.ShiftOp.Shr>
        {
            // @cons ArithmeticOpTable.ShiftOp.Shr
            public Shr()
            {
                super(">>");
            }
        }

        // @class ArithmeticOpTable.ShiftOp.UShr
        public abstract static class UShr extends ArithmeticOpTable.ShiftOp<ArithmeticOpTable.ShiftOp.UShr>
        {
            // @cons ArithmeticOpTable.ShiftOp.UShr
            public UShr()
            {
                super(">>>");
            }
        }

        // @cons ArithmeticOpTable.ShiftOp
        protected ShiftOp(String __operation)
        {
            super(__operation);
        }

        ///
        // Apply the shift to a constant.
        ///
        public abstract Constant foldConstant(Constant __c, int __amount);

        ///
        // Apply the shift to a stamp.
        ///
        public abstract Stamp foldStamp(Stamp __s, IntegerStamp __amount);

        ///
        // Get the shift amount mask for a given result stamp.
        ///
        public abstract int getShiftAmountMask(Stamp __s);
    }

    // @class ArithmeticOpTable.IntegerConvertOp
    public abstract static class IntegerConvertOp<T> extends ArithmeticOpTable.ArithmeticOp
    {
        // @class ArithmeticOpTable.IntegerConvertOp.ZeroExtend
        public abstract static class ZeroExtend extends ArithmeticOpTable.IntegerConvertOp<ArithmeticOpTable.IntegerConvertOp.ZeroExtend>
        {
            // @cons ArithmeticOpTable.IntegerConvertOp.ZeroExtend
            protected ZeroExtend()
            {
                super("ZeroExtend");
            }
        }

        // @class ArithmeticOpTable.IntegerConvertOp.SignExtend
        public abstract static class SignExtend extends ArithmeticOpTable.IntegerConvertOp<ArithmeticOpTable.IntegerConvertOp.SignExtend>
        {
            // @cons ArithmeticOpTable.IntegerConvertOp.SignExtend
            protected SignExtend()
            {
                super("SignExtend");
            }
        }

        // @class ArithmeticOpTable.IntegerConvertOp.Narrow
        public abstract static class Narrow extends ArithmeticOpTable.IntegerConvertOp<ArithmeticOpTable.IntegerConvertOp.Narrow>
        {
            // @cons ArithmeticOpTable.IntegerConvertOp.Narrow
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

        // @cons ArithmeticOpTable.IntegerConvertOp
        protected IntegerConvertOp(String __op)
        {
            super(__op);
        }

        public abstract Constant foldConstant(int __inputBits, int __resultBits, Constant __value);

        public abstract Stamp foldStamp(int __inputBits, int __resultBits, Stamp __stamp);

        public ArithmeticOpTable.IntegerConvertOp<T> unwrap()
        {
            return this;
        }

        ///
        // Computes the stamp of the input for the given output stamp.
        ///
        public abstract Stamp invertStamp(int __inputBits, int __resultBits, Stamp __outStamp);
    }
}
