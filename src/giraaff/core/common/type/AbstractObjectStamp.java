package giraaff.core.common.type;

import java.util.AbstractList;
import java.util.Objects;
import java.util.RandomAccess;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaType;

/**
 * Type describing all pointers to Java objects.
 */
// @class AbstractObjectStamp
public abstract class AbstractObjectStamp extends AbstractPointerStamp
{
    // @field
    private final ResolvedJavaType type;
    // @field
    private final boolean exactType;

    // @cons
    protected AbstractObjectStamp(ResolvedJavaType __type, boolean __exactType, boolean __nonNull, boolean __alwaysNull)
    {
        super(__nonNull, __alwaysNull);
        this.type = __type;
        this.exactType = __exactType;
    }

    protected abstract AbstractObjectStamp copyWith(ResolvedJavaType newType, boolean newExactType, boolean newNonNull, boolean newAlwaysNull);

    @Override
    protected final AbstractPointerStamp copyWith(boolean __newNonNull, boolean __newAlwaysNull)
    {
        return copyWith(type, exactType, __newNonNull, __newAlwaysNull);
    }

    @Override
    public Stamp unrestricted()
    {
        return copyWith(null, false, false, false);
    }

    @Override
    public Stamp empty()
    {
        return copyWith(null, true, true, false);
    }

    @Override
    public Stamp constant(Constant __c, MetaAccessProvider __meta)
    {
        JavaConstant __jc = (JavaConstant) __c;
        ResolvedJavaType __constType = __jc.isNull() ? null : __meta.lookupJavaType(__jc);
        return copyWith(__constType, __jc.isNonNull(), __jc.isNonNull(), __jc.isNull());
    }

    @Override
    public boolean hasValues()
    {
        return !exactType || (type != null && (isConcreteType(type)));
    }

    @Override
    public JavaKind getStackKind()
    {
        return JavaKind.Object;
    }

    @Override
    public ResolvedJavaType javaType(MetaAccessProvider __metaAccess)
    {
        if (type != null)
        {
            return type;
        }
        return __metaAccess.lookupJavaType(Object.class);
    }

    public ResolvedJavaType type()
    {
        return type;
    }

    public boolean isExactType()
    {
        return exactType && type != null;
    }

    @Override
    public Stamp meet(Stamp __otherStamp)
    {
        if (this == __otherStamp)
        {
            return this;
        }
        AbstractObjectStamp __other = (AbstractObjectStamp) __otherStamp;
        if (isEmpty())
        {
            return __other;
        }
        else if (__other.isEmpty())
        {
            return this;
        }
        ResolvedJavaType __meetType;
        boolean __meetExactType;
        boolean __meetNonNull;
        boolean __meetAlwaysNull;
        if (__other.alwaysNull())
        {
            __meetType = type();
            __meetExactType = exactType;
            __meetNonNull = false;
            __meetAlwaysNull = alwaysNull();
        }
        else if (alwaysNull())
        {
            __meetType = __other.type();
            __meetExactType = __other.exactType;
            __meetNonNull = false;
            __meetAlwaysNull = __other.alwaysNull();
        }
        else
        {
            __meetType = meetTypes(type(), __other.type());
            __meetExactType = exactType && __other.exactType;
            if (__meetExactType && type != null && __other.type != null)
            {
                // meeting two valid exact types may result in a non-exact type
                __meetExactType = Objects.equals(__meetType, type) && Objects.equals(__meetType, __other.type);
            }
            __meetNonNull = nonNull() && __other.nonNull();
            __meetAlwaysNull = false;
        }

        if (Objects.equals(__meetType, type) && __meetExactType == exactType && __meetNonNull == nonNull() && __meetAlwaysNull == alwaysNull())
        {
            return this;
        }
        else if (Objects.equals(__meetType, __other.type) && __meetExactType == __other.exactType && __meetNonNull == __other.nonNull() && __meetAlwaysNull == __other.alwaysNull())
        {
            return __other;
        }
        else
        {
            return copyWith(__meetType, __meetExactType, __meetNonNull, __meetAlwaysNull);
        }
    }

    @Override
    public Stamp join(Stamp __otherStamp)
    {
        return join0(__otherStamp, false);
    }

    /**
     * Returns the stamp representing the type of this stamp after a cast to the type represented by
     * the {@code to} stamp. While this is very similar to a {@link #join} operation, in the case
     * where both types are not obviously related, the cast operation will prefer the type of the
     * {@code to} stamp. This is necessary as long as ObjectStamps are not able to accurately
     * represent intersection types.
     *
     * For example when joining the {@link RandomAccess} type with the {@link AbstractList} type,
     * without intersection types, this would result in the most generic type ({@link Object} ). For
     * this reason, in some cases a {@code castTo} operation is preferable in order to keep at least
     * the {@link AbstractList} type.
     *
     * @param other the stamp this stamp should be casted to
     * @return the new improved stamp or {@code null} if this stamp cannot be improved
     */
    @Override
    public Stamp improveWith(Stamp __other)
    {
        return join0(__other, true);
    }

