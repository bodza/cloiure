package graalvm.compiler.core.common;

/**
 * A {@linkplain RetryableBailoutException} that will be thrown if an on-going compilation in the
 * compiler was cancelled.
 */
public final class CancellationBailoutException extends RetryableBailoutException
{
    public CancellationBailoutException()
    {
        super("Compilation cancelled.");
    }

    public static void cancelCompilation()
    {
        throw new CancellationBailoutException();
    }
}
