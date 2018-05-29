package giraaff.code;

import giraaff.core.common.PermanentBailoutException;

/**
 * Represents a bailout exception with a stack trace in terms of the Java source being compiled
 * instead of the stack trace of the compiler. The exception of the compiler is saved as the cause
 * of this exception.
 */
// @class SourceStackTraceBailoutException
public abstract class SourceStackTraceBailoutException extends PermanentBailoutException
{
    public static SourceStackTraceBailoutException create(Throwable cause, String reason, StackTraceElement[] elements)
    {
        return new SourceStackTraceBailoutException(cause, reason)
        {
            @Override
            public synchronized Throwable fillInStackTrace()
            {
                setStackTrace(elements);
                return this;
            }
        };
    }

    // @cons
    private SourceStackTraceBailoutException(Throwable cause, String reason)
    {
        super(cause, "%s", reason);
    }
}
