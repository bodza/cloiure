package giraaff.nodes.type;

import java.util.Iterator;

import jdk.vm.ci.code.CodeUtil;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.type.AbstractObjectStamp;
import giraaff.core.common.type.AbstractPointerStamp;
import giraaff.core.common.type.IntegerStamp;
import giraaff.core.common.type.Stamp;
import giraaff.core.common.type.StampFactory;
import giraaff.core.common.type.TypeReference;
import giraaff.nodes.NodeView;
import giraaff.nodes.ValueNode;

/**
 * Helper class that is used to keep all stamp-related operations in one place.
 */
// @class StampTool
public final class StampTool
{
    // @cons
    private StampTool()
    {
        super();
    }

    public static Stamp meet(Iterable<? extends ValueNode> __values)
    {
        Stamp __stamp = meetOrNull(__values, null);
        if (__stamp == null)
        {
            return StampFactory.forVoid();
        }
        return __stamp;
    }

    /**
     * Meet a collection of {@link ValueNode}s optionally excluding {@code selfValue}. If no values
     * are encountered then return {@code null}.
     */
    public static Stamp meetOrNull(Iterable<? extends ValueNode> __values, ValueNode __selfValue)
    {
        Iterator<? extends ValueNode> __iterator = __values.iterator();
        Stamp __stamp = null;
        while (__iterator.hasNext())
        {
            ValueNode __nextValue = __iterator.next();
            if (__nextValue != __selfValue)
            {
                if (__stamp == null)
                {
                    __stamp = __nextValue.stamp(NodeView.DEFAULT);
                }
                else
                {
                    __stamp = __stamp.meet(__nextValue.stamp(NodeView.DEFAULT));
                }
            }
        }
        return __stamp;
    }

    /**
     * Compute the stamp resulting from the unsigned comparison being true.
     *
     * @return null if it's can't be true or it nothing useful can be encoded.
     */
    public static Stamp unsignedCompare(Stamp __stamp, Stamp __stamp2)
    {
        IntegerStamp __x = (IntegerStamp) __stamp;
        IntegerStamp __y = (IntegerStamp) __stamp2;
        if (__x.isUnrestricted() && __y.isUnrestricted())
        {
            // Don't know anything.
            return null;
        }
        // c <| n, where c is a constant and n is known to be positive.
        if (__x.lowerBound() == __x.upperBound())
        {
            if (__y.isPositive())
            {
                if (__x.lowerBound() == (1 << __x.getBits()) - 1)
                {
                    // Constant is MAX_VALUE which must fail.
                    return null;
                }
                if (__x.lowerBound() <= __y.lowerBound())
                {
                    // Test will fail. Return illegalStamp instead?
                    return null;
                }
                // If the test succeeds then this proves that n is at greater than c so the bounds
                // are [c+1..-n.upperBound)].
                return StampFactory.forInteger(__x.getBits(), __x.lowerBound() + 1, __y.upperBound());
            }
            return null;
        }
        // n <| c, where c is a strictly positive constant
        if (__y.lowerBound() == __y.upperBound() && __y.isStrictlyPositive())
        {
            // the test proves that n is positive and less than c, [0..c-1]
            return StampFactory.forInteger(__y.getBits(), 0, __y.lowerBound() - 1);
        }
        return null;
    }

    public static Stamp stampForLeadingZeros(IntegerStamp __valueStamp)
    {
        long __mask = CodeUtil.mask(__valueStamp.getBits());
        // Don't count zeros from the mask in the result.
        int __adjust = Long.numberOfLeadingZeros(__mask);
        int __min = Long.numberOfLeadingZeros(__valueStamp.upMask() & __mask) - __adjust;
        int __max = Long.numberOfLeadingZeros(__valueStamp.downMask() & __mask) - __adjust;
        return StampFactory.forInteger(JavaKind.Int, __min, __max);
    }

    public static Stamp stampForTrailingZeros(IntegerStamp __valueStamp)
    {
        long __mask = CodeUtil.mask(__valueStamp.getBits());
        int __min = Long.numberOfTrailingZeros(__valueStamp.upMask() & __mask);
        int __max = Long.numberOfTrailingZeros(__valueStamp.downMask() & __mask);
        return StampFactory.forInteger(JavaKind.Int, __min, __max);
    }

    /**
     * Checks whether this {@link ValueNode} represents a {@linkplain Stamp#hasValues() legal}
     * pointer value which is known to be always null.
     *
     * @param node the node to check
     * @return true if this node represents a legal object value which is known to be always null
     */
    public static boolean isPointerAlwaysNull(ValueNode __node)
    {
        return isPointerAlwaysNull(__node.stamp(NodeView.DEFAULT));
    }

    /**
     * Checks whether this {@link Stamp} represents a {@linkplain Stamp#hasValues() legal} pointer
     * stamp whose values are known to be always null.
     *
     * @param stamp the stamp to check
     * @return true if this stamp represents a legal object stamp whose values are known to be
     *         always null
     */
    public static boolean isPointerAlwaysNull(Stamp __stamp)
    {
        if (__stamp instanceof AbstractPointerStamp && __stamp.hasValues())
        {
            return ((AbstractPointerStamp) __stamp).alwaysNull();
        }
        return false;
    }

