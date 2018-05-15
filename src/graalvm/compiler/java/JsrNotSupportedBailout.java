package graalvm.compiler.java;

import graalvm.compiler.core.common.PermanentBailoutException;

public class JsrNotSupportedBailout extends PermanentBailoutException {

    private static final long serialVersionUID = -7476925652727154272L;

    public JsrNotSupportedBailout(String reason) {
        super(reason);
    }
}
