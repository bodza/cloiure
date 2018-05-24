package giraaff.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.ServiceLoader;

import jdk.vm.ci.services.JVMCIPermission;
import jdk.vm.ci.services.Services;

/**
 * Interface to functionality that abstracts over which JDK version Graal is running on.
 */
public final class GraalServices
{
    private GraalServices()
    {
    }

    /**
     * Gets an {@link Iterable} of the providers available for a given service.
     *
     * @throws SecurityException if on JDK8 and a security manager is present and it denies
     *             {@link JVMCIPermission}
     */
    public static <S> Iterable<S> load(Class<S> service)
    {
        Iterable<S> iterable = ServiceLoader.load(service);
        return new Iterable<>()
        {
            @Override
            public Iterator<S> iterator()
            {
                Iterator<S> iterator = iterable.iterator();
                return new Iterator<>()
                {
                    @Override
                    public boolean hasNext()
                    {
                        return iterator.hasNext();
                    }

                    @Override
                    public S next()
                    {
                        S provider = iterator.next();
                        // Allow Graal extensions to access JVMCI
                        openJVMCITo(provider.getClass());
                        return provider;
                    }

                    @Override
                    public void remove()
                    {
                        iterator.remove();
                    }
                };
            }
        };
    }

    private static final Module JVMCI_MODULE = Services.class.getModule();

    /**
     * Opens all JVMCI packages to the module of a given class. This relies on JVMCI already having
     * opened all its packages to the module defining {@link GraalServices}.
     *
     * @param other all JVMCI packages will be opened to the module defining this class
     */
    static void openJVMCITo(Class<?> other)
    {
        Module jvmciModule = JVMCI_MODULE;
        Module otherModule = other.getModule();
        if (jvmciModule != otherModule)
        {
            for (String pkg : jvmciModule.getPackages())
            {
                if (!jvmciModule.isOpen(pkg, otherModule))
                {
                    jvmciModule.addOpens(pkg, otherModule);
                }
            }
        }
    }

    /**
     * Gets the class file bytes for {@code c}.
     */
    public static InputStream getClassfileAsStream(Class<?> c) throws IOException
    {
        return c.getModule().getResourceAsStream(c.getName().replace('.', '/') + ".class");
    }
}
