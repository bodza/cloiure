package giraaff.api.directives;

///
// Directives that influence the compilation of methods by Graal. They don't influence the semantics
// of the code, but they are useful for unit testing and benchmarking.
///
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

    ///
    // Directive for the compiler to fall back to the bytecode interpreter at this point.
    ///
    public static void deoptimize()
    {
    }

    ///
    // Directive for the compiler to fall back to the bytecode interpreter at this point, invalidate
    // the compiled code and reprofile the method.
    ///
    public static void deoptimizeAndInvalidate()
    {
    }

    ///
    // Directive for the compiler to fall back to the bytecode interpreter at this point, invalidate
    // the compiled code, record a speculation and reprofile the method.
    ///
    public static void deoptimizeAndInvalidateWithSpeculation()
    {
    }

    ///
    // Returns a boolean value indicating whether the method is executed in Graal-compiled code.
    ///
    public static boolean inCompiledCode()
    {
        return false;
    }

    ///
    // A call to this method will never be duplicated by control flow optimizations in the compiler.
    ///
    public static void controlFlowAnchor()
    {
    }

    ///
    // Consume a value, making sure the compiler doesn't optimize away the computation of this
    // value, even if it is otherwise unused.
    ///
    @SuppressWarnings("unused")
    public static void blackhole(boolean __value)
    {
    }

    ///
    // Consume a value, making sure the compiler doesn't optimize away the computation of this
    // value, even if it is otherwise unused.
    ///
    @SuppressWarnings("unused")
    public static void blackhole(byte __value)
    {
    }

    ///
    // Consume a value, making sure the compiler doesn't optimize away the computation of this
    // value, even if it is otherwise unused.
    ///
    @SuppressWarnings("unused")
    public static void blackhole(short __value)
    {
    }

    ///
    // Consume a value, making sure the compiler doesn't optimize away the computation of this
    // value, even if it is otherwise unused.
    ///
    @SuppressWarnings("unused")
    public static void blackhole(char __value)
    {
    }

    ///
    // Consume a value, making sure the compiler doesn't optimize away the computation of this
    // value, even if it is otherwise unused.
    ///
    @SuppressWarnings("unused")
    public static void blackhole(int __value)
    {
    }

    ///
    // Consume a value, making sure the compiler doesn't optimize away the computation of this
    // value, even if it is otherwise unused.
    ///
    @SuppressWarnings("unused")
    public static void blackhole(long __value)
    {
    }

    ///
    // Consume a value, making sure the compiler doesn't optimize away the computation of this
    // value, even if it is otherwise unused.
    ///
    @SuppressWarnings("unused")
    public static void blackhole(float __value)
    {
    }

    ///
    // Consume a value, making sure the compiler doesn't optimize away the computation of this
    // value, even if it is otherwise unused.
    ///
    @SuppressWarnings("unused")
    public static void blackhole(double __value)
    {
    }

    ///
    // Consume a value, making sure the compiler doesn't optimize away the computation of this
    // value, even if it is otherwise unused.
    ///
    @SuppressWarnings("unused")
    public static void blackhole(Object __value)
    {
    }

    ///
    // Forces a value to be kept in a register.
    ///
    @SuppressWarnings("unused")
    public static void bindToRegister(boolean __value)
    {
    }

    ///
    // Forces a value to be kept in a register.
    ///
    @SuppressWarnings("unused")
    public static void bindToRegister(byte __value)
    {
    }

    ///
    // Forces a value to be kept in a register.
    ///
    @SuppressWarnings("unused")
    public static void bindToRegister(short __value)
    {
    }

    ///
    // Forces a value to be kept in a register.
    ///
    @SuppressWarnings("unused")
    public static void bindToRegister(char __value)
    {
    }

    ///
    // Forces a value to be kept in a register.
    ///
    @SuppressWarnings("unused")
    public static void bindToRegister(int __value)
    {
    }

    ///
    // Forces a value to be kept in a register.
    ///
    @SuppressWarnings("unused")
    public static void bindToRegister(long __value)
    {
    }

    ///
    // Forces a value to be kept in a register.
    ///
    @SuppressWarnings("unused")
    public static void bindToRegister(float __value)
    {
    }

    ///
    // Forces a value to be kept in a register.
    ///
    @SuppressWarnings("unused")
    public static void bindToRegister(double __value)
    {
    }

    ///
    // Forces a value to be kept in a register.
    ///
    @SuppressWarnings("unused")
    public static void bindToRegister(Object __value)
    {
    }

    ///
    // Spills all caller saved registers.
    ///
    public static void spillRegisters()
    {
    }

    ///
    // Do nothing, but also make sure the compiler doesn't do any optimizations across this call.
    //
    // For example, the compiler will constant fold the expression 5 * 3, but the expression 5 *
    // opaque(3) will result in a real multiplication, because the compiler will not see that
    // opaque(3) is a constant.
    ///
    public static boolean opaque(boolean __value)
    {
        return __value;
    }

    ///
    // Do nothing, but also make sure the compiler doesn't do any optimizations across this call.
    //
    // For example, the compiler will constant fold the expression 5 * 3, but the expression 5 *
    // opaque(3) will result in a real multiplication, because the compiler will not see that
    // opaque(3) is a constant.
    ///
    public static byte opaque(byte __value)
    {
        return __value;
    }

    ///
    // Do nothing, but also make sure the compiler doesn't do any optimizations across this call.
    //
    // For example, the compiler will constant fold the expression 5 * 3, but the expression 5 *
    // opaque(3) will result in a real multiplication, because the compiler will not see that
    // opaque(3) is a constant.
    ///
    public static short opaque(short __value)
    {
        return __value;
    }

    ///
    // Do nothing, but also make sure the compiler doesn't do any optimizations across this call.
    //
    // For example, the compiler will constant fold the expression 5 * 3, but the expression 5 *
    // opaque(3) will result in a real multiplication, because the compiler will not see that
    // opaque(3) is a constant.
    ///
    public static char opaque(char __value)
    {
        return __value;
    }

    ///
    // Do nothing, but also make sure the compiler doesn't do any optimizations across this call.
    //
    // For example, the compiler will constant fold the expression 5 * 3, but the expression 5 *
    // opaque(3) will result in a real multiplication, because the compiler will not see that
    // opaque(3) is a constant.
    ///
    public static int opaque(int __value)
    {
        return __value;
    }

    ///
    // Do nothing, but also make sure the compiler doesn't do any optimizations across this call.
    //
    // For example, the compiler will constant fold the expression 5 * 3, but the expression 5 *
    // opaque(3) will result in a real multiplication, because the compiler will not see that
    // opaque(3) is a constant.
    ///
    public static long opaque(long __value)
    {
        return __value;
    }

    ///
    // Do nothing, but also make sure the compiler doesn't do any optimizations across this call.
    //
    // For example, the compiler will constant fold the expression 5 * 3, but the expression 5 *
    // opaque(3) will result in a real multiplication, because the compiler will not see that
    // opaque(3) is a constant.
    ///
    public static float opaque(float __value)
    {
        return __value;
    }

    ///
    // Do nothing, but also make sure the compiler doesn't do any optimizations across this call.
    //
    // For example, the compiler will constant fold the expression 5 * 3, but the expression 5 *
    // opaque(3) will result in a real multiplication, because the compiler will not see that
    // opaque(3) is a constant.
    ///
    public static double opaque(double __value)
    {
        return __value;
    }

    ///
    // Do nothing, but also make sure the compiler doesn't do any optimizations across this call.
    //
    // For example, the compiler will constant fold the expression 5 * 3, but the expression 5 *
    // opaque(3) will result in a real multiplication, because the compiler will not see that
    // opaque(3) is a constant.
    ///
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

    ///
    // Ensures that the given object will be virtual (escape analyzed) at all points that are
    // dominated by the current position.
    ///
    public static void ensureVirtualized(@SuppressWarnings("unused") Object __object)
    {
    }

    ///
    // Ensures that the given object will be virtual at the current position.
    ///
    public static void ensureVirtualizedHere(@SuppressWarnings("unused") Object __object)
    {
    }
}
