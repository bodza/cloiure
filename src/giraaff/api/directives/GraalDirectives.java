package giraaff.api.directives;

/**
 * Directives that influence the compilation of methods by Graal. They don't influence the semantics
 * of the code, but they are useful for unit testing and benchmarking.
 */
// @class GraalDirectives
public final class GraalDirectives
{
    // @def
    public static final double LIKELY_PROBABILITY = 0.75;
    // @def
    public static final double UNLIKELY_PROBABILITY = 1.0 - LIKELY_PROBABILITY;

    // @def
    public static final double SLOWPATH_PROBABILITY = 0.0001;
    // @def
    public static final double FASTPATH_PROBABILITY = 1.0 - SLOWPATH_PROBABILITY;

    /**
     * Directive for the compiler to fall back to the bytecode interpreter at this point.
     */
    public static void deoptimize()
    {
    }

    /**
     * Directive for the compiler to fall back to the bytecode interpreter at this point, invalidate
     * the compiled code and reprofile the method.
     */
    public static void deoptimizeAndInvalidate()
    {
    }

    /**
     * Directive for the compiler to fall back to the bytecode interpreter at this point, invalidate
     * the compiled code, record a speculation and reprofile the method.
     */
    public static void deoptimizeAndInvalidateWithSpeculation()
    {
    }

    /**
     * Returns a boolean value indicating whether the method is executed in Graal-compiled code.
     */
    public static boolean inCompiledCode()
    {
        return false;
    }

    /**
     * A call to this method will never be duplicated by control flow optimizations in the compiler.
     */
    public static void controlFlowAnchor()
    {
    }

    /**
     * Injects a probability for the given condition into the profiling information of a branch
     * instruction. The probability must be a value between 0.0 and 1.0 (inclusive).
     *
     * Example usage (it specifies that the likelihood for a to be greater than b is 90%):
     *
     * <code>
     * if (injectBranchProbability(0.9, a &gt; b)) {
     *    // ...
     * }
     * </code>
     *
     * There are predefined constants for commonly used probabilities (see
     * {@link #LIKELY_PROBABILITY} , {@link #UNLIKELY_PROBABILITY}, {@link #SLOWPATH_PROBABILITY},
     * {@link #FASTPATH_PROBABILITY} ).
     *
     * @param probability the probability value between 0.0 and 1.0 that should be injected
     */
    public static boolean injectBranchProbability(double __probability, boolean __condition)
    {
        return __condition;
    }

    /**
     * Injects an average iteration count of a loop into the probability information of a loop exit
     * condition. The iteration count specifies how often the condition is checked, i.e. in for and
     * while loops it is one more than the body iteration count, and in do-while loops it is equal
     * to the body iteration count. The iteration count must be >= 1.0.
     *
     * Example usage (it specifies that the expected iteration count of the loop condition is 500,
     * so the iteration count of the loop body is 499):
     *
     * <code>
     * for (int i = 0; injectIterationCount(500, i < array.length); i++) {
     *     // ...
     * }
     * </code>
     *
     * @param iterations the expected number of iterations that should be injected
     */
    public static boolean injectIterationCount(double __iterations, boolean __condition)
    {
        return injectBranchProbability(1. - 1. / __iterations, __condition);
    }

    /**
     * Consume a value, making sure the compiler doesn't optimize away the computation of this
     * value, even if it is otherwise unused.
     */
    @SuppressWarnings("unused")
    public static void blackhole(boolean __value)
    {
    }

    /**
     * Consume a value, making sure the compiler doesn't optimize away the computation of this
     * value, even if it is otherwise unused.
     */
    @SuppressWarnings("unused")
    public static void blackhole(byte __value)
    {
    }

    /**
     * Consume a value, making sure the compiler doesn't optimize away the computation of this
     * value, even if it is otherwise unused.
     */
    @SuppressWarnings("unused")
    public static void blackhole(short __value)
    {
    }

    /**
     * Consume a value, making sure the compiler doesn't optimize away the computation of this
     * value, even if it is otherwise unused.
     */
    @SuppressWarnings("unused")
    public static void blackhole(char __value)
    {
    }

    /**
     * Consume a value, making sure the compiler doesn't optimize away the computation of this
     * value, even if it is otherwise unused.
     */
    @SuppressWarnings("unused")
    public static void blackhole(int __value)
    {
    }

    /**
     * Consume a value, making sure the compiler doesn't optimize away the computation of this
     * value, even if it is otherwise unused.
     */
    @SuppressWarnings("unused")
    public static void blackhole(long __value)
    {
    }

    /**
     * Consume a value, making sure the compiler doesn't optimize away the computation of this
     * value, even if it is otherwise unused.
     */
    @SuppressWarnings("unused")
    public static void blackhole(float __value)
    {
    }

    /**
     * Consume a value, making sure the compiler doesn't optimize away the computation of this
     * value, even if it is otherwise unused.
     */
    @SuppressWarnings("unused")
    public static void blackhole(double __value)
    {
    }

    /**
     * Consume a value, making sure the compiler doesn't optimize away the computation of this
     * value, even if it is otherwise unused.
     */
    @SuppressWarnings("unused")
    public static void blackhole(Object __value)
    {
    }

