package giraaff.nodes.calc;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.ConstantReflectionProvider;

import giraaff.core.common.calc.CanonicalCondition;
import giraaff.nodes.ValueNode;
import giraaff.nodes.ValueNodeInterface;

/**
 * Represents a conversion between primitive types.
 */
// @iface ConvertNode
public interface ConvertNode extends ValueNodeInterface
{
    ValueNode getValue();

    Constant convert(Constant c, ConstantReflectionProvider constantReflection);

    Constant reverse(Constant c, ConstantReflectionProvider constantReflection);

    /**
     * Checks whether a null check may skip the conversion. This is true if in the conversion NULL
     * is converted to NULL and if it is the only value converted to NULL.
     *
     * @return whether a null check may skip the conversion
     */
    boolean mayNullCheckSkipConversion();

    /**
     * Check whether a conversion is lossless.
     *
     * @return true iff reverse(convert(c)) == c for all c
     */
    boolean isLossless();

    /**
     * Check whether a conversion preserves comparison order.
     *
     * @param op a comparison operator
     * @return true iff (c1 op c2) == (convert(c1) op convert(c2)) for all c1, c2
     */
    default boolean preservesOrder(CanonicalCondition op)
    {
        return isLossless();
    }

    /**
     * Check whether a conversion preserves comparison order against a particular constant value.
     *
     * @param op a comparison operator
     * @return true iff (c1 op value) == (convert(c1) op convert(value)) for value and all c1
     */
    default boolean preservesOrder(CanonicalCondition op, Constant value, ConstantReflectionProvider constantReflection)
    {
        return preservesOrder(op);
    }
}
