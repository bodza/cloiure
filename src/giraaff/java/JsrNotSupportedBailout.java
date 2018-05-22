package giraaff.java;

import giraaff.core.common.PermanentBailoutException;

public class JsrNotSupportedBailout extends PermanentBailoutException
{
    public JsrNotSupportedBailout(String reason)
    {
        super(reason);
    }
}