    private Stamp join0(Stamp __otherStamp, boolean __improve)
    {
        if (this == __otherStamp)
        {
            return this;
        }
        AbstractObjectStamp __other = (AbstractObjectStamp) __otherStamp;
        if (isEmpty())
        {
            return this;
        }
        else if (__other.isEmpty())
        {
            return __other;
        }

        ResolvedJavaType __joinType;
        boolean __joinAlwaysNull = alwaysNull() || __other.alwaysNull();
        boolean __joinNonNull = nonNull() || __other.nonNull();
        boolean __joinExactType = exactType || __other.exactType;
        if (Objects.equals(type, __other.type))
        {
            __joinType = type;
        }
        else if (type == null)
        {
            __joinType = __other.type;
        }
        else if (__other.type == null)
        {
            __joinType = type;
        }
        else
        {
            // both types are != null and different
            if (type.isAssignableFrom(__other.type))
            {
                __joinType = __other.type;
                if (exactType)
                {
                    __joinAlwaysNull = true;
                }
            }
            else if (__other.type.isAssignableFrom(type))
            {
                __joinType = type;
                if (__other.exactType)
                {
                    __joinAlwaysNull = true;
                }
            }
            else
            {
                if (__improve)
                {
                    __joinType = type;
                    __joinExactType = exactType;
                }
                else
                {
                    __joinType = null;
                }

                if (__joinExactType || (!isInterfaceOrArrayOfInterface(type) && !isInterfaceOrArrayOfInterface(__other.type)))
                {
                    __joinAlwaysNull = true;
                }
            }
        }
        if (__joinAlwaysNull)
        {
            __joinType = null;
            __joinExactType = false;
        }
        if (__joinExactType && __joinType == null)
        {
            return empty();
        }
        if (__joinAlwaysNull && __joinNonNull)
        {
            return empty();
        }
        else if (__joinExactType && !isConcreteType(__joinType))
        {
            return empty();
        }
        if (Objects.equals(__joinType, type) && __joinExactType == exactType && __joinNonNull == nonNull() && __joinAlwaysNull == alwaysNull())
        {
            return this;
        }
        else if (Objects.equals(__joinType, __other.type) && __joinExactType == __other.exactType && __joinNonNull == __other.nonNull() && __joinAlwaysNull == __other.alwaysNull())
        {
            return __other;
        }
        else
        {
            return copyWith(__joinType, __joinExactType, __joinNonNull, __joinAlwaysNull);
        }
    }

    private static boolean isInterfaceOrArrayOfInterface(ResolvedJavaType __t)
    {
        return __t.isInterface() || (__t.isArray() && __t.getElementalType().isInterface());
    }

    public static boolean isConcreteType(ResolvedJavaType __type)
    {
        return !(__type.isAbstract() && !__type.isArray());
    }

    private static ResolvedJavaType meetTypes(ResolvedJavaType __a, ResolvedJavaType __b)
    {
        if (Objects.equals(__a, __b))
        {
            return __a;
        }
        else if (__a == null || __b == null)
        {
            return null;
        }
        else
        {
            // The 'meetTypes' operation must be commutative. One way to achieve this is
            // to totally order the types and always call 'meetOrderedNonNullTypes' in the
            // same order. We establish the order by first comparing the hash-codes for
            // performance reasons, and then comparing the internal names of the types.
            int __hashA = __a.getName().hashCode();
            int __hashB = __b.getName().hashCode();
            if (__hashA < __hashB)
            {
                return meetOrderedNonNullTypes(__a, __b);
            }
            else if (__hashB < __hashA)
            {
                return meetOrderedNonNullTypes(__b, __a);
            }
            else
            {
                int __diff = __a.getName().compareTo(__b.getName());
                if (__diff <= 0)
                {
                    return meetOrderedNonNullTypes(__a, __b);
                }
                else
                {
                    return meetOrderedNonNullTypes(__b, __a);
                }
            }
        }
    }

    private static ResolvedJavaType meetOrderedNonNullTypes(ResolvedJavaType __a, ResolvedJavaType __b)
    {
        ResolvedJavaType __result = __a.findLeastCommonAncestor(__b);
        if (__result.isJavaLangObject() && __a.isInterface() && __b.isInterface())
        {
            // Both types are incompatible interfaces => search for first possible common
            // ancestor match among super interfaces.
            ResolvedJavaType[] __interfacesA = __a.getInterfaces();
            ResolvedJavaType[] __interfacesB = __b.getInterfaces();
            for (int __i = 0; __i < __interfacesA.length; ++__i)
            {
                ResolvedJavaType __interface1 = __interfacesA[__i];
                for (int __j = 0; __j < __interfacesB.length; ++__j)
                {
                    ResolvedJavaType __interface2 = __interfacesB[__j];
                    ResolvedJavaType __leastCommon = meetTypes(__interface1, __interface2);
                    if (__leastCommon.isInterface())
                    {
                        return __leastCommon;
                    }
                }
            }
        }
        return __result;
    }

    @Override
    public int hashCode()
    {
        final int __prime = 31;
        int __result = 1;
        __result = __prime * __result + super.hashCode();
        __result = __prime * __result + (exactType ? 1231 : 1237);
        __result = __prime * __result + ((type == null || type.isJavaLangObject()) ? 0 : type.hashCode());
        return __result;
    }

    @Override
    public boolean equals(Object __obj)
    {
        if (this == __obj)
        {
            return true;
        }
        if (__obj == null || getClass() != __obj.getClass())
        {
            return false;
        }
        AbstractObjectStamp __other = (AbstractObjectStamp) __obj;
        if (exactType != __other.exactType)
        {
            return false;
        }
        // null == java.lang.Object
        if (type == null)
        {
            if (__other.type != null && !__other.type.isJavaLangObject())
            {
                return false;
            }
        }
        else if (__other.type == null)
        {
            if (type != null && !type.isJavaLangObject())
            {
                return false;
            }
        }
        else if (!type.equals(__other.type))
        {
            return false;
        }
        return super.equals(__other);
    }
}
