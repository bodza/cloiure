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

///
// Information about arithmetic operations.
///
// @class ArithmeticOpTable
public final class ArithmeticOpTable
{
    // @field
    private final UnaryOp<Neg> ___neg;
    // @field
    private final BinaryOp<Add> ___add;
    // @field
    private final BinaryOp<Sub> ___sub;

    // @field
    private final BinaryOp<Mul> ___mul;
    // @field
    private final BinaryOp<MulHigh> ___mulHigh;
    // @field
    private final BinaryOp<UMulHigh> ___umulHigh;
    // @field
    private final BinaryOp<Div> ___div;
    // @field
    private final BinaryOp<Rem> ___rem;

    // @field
    private final UnaryOp<Not> ___not;
    // @field
    private final BinaryOp<And> ___and;
    // @field
    private final BinaryOp<Or> ___or;
    // @field
    private final BinaryOp<Xor> ___xor;

    // @field
    private final ShiftOp<Shl> ___shl;
    // @field
    private final ShiftOp<Shr> ___shr;
    // @field
    private final ShiftOp<UShr> ___ushr;

    // @field
    private final UnaryOp<Abs> ___abs;

    // @field
    private final IntegerConvertOp<ZeroExtend> ___zeroExtend;
    // @field
    private final IntegerConvertOp<SignExtend> ___signExtend;
    // @field
    private final IntegerConvertOp<Narrow> ___narrow;

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

    public BinaryOp<?>[] getBinaryOps()
    {
        return new BinaryOp<?>[] { this.___add, this.___sub, this.___mul, this.___mulHigh, this.___umulHigh, this.___div, this.___rem, this.___and, this.___or, this.___xor };
    }

    public UnaryOp<?>[] getUnaryOps()
    {
        return new UnaryOp<?>[] { this.___neg, this.___not, this.___abs };
    }

    public ShiftOp<?>[] getShiftOps()
    {
        return new ShiftOp<?>[] { this.___shl, this.___shr, this.___ushr };
    }

    public IntegerConvertOp<?>[] getIntegerConvertOps()
    {
        return new IntegerConvertOp<?>[] { this.___zeroExtend, this.___signExtend, this.___narrow };
    }

