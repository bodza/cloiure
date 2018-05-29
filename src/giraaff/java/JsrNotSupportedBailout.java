package giraaff.java;

import giraaff.core.common.PermanentBailoutException;

// @class JsrNotSupportedBailout
public final class JsrNotSupportedBailout extends PermanentBailoutException
{
    // @cons
    public JsrNotSupportedBailout(String reason)
    {
        super(reason);
    }
}
