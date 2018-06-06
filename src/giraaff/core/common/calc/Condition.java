package giraaff.core.common.calc;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.ConstantReflectionProvider;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.PrimitiveConstant;

import giraaff.util.GraalError;

///
// Condition codes used in conditionals.
///
// @enum Condition
public enum Condition
{
    ///
    // Equal.
    ///
    EQ("=="),

    ///
    // Not equal.
    ///
    NE("!="),

    ///
    // Signed less than.
    ///
    LT("<"),

    ///
    // Signed less than or equal.
    ///
    LE("<="),

    ///
    // Signed greater than.
    ///
    GT(">"),

    ///
    // Signed greater than or equal.
    ///
    GE(">="),

    ///
    // Unsigned greater than or equal ("above than or equal").
    ///
    AE("|>=|"),

    ///
    // Unsigned less than or equal ("below than or equal").
    ///
    BE("|<=|"),

    ///
    // Unsigned greater than ("above than").
    ///
    AT("|>|"),

    ///
    // Unsigned less than ("below than").
    ///
    BT("|<|");

    // @field
    public final String ___operator;

    // @cons Condition
    Condition(String __operator)
    {
        this.___operator = __operator;
    }

    // @class Condition.CanonicalizedCondition
    public static final class CanonicalizedCondition
    {
        // @field
        private final CanonicalCondition ___canonicalCondition;
        // @field
        private final boolean ___mirror;
        // @field
        private final boolean ___negate;

        // @cons Condition.CanonicalizedCondition
        private CanonicalizedCondition(CanonicalCondition __canonicalCondition, boolean __mirror, boolean __negate)
        {
            super();
            this.___canonicalCondition = __canonicalCondition;
            this.___mirror = __mirror;
            this.___negate = __negate;
        }

        public CanonicalCondition getCanonicalCondition()
        {
            return this.___canonicalCondition;
        }

        public boolean mustMirror()
        {
            return this.___mirror;
        }

        public boolean mustNegate()
        {
            return this.___negate;
        }
    }

    public Condition.CanonicalizedCondition canonicalize()
    {
        CanonicalCondition __canonicalCondition;
        switch (this)
        {
            case EQ:
            case NE:
            {
                __canonicalCondition = CanonicalCondition.EQ;
                break;
            }
            case LT:
            case LE:
            case GT:
            case GE:
            {
                __canonicalCondition = CanonicalCondition.LT;
                break;
            }
            case BT:
            case BE:
            case AT:
            case AE:
            {
                __canonicalCondition = CanonicalCondition.BT;
                break;
            }
            default:
                throw GraalError.shouldNotReachHere();
        }
        return new Condition.CanonicalizedCondition(__canonicalCondition, canonicalMirror(), canonicalNegate());
    }

    ///
    // Given a condition and its negation, this method returns true for one of the two and false for
    // the other one. This can be used to keep comparisons in a canonical form.
    //
    // @return true if this condition is considered to be the canonical form, false otherwise.
    ///
    public boolean isCanonical()
    {
        switch (this)
        {
            case EQ:
                return true;
            case NE:
                return false;
            case LT:
                return true;
            case LE:
                return false;
            case GT:
                return false;
            case GE:
                return false;
            case BT:
                return true;
            case BE:
                return false;
            case AT:
                return false;
            case AE:
                return false;
        }
        throw GraalError.shouldNotReachHere();
    }

    ///
    // Returns true if the condition needs to be mirrored to get to a canonical condition. The
    // result of the mirroring operation might still need to be negated to achieve a canonical form.
    ///
    private boolean canonicalMirror()
    {
        switch (this)
        {
            case EQ:
                return false;
            case NE:
                return false;
            case LT:
                return false;
            case LE:
                return true;
            case GT:
                return true;
            case GE:
                return false;
            case BT:
                return false;
            case BE:
                return true;
            case AT:
                return true;
            case AE:
                return false;
        }
        throw GraalError.shouldNotReachHere();
    }

    ///
    // Returns true if the condition needs to be negated to get to a canonical condition. The result
    // of the negation might still need to be mirrored to achieve a canonical form.
    ///
    private boolean canonicalNegate()
    {
        switch (this)
        {
            case EQ:
                return false;
            case NE:
                return true;
            case LT:
                return false;
            case LE:
                return true;
            case GT:
                return false;
            case GE:
                return true;
            case BT:
                return false;
            case BE:
                return true;
            case AT:
                return false;
            case AE:
                return true;
        }
        throw GraalError.shouldNotReachHere();
    }

