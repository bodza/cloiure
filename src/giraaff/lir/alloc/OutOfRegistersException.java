package giraaff.lir.alloc;

import giraaff.core.common.PermanentBailoutException;

/**
 * Thrown if the register allocator runs out of registers. This should never happen in normal mode.
 */
public final class OutOfRegistersException extends PermanentBailoutException
{
    private final String description;

    public OutOfRegistersException(String msg)
    {
        super(msg);
        this.description = "";
    }

    public OutOfRegistersException(Throwable cause, String msg)
    {
        super(cause, msg);
        this.description = "";
    }

    public OutOfRegistersException(String msg, String description)
    {
        super(msg);
        this.description = description;
    }

    public OutOfRegistersException(Throwable cause, String msg, String description)
    {
        super(cause, msg);
        this.description = description;
    }

    public String getDescription()
    {
        return description;
    }
}