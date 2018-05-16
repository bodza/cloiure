package graalvm.compiler.debug;

/**
 * An {@link AutoCloseable} whose {@link #close()} does not throw a checked exception.
 */
public interface DebugCloseable extends AutoCloseable
{
    DebugCloseable VOID_CLOSEABLE = new DebugCloseable()
    {
        @Override
        public void close()
        {
        }
    };

    /**
     * Gets the debug context associated with this object.
     */
    default DebugContext getDebug()
    {
        return null;
    }

    @Override
    void close();
}
