package giraaff.core.common;

import jdk.vm.ci.code.BailoutException;

// @class RetryableBailoutException
public final class RetryableBailoutException extends BailoutException
{
    // @cons
    public RetryableBailoutException(String format, Object... args)
    {
        super(false, format, args);
    }

    // @cons
    public RetryableBailoutException(String reason)
    {
        super(false, reason);
    }

    // @cons
    public RetryableBailoutException(Throwable cause, String format, Object... args)
    {
        super(cause, format, args);
    }
}
