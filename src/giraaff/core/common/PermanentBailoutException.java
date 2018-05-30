package giraaff.core.common;

import jdk.vm.ci.code.BailoutException;

// @class PermanentBailoutException
public final class PermanentBailoutException extends BailoutException
{
    // @cons
    public PermanentBailoutException(String format, Object... args)
    {
        super(true, format, args);
    }

    // @cons
    public PermanentBailoutException(String reason)
    {
        super(true, "%s", reason);
    }

    // @cons
    public PermanentBailoutException(Throwable cause, String format, Object... args)
    {
        super(cause, format, args);
    }
}
