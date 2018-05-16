package graalvm.compiler.core;

/**
 * A compiler thread is a daemon thread that runs at {@link Thread#MAX_PRIORITY}.
 */
public class CompilerThread extends Thread
{
    public CompilerThread(Runnable r, String namePrefix)
    {
        super(r);
        this.setName(namePrefix + "-" + this.getId());
        this.setPriority(Thread.MAX_PRIORITY);
        this.setDaemon(true);
    }

    @Override
    public void run()
    {
        setContextClassLoader(getClass().getClassLoader());
        super.run();
    }
}
