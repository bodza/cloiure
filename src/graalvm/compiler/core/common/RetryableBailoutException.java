package graalvm.compiler.core.common;

import jdk.vm.ci.code.BailoutException;

public class RetryableBailoutException extends BailoutException {

    private static final long serialVersionUID = -7145365025679144525L;

    public RetryableBailoutException(String format, Object... args) {
        super(false, format, args);
    }

    public RetryableBailoutException(String reason) {
        super(false, reason);
    }

    public RetryableBailoutException(Throwable cause, String format, Object... args) {
        super(cause, format, args);
    }

}
