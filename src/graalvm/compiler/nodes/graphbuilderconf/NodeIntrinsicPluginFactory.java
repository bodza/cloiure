package graalvm.compiler.nodes.graphbuilderconf;

import graalvm.compiler.core.common.type.Stamp;

public interface NodeIntrinsicPluginFactory
{
    public interface InjectionProvider
    {
        <T> T getInjectedArgument(Class<T> type);

        /**
         * Gets a stamp denoting a given type and non-nullness property.
         *
         * @param type the type the returned stamp represents
         * @param nonNull specifies if the returned stamp denotes a value that is guaranteed to be
         *            non-null
         */
        Stamp getInjectedStamp(Class<?> type, boolean nonNull);
    }

    void registerPlugins(InvocationPlugins plugins, InjectionProvider injection);
}
