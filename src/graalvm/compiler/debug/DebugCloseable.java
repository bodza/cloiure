package graalvm.compiler.debug;

/**
 * An {@link AutoCloseable} whose {@link #close()} does not throw a checked exception.
 */
public interface DebugCloseable extends AutoCloseable
{
    @Override
    void close();
}