    ///
    // Negate this conditional.
    //
    // @return the condition that represents the negation
    ///
    public final Condition negate()
    {
        switch (this)
        {
            case EQ:
                return NE;
            case NE:
                return EQ;
            case LT:
                return GE;
            case LE:
                return GT;
            case GT:
                return LE;
            case GE:
                return LT;
            case BT:
                return AE;
            case BE:
                return AT;
            case AT:
                return BE;
            case AE:
                return BT;
        }
        throw GraalError.shouldNotReachHere();
    }

    public boolean implies(Condition __other)
    {
        if (__other == this)
        {
            return true;
        }
        switch (this)
        {
            case EQ:
                return __other == LE || __other == GE || __other == BE || __other == AE;
            case NE:
                return false;
            case LT:
                return __other == LE || __other == NE;
            case LE:
                return false;
            case GT:
                return __other == GE || __other == NE;
            case GE:
                return false;
            case BT:
                return __other == BE || __other == NE;
            case BE:
                return false;
            case AT:
                return __other == AE || __other == NE;
            case AE:
                return false;
        }
        throw GraalError.shouldNotReachHere();
    }

    ///
    // Mirror this conditional (i.e. commute "a op b" to "b op' a")
    //
    // @return the condition representing the equivalent commuted operation
    ///
    public final Condition mirror()
    {
        switch (this)
        {
            case EQ:
                return EQ;
            case NE:
                return NE;
            case LT:
                return GT;
            case LE:
                return GE;
            case GT:
                return LT;
            case GE:
                return LE;
            case BT:
                return AT;
            case BE:
                return AE;
            case AT:
                return BT;
            case AE:
                return BE;
        }
        throw new IllegalArgumentException();
    }

    ///
    // Returns true if this condition represents an unsigned comparison. EQ and NE are not
    // considered to be unsigned.
    ///
    public final boolean isUnsigned()
    {
        return this == Condition.BT || this == Condition.BE || this == Condition.AT || this == Condition.AE;
    }

    ///
    // Checks if this conditional operation is commutative.
    //
    // @return {@code true} if this operation is commutative
    ///
    public final boolean isCommutative()
    {
        return this == EQ || this == NE;
    }

    ///
    // Attempts to fold a comparison between two constants and return the result.
    //
    // @param lt the constant on the left side of the comparison
    // @param rt the constant on the right side of the comparison
    // @param constantReflection needed to compare constants
    // @return {@link Boolean#TRUE} if the comparison is known to be true, {@link Boolean#FALSE} if
    //         the comparison is known to be false
    ///
    public boolean foldCondition(Constant __lt, Constant __rt, ConstantReflectionProvider __constantReflection)
    {
        if (__lt instanceof PrimitiveConstant)
        {
            PrimitiveConstant __lp = (PrimitiveConstant) __lt;
            PrimitiveConstant __rp = (PrimitiveConstant) __rt;
            return foldCondition(__lp, __rp);
        }
        else
        {
            Boolean __equal = __constantReflection.constantEquals(__lt, __rt);
            if (__equal == null)
            {
                throw new GraalError("could not fold %s %s %s", __lt, this, __rt);
            }
            switch (this)
            {
                case EQ:
                    return __equal.booleanValue();
                case NE:
                    return !__equal.booleanValue();
                default:
                    throw new GraalError("expected condition: %s", this);
            }
        }
    }

    ///
    // Attempts to fold a comparison between two primitive constants and return the result.
    //
    // @param lp the constant on the left side of the comparison
    // @param rp the constant on the right side of the comparison
    // @return true if the comparison is known to be true, false if the comparison is known to be false
    ///
    public boolean foldCondition(PrimitiveConstant __lp, PrimitiveConstant __rp)
    {
        switch (__lp.getJavaKind())
        {
            case Boolean:
            case Byte:
            case Char:
            case Short:
            case Int:
            {
                int __x = __lp.asInt();
                int __y = __rp.asInt();
                switch (this)
                {
                    case EQ:
                        return __x == __y;
                    case NE:
                        return __x != __y;
                    case LT:
                        return __x < __y;
                    case LE:
                        return __x <= __y;
                    case GT:
                        return __x > __y;
                    case GE:
                        return __x >= __y;
                    case AE:
                        return UnsignedMath.aboveOrEqual(__x, __y);
                    case BE:
                        return UnsignedMath.belowOrEqual(__x, __y);
                    case AT:
                        return UnsignedMath.aboveThan(__x, __y);
                    case BT:
                        return UnsignedMath.belowThan(__x, __y);
                    default:
                        throw new GraalError("expected condition: %s", this);
                }
            }
            case Long:
            {
                long __x = __lp.asLong();
                long __y = __rp.asLong();
                switch (this)
                {
                    case EQ:
                        return __x == __y;
                    case NE:
                        return __x != __y;
                    case LT:
                        return __x < __y;
                    case LE:
                        return __x <= __y;
                    case GT:
                        return __x > __y;
                    case GE:
                        return __x >= __y;
                    case AE:
                        return UnsignedMath.aboveOrEqual(__x, __y);
                    case BE:
                        return UnsignedMath.belowOrEqual(__x, __y);
                    case AT:
                        return UnsignedMath.aboveThan(__x, __y);
                    case BT:
                        return UnsignedMath.belowThan(__x, __y);
                    default:
                        throw new GraalError("expected condition: %s", this);
                }
            }
            default:
                throw new GraalError("expected value kind %s while folding condition: %s", __lp.getJavaKind(), this);
        }
    }

