package graalvm.compiler.api.runtime;

import jdk.vm.ci.common.JVMCIError;

public interface GraalRuntime
{
    String getName();

    <T> T getCapability(Class<T> clazz);

    default <T> T getRequiredCapability(Class<T> clazz)
    {
        T ret = getCapability(clazz);
        if (ret == null)
        {
            throw new JVMCIError("The VM does not expose the required Graal capability %s.", clazz.getName());
        }
        return ret;
    }
}