    /**
     * Checks whether this {@link ValueNode} represents a {@linkplain Stamp#hasValues() legal}
     * pointer value which is known to never be null.
     *
     * @param node the node to check
     * @return true if this node represents a legal object value which is known to never be null
     */
    public static boolean isPointerNonNull(ValueNode __node)
    {
        return isPointerNonNull(__node.stamp(NodeView.DEFAULT));
    }

    /**
     * Checks whether this {@link Stamp} represents a {@linkplain Stamp#hasValues() legal} pointer
     * stamp whose values are known to never be null.
     *
     * @param stamp the stamp to check
     * @return true if this stamp represents a legal object stamp whose values are known to be
     *         always null
     */
    public static boolean isPointerNonNull(Stamp __stamp)
    {
        if (__stamp instanceof AbstractPointerStamp)
        {
            return ((AbstractPointerStamp) __stamp).nonNull();
        }
        return false;
    }

    /**
     * Returns the {@linkplain ResolvedJavaType Java type} this {@linkplain ValueNode} has if it is
     * a {@linkplain Stamp#hasValues() legal} Object value.
     *
     * @param node the node to check
     * @return the Java type this value has if it is a legal Object type, null otherwise
     */
    public static TypeReference typeReferenceOrNull(ValueNode __node)
    {
        return typeReferenceOrNull(__node.stamp(NodeView.DEFAULT));
    }

    public static ResolvedJavaType typeOrNull(ValueNode __node)
    {
        return typeOrNull(__node.stamp(NodeView.DEFAULT));
    }

    public static ResolvedJavaType typeOrNull(Stamp __stamp)
    {
        TypeReference __type = typeReferenceOrNull(__stamp);
        return __type == null ? null : __type.getType();
    }

    public static ResolvedJavaType typeOrNull(Stamp __stamp, MetaAccessProvider __metaAccess)
    {
        if (__stamp instanceof AbstractObjectStamp && __stamp.hasValues())
        {
            AbstractObjectStamp __abstractObjectStamp = (AbstractObjectStamp) __stamp;
            ResolvedJavaType __type = __abstractObjectStamp.type();
            if (__type == null)
            {
                return __metaAccess.lookupJavaType(Object.class);
            }
            else
            {
                return __type;
            }
        }
        return null;
    }

    public static ResolvedJavaType typeOrNull(ValueNode __node, MetaAccessProvider __metaAccess)
    {
        return typeOrNull(__node.stamp(NodeView.DEFAULT), __metaAccess);
    }

    /**
     * Returns the {@linkplain ResolvedJavaType Java type} this {@linkplain Stamp} has if it is a
     * {@linkplain Stamp#hasValues() legal} Object stamp.
     *
     * @param stamp the stamp to check
     * @return the Java type this stamp has if it is a legal Object stamp, null otherwise
     */
    public static TypeReference typeReferenceOrNull(Stamp __stamp)
    {
        if (__stamp instanceof AbstractObjectStamp && __stamp.hasValues())
        {
            AbstractObjectStamp __abstractObjectStamp = (AbstractObjectStamp) __stamp;
            if (__abstractObjectStamp.isExactType())
            {
                return TypeReference.createExactTrusted(__abstractObjectStamp.type());
            }
            else
            {
                return TypeReference.createTrustedWithoutAssumptions(__abstractObjectStamp.type());
            }
        }
        return null;
    }

    /**
     * Checks whether this {@link ValueNode} represents a {@linkplain Stamp#hasValues() legal}
     * Object value whose Java type is known exactly. If this method returns true then the
     * {@linkplain ResolvedJavaType Java type} returned by {@link #typeReferenceOrNull(ValueNode)}
     * is the concrete dynamic/runtime Java type of this value.
     *
     * @param node the node to check
     * @return true if this node represents a legal object value whose Java type is known exactly
     */
    public static boolean isExactType(ValueNode __node)
    {
        return isExactType(__node.stamp(NodeView.DEFAULT));
    }

    /**
     * Checks whether this {@link Stamp} represents a {@linkplain Stamp#hasValues() legal} Object
     * stamp whose {@linkplain ResolvedJavaType Java type} is known exactly. If this method returns
     * true then the Java type returned by {@link #typeReferenceOrNull(Stamp)} is the only concrete
     * dynamic/runtime Java type possible for values of this stamp.
     *
     * @param stamp the stamp to check
     * @return true if this node represents a legal object stamp whose Java type is known exactly
     */
    public static boolean isExactType(Stamp __stamp)
    {
        if (__stamp instanceof AbstractObjectStamp && __stamp.hasValues())
        {
            return ((AbstractObjectStamp) __stamp).isExactType();
        }
        return false;
    }
}
