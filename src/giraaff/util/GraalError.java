package giraaff.util;

/**
 * Indicates a condition that should never occur during normal operation.
 */
// @class GraalError
public class GraalError extends Error
{
    public static RuntimeException unimplemented()
    {
        throw new GraalError("unimplemented");
    }

    public static RuntimeException unimplemented(String __msg)
    {
        throw new GraalError("unimplemented: %s", __msg);
    }

    public static RuntimeException shouldNotReachHere()
    {
        throw new GraalError("should not reach here");
    }

    public static RuntimeException shouldNotReachHere(String __msg)
    {
        throw new GraalError("should not reach here: %s", __msg);
    }

    public static RuntimeException shouldNotReachHere(Throwable __cause)
    {
        throw new GraalError(__cause);
    }

    /**
     * Checks a given condition and throws a {@link GraalError} if it is false. Guarantees are
     * stronger than assertions in that they are always checked. Error messages for guarantee
     * violations should clearly indicate the nature of the problem as well as a suggested solution
     * if possible.
     *
     * @param condition the condition to check
     * @param msg the message that will be associated with the error
     */
    public static void guarantee(boolean __condition, String __msg)
    {
        if (!__condition)
        {
            throw new GraalError("failed guarantee: " + __msg);
        }
    }

    /**
     * Checks a given condition and throws a {@link GraalError} if it is false. Guarantees are
     * stronger than assertions in that they are always checked. Error messages for guarantee
     * violations should clearly indicate the nature of the problem as well as a suggested solution
     * if possible.
     *
     * @param condition the condition to check
     * @param msg the message that will be associated with the error, in {@link String#format(String, Object...)} syntax
     * @param arg argument to the format string in {@code msg}
     */
    public static void guarantee(boolean __condition, String __msg, Object __arg)
    {
        if (!__condition)
        {
            throw new GraalError("failed guarantee: " + __msg, __arg);
        }
    }

    /**
     * Checks a given condition and throws a {@link GraalError} if it is false. Guarantees are
     * stronger than assertions in that they are always checked. Error messages for guarantee
     * violations should clearly indicate the nature of the problem as well as a suggested solution
     * if possible.
     *
     * @param condition the condition to check
     * @param msg the message that will be associated with the error, in {@link String#format(String, Object...)} syntax
     * @param arg1 argument to the format string in {@code msg}
     * @param arg2 argument to the format string in {@code msg}
     */
    public static void guarantee(boolean __condition, String __msg, Object __arg1, Object __arg2)
    {
        if (!__condition)
        {
            throw new GraalError("failed guarantee: " + __msg, __arg1, __arg2);
        }
    }

    /**
     * Checks a given condition and throws a {@link GraalError} if it is false. Guarantees are
     * stronger than assertions in that they are always checked. Error messages for guarantee
     * violations should clearly indicate the nature of the problem as well as a suggested solution
     * if possible.
     *
     * @param condition the condition to check
     * @param msg the message that will be associated with the error, in {@link String#format(String, Object...)} syntax
     * @param arg1 argument to the format string in {@code msg}
     * @param arg2 argument to the format string in {@code msg}
     * @param arg3 argument to the format string in {@code msg}
     */
    public static void guarantee(boolean __condition, String __msg, Object __arg1, Object __arg2, Object __arg3)
    {
        if (!__condition)
        {
            throw new GraalError("failed guarantee: " + __msg, __arg1, __arg2, __arg3);
        }
    }

    /**
     * This override exists to catch cases when {@link #guarantee(boolean, String, Object)} is
     * called with one argument bound to a varargs method parameter. It will bind to this method
     * instead of the single arg variant and produce a deprecation warning instead of silently
     * wrapping the Object[] inside of another Object[].
     */
    @Deprecated
    public static void guarantee(boolean __condition, String __msg, Object... __args)
    {
        if (!__condition)
        {
            throw new GraalError("failed guarantee: " + __msg, __args);
        }
    }

    /**
     * This constructor creates a {@link GraalError} with a given message.
     *
     * @param msg the message that will be associated with the error
     */
    // @cons
    public GraalError(String __msg)
    {
        super(__msg);
    }

    /**
     * This constructor creates a {@link GraalError} with a message assembled via
     * {@link String#format(String, Object...)}.
     *
     * @param msg the message that will be associated with the error, in String.format syntax
     * @param args parameters to String.format
     */
    // @cons
    public GraalError(String __msg, Object... __args)
    {
        super(String.format(__msg, __args));
    }

    /**
     * This constructor creates a {@link GraalError} for a given causing Throwable instance.
     *
     * @param cause the original exception that contains additional information on this error
     */
    // @cons
    public GraalError(Throwable __cause)
    {
        super(__cause);
    }
}
