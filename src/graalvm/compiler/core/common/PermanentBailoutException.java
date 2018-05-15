package graalvm.compiler.core.common;

import jdk.vm.ci.code.BailoutException;

public class PermanentBailoutException extends BailoutException {

    private static final long serialVersionUID = -2683649650135362549L;

    public PermanentBailoutException(String format, Object... args) {
        super(true, format, args);
    }

    public PermanentBailoutException(String reason) {
        super(true, "%s", reason);
    }

    public PermanentBailoutException(Throwable cause, String format, Object... args) {
        super(cause, format, args);
    }
}
