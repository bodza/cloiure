package graalvm.compiler.nodes.calc;

/**
 * Marker interface for nodes where it is valid to apply a {@link NarrowNode} to its inputs and do a
 * narrow operation instead of doing the wide operation and applying the {@link NarrowNode} to the
 * result.
 */
public interface NarrowableArithmeticNode
{
    /**
     * Check whether this operation can be narrowed to {@code resultBits} bit without loss of
     * precision.
     *
     * @param resultBits
     */
    default boolean isNarrowable(int resultBits)
    {
        return true;
    }
}
