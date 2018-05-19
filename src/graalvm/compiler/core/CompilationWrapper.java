package graalvm.compiler.core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

import graalvm.compiler.debug.TTY;
import graalvm.compiler.options.EnumOptionKey;
import graalvm.compiler.options.OptionValues;

import jdk.vm.ci.code.BailoutException;

/**
 * Wrapper for a compilation that centralizes what action to take when an uncaught exception occurs during compilation.
 */
public abstract class CompilationWrapper<T>
{
    public CompilationWrapper()
    {
    }

    /**
     * Handles an uncaught exception.
     *
     * @param t an exception thrown during {@link #run()}
     * @return a value representing the result of a failed compilation (may be {@code null})
     */
    protected abstract T handleException(Throwable t);

    /**
     * Perform the compilation wrapped by this object.
     */
    protected abstract T performCompilation();

    /**
     * Gets a value that represents the input to the compilation.
     */
    @Override
    public abstract String toString();

    public final T run()
    {
        try
        {
            return performCompilation();
        }
        catch (Throwable cause)
        {
            return handleException(cause);
        }
    }
}