    // @def
    public static final ArithmeticOpTable EMPTY = new ArithmeticOpTable(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

    // @iface ArithmeticOpTable.ArithmeticOpWrapper
    public interface ArithmeticOpWrapper
    {
        <OP> UnaryOp<OP> wrapUnaryOp(UnaryOp<OP> __op);

        <OP> BinaryOp<OP> wrapBinaryOp(BinaryOp<OP> __op);

        <OP> ShiftOp<OP> wrapShiftOp(ShiftOp<OP> __op);

        <OP> IntegerConvertOp<OP> wrapIntegerConvertOp(IntegerConvertOp<OP> __op);
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

        IntegerConvertOp<ZeroExtend> __zeroExtend = wrapIfNonNull(__wrapper::wrapIntegerConvertOp, __inner.getZeroExtend());
        IntegerConvertOp<SignExtend> __signExtend = wrapIfNonNull(__wrapper::wrapIntegerConvertOp, __inner.getSignExtend());
        IntegerConvertOp<Narrow> __narrow = wrapIfNonNull(__wrapper::wrapIntegerConvertOp, __inner.getNarrow());

        return new ArithmeticOpTable(__neg, __add, __sub, __mul, __mulHigh, __umulHigh, __div, __rem, __not, __and, __or, __xor, __shl, __shr, __ushr, __abs, __zeroExtend, __signExtend, __narrow);
    }

    // @cons
    protected ArithmeticOpTable(UnaryOp<Neg> __neg, BinaryOp<Add> __add, BinaryOp<Sub> __sub, BinaryOp<Mul> __mul, BinaryOp<MulHigh> __mulHigh, BinaryOp<UMulHigh> __umulHigh, BinaryOp<Div> __div, BinaryOp<Rem> __rem, UnaryOp<Not> __not, BinaryOp<And> __and, BinaryOp<Or> __or, BinaryOp<Xor> __xor, ShiftOp<Shl> __shl, ShiftOp<Shr> __shr, ShiftOp<UShr> __ushr, UnaryOp<Abs> __abs, IntegerConvertOp<ZeroExtend> __zeroExtend, IntegerConvertOp<SignExtend> __signExtend, IntegerConvertOp<Narrow> __narrow)
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
    public UnaryOp<Neg> getNeg()
    {
        return this.___neg;
    }

    ///
    // Describes the addition operation.
    ///
    public BinaryOp<Add> getAdd()
    {
        return this.___add;
    }

    ///
    // Describes the subtraction operation.
    ///
    public BinaryOp<Sub> getSub()
    {
        return this.___sub;
    }

    ///
    // Describes the multiplication operation.
    ///
    public BinaryOp<Mul> getMul()
    {
        return this.___mul;
    }

    ///
    // Describes a signed operation that multiples the upper 32-bits of two long values.
    ///
    public BinaryOp<MulHigh> getMulHigh()
    {
        return this.___mulHigh;
    }

    ///
    // Describes an unsigned operation that multiples the upper 32-bits of two long values.
    ///
    public BinaryOp<UMulHigh> getUMulHigh()
    {
        return this.___umulHigh;
    }

    ///
    // Describes the division operation.
    ///
    public BinaryOp<Div> getDiv()
    {
        return this.___div;
    }

    ///
    // Describes the remainder operation.
    ///
    public BinaryOp<Rem> getRem()
    {
        return this.___rem;
    }

    ///
    // Describes the bitwise not operation.
    ///
    public UnaryOp<Not> getNot()
    {
        return this.___not;
    }

    ///
    // Describes the bitwise and operation.
    ///
    public BinaryOp<And> getAnd()
    {
        return this.___and;
    }

    ///
    // Describes the bitwise or operation.
    ///
    public BinaryOp<Or> getOr()
    {
        return this.___or;
    }

    ///
    // Describes the bitwise xor operation.
    ///
    public BinaryOp<Xor> getXor()
    {
        return this.___xor;
    }

    ///
    // Describes the shift left operation.
    ///
    public ShiftOp<Shl> getShl()
    {
        return this.___shl;
    }

    ///
    // Describes the signed shift right operation.
    ///
    public ShiftOp<Shr> getShr()
    {
        return this.___shr;
    }

    ///
    // Describes the unsigned shift right operation.
    ///
    public ShiftOp<UShr> getUShr()
    {
        return this.___ushr;
    }

    ///
    // Describes the absolute value operation.
    ///
    public UnaryOp<Abs> getAbs()
    {
        return this.___abs;
    }

    ///
    // Describes the zero extend conversion.
    ///
    public IntegerConvertOp<ZeroExtend> getZeroExtend()
    {
        return this.___zeroExtend;
    }

    ///
    // Describes the sign extend conversion.
    ///
    public IntegerConvertOp<SignExtend> getSignExtend()
    {
        return this.___signExtend;
    }

    ///
    // Describes the narrowing conversion.
    ///
    public IntegerConvertOp<Narrow> getNarrow()
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

    // @class ArithmeticOpTable.Op
    public abstract static class Op
    {
        // @field
        private final String ___operator;

        // @cons
        protected Op(String __operator)
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
            Op __that = (Op) __obj;
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

        // @cons
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

        public UnaryOp<T> unwrap()
        {
            return this;
        }
    }

    ///
    // Describes a binary arithmetic operation.
    ///
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
        private final boolean ___associative;
        // @field
        private final boolean ___commutative;

        // @cons
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

        public BinaryOp<T> unwrap()
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
            BinaryOp<?> __that = (BinaryOp<?>) __obj;
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

        public abstract Constant foldConstant(int __inputBits, int __resultBits, Constant __value);

        public abstract Stamp foldStamp(int __inputBits, int __resultBits, Stamp __stamp);

        public IntegerConvertOp<T> unwrap()
        {
            return this;
        }

        ///
        // Computes the stamp of the input for the given output stamp.
        ///
        public abstract Stamp invertStamp(int __inputBits, int __resultBits, Stamp __outStamp);
    }
}
