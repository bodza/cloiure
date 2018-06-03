package giraaff.core.common.type;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.meta.Assumptions;
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.meta.Signature;

import giraaff.core.common.NumUtil;
import giraaff.util.GraalError;

// @class StampFactory
public final class StampFactory
{
    // @cons
    private StampFactory()
    {
        super();
    }

    // @def
    private static final Stamp[] stampCache = new Stamp[JavaKind.values().length];
    // @def
    private static final Stamp[] emptyStampCache = new Stamp[JavaKind.values().length];
    // @def
    private static final Stamp objectStamp = new ObjectStamp(null, false, false, false);
    // @def
    private static final Stamp objectNonNullStamp = new ObjectStamp(null, false, true, false);
    // @def
    private static final Stamp objectAlwaysNullStamp = new ObjectStamp(null, false, false, true);
    // @def
    private static final Stamp positiveInt = forInteger(JavaKind.Int, 0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
    // @def
    private static final Stamp booleanTrue = forInteger(JavaKind.Boolean, -1, -1, 1, 1);
    // @def
    private static final Stamp booleanFalse = forInteger(JavaKind.Boolean, 0, 0, 0, 0);
    // @def
    private static final Stamp rawPointer = new RawPointerStamp();

    private static void setCache(JavaKind __kind, Stamp __stamp)
    {
        stampCache[__kind.ordinal()] = __stamp;
    }

    private static void setIntCache(JavaKind __kind)
    {
        int __bits = __kind.getStackKind().getBitCount();
        long __mask;
        if (__kind.isUnsigned())
        {
            __mask = CodeUtil.mask(__kind.getBitCount());
        }
        else
        {
            __mask = CodeUtil.mask(__bits);
        }
        setCache(__kind, IntegerStamp.create(__bits, __kind.getMinValue(), __kind.getMaxValue(), 0, __mask));
    }

    private static void setFloatCache(JavaKind __kind)
    {
        setCache(__kind, new FloatStamp(__kind.getBitCount()));
    }

    static
    {
        setIntCache(JavaKind.Boolean);
        setIntCache(JavaKind.Byte);
        setIntCache(JavaKind.Short);
        setIntCache(JavaKind.Char);
        setIntCache(JavaKind.Int);
        setIntCache(JavaKind.Long);

        setFloatCache(JavaKind.Float);
        setFloatCache(JavaKind.Double);

        setCache(JavaKind.Object, objectStamp);
        setCache(JavaKind.Void, VoidStamp.getInstance());
        setCache(JavaKind.Illegal, IllegalStamp.getInstance());

        for (JavaKind __k : JavaKind.values())
        {
            if (stampCache[__k.ordinal()] != null)
            {
                emptyStampCache[__k.ordinal()] = stampCache[__k.ordinal()].empty();
            }
        }
    }

    public static Stamp tautology()
    {
        return booleanTrue;
    }

    public static Stamp contradiction()
    {
        return booleanFalse;
    }

    ///
    // Return a stamp for a Java kind, as it would be represented on the bytecode stack.
    ///
    public static Stamp forKind(JavaKind __kind)
    {
        return stampCache[__kind.ordinal()];
    }

    ///
    // Return the stamp for the {@code void} type. This will return a singleton instance than can be
    // compared using {@code ==}.
    ///
    public static Stamp forVoid()
    {
        return VoidStamp.getInstance();
    }

    public static Stamp intValue()
    {
        return forKind(JavaKind.Int);
    }

    public static Stamp positiveInt()
    {
        return positiveInt;
    }

    public static Stamp empty(JavaKind __kind)
    {
        return emptyStampCache[__kind.ordinal()];
    }

    public static IntegerStamp forInteger(JavaKind __kind, long __lowerBound, long __upperBound, long __downMask, long __upMask)
    {
        return IntegerStamp.create(__kind.getBitCount(), __lowerBound, __upperBound, __downMask, __upMask);
    }

    public static IntegerStamp forInteger(JavaKind __kind, long __lowerBound, long __upperBound)
    {
        return forInteger(__kind.getBitCount(), __lowerBound, __upperBound);
    }

    ///
    // Create a new stamp use {@code newLowerBound} and {@code newUpperBound} computing the
    // appropriate {@link IntegerStamp#upMask} and {@link IntegerStamp#downMask} and incorporating
    // any mask information from {@code maskStamp}.
    //
    // @return a new stamp with the appropriate bounds and masks
    ///
    public static IntegerStamp forIntegerWithMask(int __bits, long __newLowerBound, long __newUpperBound, IntegerStamp __maskStamp)
    {
        IntegerStamp __limit = StampFactory.forInteger(__bits, __newLowerBound, __newUpperBound);
        return IntegerStamp.create(__bits, __newLowerBound, __newUpperBound, __limit.downMask() | __maskStamp.downMask(), __limit.upMask() & __maskStamp.upMask());
    }

    public static IntegerStamp forIntegerWithMask(int __bits, long __newLowerBound, long __newUpperBound, long __newDownMask, long __newUpMask)
    {
        IntegerStamp __limit = StampFactory.forInteger(__bits, __newLowerBound, __newUpperBound);
        return IntegerStamp.create(__bits, __newLowerBound, __newUpperBound, __limit.downMask() | __newDownMask, __limit.upMask() & __newUpMask);
    }

    public static IntegerStamp forInteger(int __bits)
    {
        return IntegerStamp.create(__bits, CodeUtil.minValue(__bits), CodeUtil.maxValue(__bits), 0, CodeUtil.mask(__bits));
    }

    public static IntegerStamp forUnsignedInteger(int __bits)
    {
        return forUnsignedInteger(__bits, 0, NumUtil.maxValueUnsigned(__bits), 0, CodeUtil.mask(__bits));
    }

    public static IntegerStamp forUnsignedInteger(int __bits, long __unsignedLowerBound, long __unsignedUpperBound)
    {
        return forUnsignedInteger(__bits, __unsignedLowerBound, __unsignedUpperBound, 0, CodeUtil.mask(__bits));
    }

    public static IntegerStamp forUnsignedInteger(int __bits, long __unsignedLowerBound, long __unsignedUpperBound, long __downMask, long __upMask)
    {
        long __lowerBound = CodeUtil.signExtend(__unsignedLowerBound, __bits);
        long __upperBound = CodeUtil.signExtend(__unsignedUpperBound, __bits);
        if (!NumUtil.sameSign(__lowerBound, __upperBound))
        {
            __lowerBound = CodeUtil.minValue(__bits);
            __upperBound = CodeUtil.maxValue(__bits);
        }
        long __mask = CodeUtil.mask(__bits);
        return IntegerStamp.create(__bits, __lowerBound, __upperBound, __downMask & __mask, __upMask & __mask);
    }

    public static IntegerStamp forInteger(int __bits, long __lowerBound, long __upperBound)
    {
        return IntegerStamp.create(__bits, __lowerBound, __upperBound, 0, CodeUtil.mask(__bits));
    }

    public static FloatStamp forFloat(JavaKind __kind, double __lowerBound, double __upperBound, boolean __nonNaN)
    {
        return new FloatStamp(__kind.getBitCount(), __lowerBound, __upperBound, __nonNaN);
    }

    public static Stamp forConstant(JavaConstant __value)
    {
        JavaKind __kind = __value.getJavaKind();
        switch (__kind)
        {
            case Boolean:
            case Byte:
            case Char:
            case Short:
            case Int:
            case Long:
            {
                long __mask = __value.asLong() & CodeUtil.mask(__kind.getBitCount());
                return forInteger(__kind.getStackKind(), __value.asLong(), __value.asLong(), __mask, __mask);
            }
            case Float:
                return forFloat(__kind, __value.asFloat(), __value.asFloat(), !Float.isNaN(__value.asFloat()));
            case Double:
                return forFloat(__kind, __value.asDouble(), __value.asDouble(), !Double.isNaN(__value.asDouble()));
            case Illegal:
                return forKind(JavaKind.Illegal);
            case Object:
                if (__value.isNull())
                {
                    return alwaysNull();
                }
                else
                {
                    return objectNonNull();
                }
            default:
                throw new GraalError("unexpected kind: %s", __kind);
        }
    }

    public static Stamp forConstant(JavaConstant __value, MetaAccessProvider __metaAccess)
    {
        if (__value.getJavaKind() == JavaKind.Object)
        {
            ResolvedJavaType __type = __value.isNull() ? null : __metaAccess.lookupJavaType(__value);
            return new ObjectStamp(__type, __value.isNonNull(), __value.isNonNull(), __value.isNull());
        }
        else
        {
            return forConstant(__value);
        }
    }

    public static Stamp object()
    {
        return objectStamp;
    }

    public static Stamp objectNonNull()
    {
        return objectNonNullStamp;
    }

    public static Stamp alwaysNull()
    {
        return objectAlwaysNullStamp;
    }

    public static ObjectStamp object(TypeReference __type)
    {
        return object(__type, false);
    }

    public static ObjectStamp objectNonNull(TypeReference __type)
    {
        return object(__type, true);
    }

    public static ObjectStamp object(TypeReference __type, boolean __nonNull)
    {
        if (__type == null)
        {
            return new ObjectStamp(null, false, __nonNull, false);
        }
        else
        {
            return new ObjectStamp(__type.getType(), __type.isExact(), __nonNull, false);
        }
    }

    public static Stamp[] createParameterStamps(Assumptions __assumptions, ResolvedJavaMethod __method)
    {
        return createParameterStamps(__assumptions, __method, false);
    }

    public static Stamp[] createParameterStamps(Assumptions __assumptions, ResolvedJavaMethod __method, boolean __trustInterfaceTypes)
    {
        Signature __signature = __method.getSignature();
        Stamp[] __result = new Stamp[__signature.getParameterCount(__method.hasReceiver())];

        int __index = 0;
        ResolvedJavaType __accessingClass = __method.getDeclaringClass();
        if (__method.hasReceiver())
        {
            if (__trustInterfaceTypes)
            {
                __result[__index++] = StampFactory.objectNonNull(TypeReference.createTrusted(__assumptions, __accessingClass));
            }
            else
            {
                __result[__index++] = StampFactory.objectNonNull(TypeReference.create(__assumptions, __accessingClass));
            }
        }

        for (int __i = 0; __i < __signature.getParameterCount(false); __i++)
        {
            JavaType __type = __signature.getParameterType(__i, __accessingClass);
            JavaKind __kind = __type.getJavaKind();

            Stamp __stamp;
            if (__kind == JavaKind.Object && __type instanceof ResolvedJavaType)
            {
                if (__trustInterfaceTypes)
                {
                    __stamp = StampFactory.object(TypeReference.createTrusted(__assumptions, (ResolvedJavaType) __type));
                }
                else
                {
                    __stamp = StampFactory.object(TypeReference.create(__assumptions, (ResolvedJavaType) __type));
                }
            }
            else
            {
                __stamp = StampFactory.forKind(__kind);
            }
            __result[__index++] = __stamp;
        }

        return __result;
    }

    public static Stamp pointer()
    {
        return rawPointer;
    }

    public static StampPair forDeclaredType(Assumptions __assumptions, JavaType __returnType, boolean __nonNull)
    {
        if (__returnType.getJavaKind() == JavaKind.Object && __returnType instanceof ResolvedJavaType)
        {
            ResolvedJavaType __resolvedJavaType = (ResolvedJavaType) __returnType;
            TypeReference __reference = TypeReference.create(__assumptions, __resolvedJavaType);
            ResolvedJavaType __elementalType = __resolvedJavaType.getElementalType();
            if (__elementalType.isInterface())
            {
                TypeReference __uncheckedType;
                ResolvedJavaType __elementalImplementor = __elementalType.getSingleImplementor();
                if (__elementalImplementor != null && !__elementalType.equals(__elementalImplementor))
                {
                    ResolvedJavaType __implementor = __elementalImplementor;
                    ResolvedJavaType __t = __resolvedJavaType;
                    while (__t.isArray())
                    {
                        __implementor = __implementor.getArrayClass();
                        __t = __t.getComponentType();
                    }
                    __uncheckedType = TypeReference.createTrusted(__assumptions, __implementor);
                }
                else
                {
                    __uncheckedType = TypeReference.createTrusted(__assumptions, __resolvedJavaType);
                }
                return StampPair.create(StampFactory.object(__reference, __nonNull), StampFactory.object(__uncheckedType, __nonNull));
            }
            return StampPair.createSingle(StampFactory.object(__reference, __nonNull));
        }
        else
        {
            return StampPair.createSingle(StampFactory.forKind(__returnType.getJavaKind()));
        }
    }
}