    /**
     * Forces a value to be kept in a register.
     */
    @SuppressWarnings("unused")
    public static void bindToRegister(boolean __value)
    {
    }

    /**
     * Forces a value to be kept in a register.
     */
    @SuppressWarnings("unused")
    public static void bindToRegister(byte __value)
    {
    }

    /**
     * Forces a value to be kept in a register.
     */
    @SuppressWarnings("unused")
    public static void bindToRegister(short __value)
    {
    }

    /**
     * Forces a value to be kept in a register.
     */
    @SuppressWarnings("unused")
    public static void bindToRegister(char __value)
    {
    }

    /**
     * Forces a value to be kept in a register.
     */
    @SuppressWarnings("unused")
    public static void bindToRegister(int __value)
    {
    }

    /**
     * Forces a value to be kept in a register.
     */
    @SuppressWarnings("unused")
    public static void bindToRegister(long __value)
    {
    }

    /**
     * Forces a value to be kept in a register.
     */
    @SuppressWarnings("unused")
    public static void bindToRegister(float __value)
    {
    }

    /**
     * Forces a value to be kept in a register.
     */
    @SuppressWarnings("unused")
    public static void bindToRegister(double __value)
    {
    }

    /**
     * Forces a value to be kept in a register.
     */
    @SuppressWarnings("unused")
    public static void bindToRegister(Object __value)
    {
    }

    /**
     * Spills all caller saved registers.
     */
    public static void spillRegisters()
    {
    }

    /**
     * Do nothing, but also make sure the compiler doesn't do any optimizations across this call.
     *
     * For example, the compiler will constant fold the expression 5 * 3, but the expression 5 *
     * opaque(3) will result in a real multiplication, because the compiler will not see that
     * opaque(3) is a constant.
     */
    public static boolean opaque(boolean __value)
    {
        return __value;
    }

    /**
     * Do nothing, but also make sure the compiler doesn't do any optimizations across this call.
     *
     * For example, the compiler will constant fold the expression 5 * 3, but the expression 5 *
     * opaque(3) will result in a real multiplication, because the compiler will not see that
     * opaque(3) is a constant.
     */
    public static byte opaque(byte __value)
    {
        return __value;
    }

    /**
     * Do nothing, but also make sure the compiler doesn't do any optimizations across this call.
     *
     * For example, the compiler will constant fold the expression 5 * 3, but the expression 5 *
     * opaque(3) will result in a real multiplication, because the compiler will not see that
     * opaque(3) is a constant.
     */
    public static short opaque(short __value)
    {
        return __value;
    }

    /**
     * Do nothing, but also make sure the compiler doesn't do any optimizations across this call.
     *
     * For example, the compiler will constant fold the expression 5 * 3, but the expression 5 *
     * opaque(3) will result in a real multiplication, because the compiler will not see that
     * opaque(3) is a constant.
     */
    public static char opaque(char __value)
    {
        return __value;
    }

    /**
     * Do nothing, but also make sure the compiler doesn't do any optimizations across this call.
     *
     * For example, the compiler will constant fold the expression 5 * 3, but the expression 5 *
     * opaque(3) will result in a real multiplication, because the compiler will not see that
     * opaque(3) is a constant.
     */
    public static int opaque(int __value)
    {
        return __value;
    }

    /**
     * Do nothing, but also make sure the compiler doesn't do any optimizations across this call.
     *
     * For example, the compiler will constant fold the expression 5 * 3, but the expression 5 *
     * opaque(3) will result in a real multiplication, because the compiler will not see that
     * opaque(3) is a constant.
     */
    public static long opaque(long __value)
    {
        return __value;
    }

    /**
     * Do nothing, but also make sure the compiler doesn't do any optimizations across this call.
     *
     * For example, the compiler will constant fold the expression 5 * 3, but the expression 5 *
     * opaque(3) will result in a real multiplication, because the compiler will not see that
     * opaque(3) is a constant.
     */
    public static float opaque(float __value)
    {
        return __value;
    }

    /**
     * Do nothing, but also make sure the compiler doesn't do any optimizations across this call.
     *
     * For example, the compiler will constant fold the expression 5 * 3, but the expression 5 *
     * opaque(3) will result in a real multiplication, because the compiler will not see that
     * opaque(3) is a constant.
     */
    public static double opaque(double __value)
    {
        return __value;
    }

    /**
     * Do nothing, but also make sure the compiler doesn't do any optimizations across this call.
     *
     * For example, the compiler will constant fold the expression 5 * 3, but the expression 5 *
     * opaque(3) will result in a real multiplication, because the compiler will not see that
     * opaque(3) is a constant.
     */
    public static <T> T opaque(T __value)
    {
        return __value;
    }

    public static <T> T guardingNonNull(T __value)
    {
        if (__value == null)
        {
            deoptimize();
        }
        return __value;
    }

    /**
     * Ensures that the given object will be virtual (escape analyzed) at all points that are
     * dominated by the current position.
     */
    public static void ensureVirtualized(@SuppressWarnings("unused") Object __object)
    {
    }

    /**
     * Ensures that the given object will be virtual at the current position.
     */
    public static void ensureVirtualizedHere(@SuppressWarnings("unused") Object __object)
    {
    }
}
