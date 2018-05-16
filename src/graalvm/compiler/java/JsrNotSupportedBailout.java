package graalvm.compiler.java;

import graalvm.compiler.core.common.PermanentBailoutException;

public class JsrNotSupportedBailout extends PermanentBailoutException
{
    public JsrNotSupportedBailout(String reason)
    {
        super(reason);
    }
}