    public Condition join(Condition __other)
    {
        if (__other == this)
        {
            return this;
        }
        switch (this)
        {
            case EQ:
                if (__other == LE || __other == GE || __other == BE || __other == AE)
                {
                    return EQ;
                }
                else
                {
                    return null;
                }
            case NE:
                if (__other == LT || __other == GT || __other == BT || __other == AT)
                {
                    return __other;
                }
                else if (__other == LE)
                {
                    return LT;
                }
                else if (__other == GE)
                {
                    return GT;
                }
                else if (__other == BE)
                {
                    return BT;
                }
                else if (__other == AE)
                {
                    return AT;
                }
                else
                {
                    return null;
                }
            case LE:
                if (__other == GE || __other == EQ)
                {
                    return EQ;
                }
                else if (__other == NE || __other == LT)
                {
                    return LT;
                }
                else
                {
                    return null;
                }
            case LT:
                if (__other == NE || __other == LE)
                {
                    return LT;
                }
                else
                {
                    return null;
                }
            case GE:
                if (__other == LE || __other == EQ)
                {
                    return EQ;
                }
                else if (__other == NE || __other == GT)
                {
                    return GT;
                }
                else
                {
                    return null;
                }
            case GT:
                if (__other == NE || __other == GE)
                {
                    return GT;
                }
                else
                {
                    return null;
                }
            case BE:
                if (__other == AE || __other == EQ)
                {
                    return EQ;
                }
                else if (__other == NE || __other == BT)
                {
                    return BT;
                }
                else
                {
                    return null;
                }
            case BT:
                if (__other == NE || __other == BE)
                {
                    return BT;
                }
                else
                {
                    return null;
                }
            case AE:
                if (__other == BE || __other == EQ)
                {
                    return EQ;
                }
                else if (__other == NE || __other == AT)
                {
                    return AT;
                }
                else
                {
                    return null;
                }
            case AT:
                if (__other == NE || __other == AE)
                {
                    return AT;
                }
                else
                {
                    return null;
                }
        }
        throw GraalError.shouldNotReachHere();
    }

    public Condition meet(Condition __other)
    {
        if (__other == this)
        {
            return this;
        }
        switch (this)
        {
            case EQ:
                if (__other == LE || __other == GE || __other == BE || __other == AE)
                {
                    return __other;
                }
                else if (__other == LT)
                {
                    return LE;
                }
                else if (__other == GT)
                {
                    return GE;
                }
                else if (__other == BT)
                {
                    return BE;
                }
                else if (__other == AT)
                {
                    return AE;
                }
                else
                {
                    return null;
                }
            case NE:
                if (__other == LT || __other == GT || __other == BT || __other == AT)
                {
                    return NE;
                }
                else
                {
                    return null;
                }
            case LE:
                if (__other == EQ || __other == LT)
                {
                    return LE;
                }
                else
                {
                    return null;
                }
            case LT:
                if (__other == EQ || __other == LE)
                {
                    return LE;
                }
                else if (__other == NE || __other == GT)
                {
                    return NE;
                }
                else
                {
                    return null;
                }
            case GE:
                if (__other == EQ || __other == GT)
                {
                    return GE;
                }
                else
                {
                    return null;
                }
            case GT:
                if (__other == EQ || __other == GE)
                {
                    return GE;
                }
                else if (__other == NE || __other == LT)
                {
                    return NE;
                }
                else
                {
                    return null;
                }
            case BE:
                if (__other == EQ || __other == BT)
                {
                    return BE;
                }
                else
                {
                    return null;
                }
            case BT:
                if (__other == EQ || __other == BE)
                {
                    return BE;
                }
                else if (__other == NE || __other == AT)
                {
                    return NE;
                }
                else
                {
                    return null;
                }
            case AE:
                if (__other == EQ || __other == AT)
                {
                    return AE;
                }
                else
                {
                    return null;
                }
            case AT:
                if (__other == EQ || __other == AE)
                {
                    return AE;
                }
                else if (__other == NE || __other == BT)
                {
                    return NE;
                }
                else
                {
                    return null;
                }
        }
        throw GraalError.shouldNotReachHere();
    }
}
