package graalvm.compiler.lir;

import graalvm.compiler.core.common.PermanentBailoutException;
import graalvm.compiler.options.OptionKey;

/**
 * Restarts the {@link LIR low-level} compilation with a modified configuration.
 * {@link BailoutAndRestartBackendException.Options#LIRUnlockBackendRestart LIRUnlockBackendRestart}
 * needs to be enabled. Use only for debugging purposes only.
 */
public abstract class BailoutAndRestartBackendException extends PermanentBailoutException
{
    public static class Options
    {
        // "Unlock backend restart feature."
        public static final OptionKey<Boolean> LIRUnlockBackendRestart = new OptionKey<>(false);
    }

    public BailoutAndRestartBackendException(String msg)
    {
        super(msg);
    }

    public BailoutAndRestartBackendException(Throwable cause, String msg)
    {
        super(cause, msg);
    }
}
