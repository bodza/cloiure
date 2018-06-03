package giraaff.nodes.graphbuilderconf;

import java.util.Arrays;

import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.ResolvedJavaType;

import giraaff.core.common.type.StampPair;

// @class GraphBuilderConfiguration
public final class GraphBuilderConfiguration
{
    // @class GraphBuilderConfiguration.Plugins
    public static final class Plugins
    {
        // @field
        private final InvocationPlugins invocationPlugins;
        // @field
        private NodePlugin[] nodePlugins;
        // @field
        private ParameterPlugin[] parameterPlugins;
        // @field
        private TypePlugin[] typePlugins;
        // @field
        private InlineInvokePlugin[] inlineInvokePlugins;
        // @field
        private LoopExplosionPlugin loopExplosionPlugin;
        // @field
        private InvokeDynamicPlugin invokeDynamicPlugin;

        /**
         * Creates a copy of a given set of plugins. The {@link InvocationPlugins} in
         * {@code copyFrom} become the {@linkplain InvocationPlugins#getParent() default}
         * {@linkplain #getInvocationPlugins() invocation plugins} in this object.
         */
        // @cons
        public Plugins(Plugins __copyFrom)
        {
            super();
            this.invocationPlugins = new InvocationPlugins(__copyFrom.invocationPlugins);
            this.nodePlugins = __copyFrom.nodePlugins;
            this.parameterPlugins = __copyFrom.parameterPlugins;
            this.typePlugins = __copyFrom.typePlugins;
            this.inlineInvokePlugins = __copyFrom.inlineInvokePlugins;
            this.loopExplosionPlugin = __copyFrom.loopExplosionPlugin;
            this.invokeDynamicPlugin = __copyFrom.invokeDynamicPlugin;
        }

        /**
         * Creates a new set of plugins.
         *
         * @param invocationPlugins the {@linkplain #getInvocationPlugins() invocation plugins} in
         *            this object
         */
        // @cons
        public Plugins(InvocationPlugins __invocationPlugins)
        {
            super();
            this.invocationPlugins = __invocationPlugins;
            this.nodePlugins = new NodePlugin[0];
            this.parameterPlugins = new ParameterPlugin[0];
            this.typePlugins = new TypePlugin[0];
            this.inlineInvokePlugins = new InlineInvokePlugin[0];
        }

        public InvocationPlugins getInvocationPlugins()
        {
            return invocationPlugins;
        }

        public NodePlugin[] getNodePlugins()
        {
            return nodePlugins;
        }

        public void appendNodePlugin(NodePlugin __plugin)
        {
            nodePlugins = Arrays.copyOf(nodePlugins, nodePlugins.length + 1);
            nodePlugins[nodePlugins.length - 1] = __plugin;
        }

        public void prependNodePlugin(NodePlugin __plugin)
        {
            NodePlugin[] __newPlugins = new NodePlugin[nodePlugins.length + 1];
            System.arraycopy(nodePlugins, 0, __newPlugins, 1, nodePlugins.length);
            __newPlugins[0] = __plugin;
            nodePlugins = __newPlugins;
        }

        public void clearNodePlugin()
        {
            nodePlugins = new NodePlugin[0];
        }

        public ParameterPlugin[] getParameterPlugins()
        {
            return parameterPlugins;
        }

        public void appendParameterPlugin(ParameterPlugin __plugin)
        {
            parameterPlugins = Arrays.copyOf(parameterPlugins, parameterPlugins.length + 1);
            parameterPlugins[parameterPlugins.length - 1] = __plugin;
        }

        public void prependParameterPlugin(ParameterPlugin __plugin)
        {
            ParameterPlugin[] __newPlugins = new ParameterPlugin[parameterPlugins.length + 1];
            System.arraycopy(parameterPlugins, 0, __newPlugins, 1, parameterPlugins.length);
            __newPlugins[0] = __plugin;
            parameterPlugins = __newPlugins;
        }

        public TypePlugin[] getTypePlugins()
        {
            return typePlugins;
        }

        public void appendTypePlugin(TypePlugin __plugin)
        {
            typePlugins = Arrays.copyOf(typePlugins, typePlugins.length + 1);
            typePlugins[typePlugins.length - 1] = __plugin;
        }

        public void prependTypePlugin(TypePlugin __plugin)
        {
            TypePlugin[] __newPlugins = new TypePlugin[typePlugins.length + 1];
            System.arraycopy(typePlugins, 0, __newPlugins, 1, typePlugins.length);
            __newPlugins[0] = __plugin;
            typePlugins = __newPlugins;
        }

        public void clearParameterPlugin()
        {
            parameterPlugins = new ParameterPlugin[0];
        }

        public InlineInvokePlugin[] getInlineInvokePlugins()
        {
            return inlineInvokePlugins;
        }

        public void appendInlineInvokePlugin(InlineInvokePlugin __plugin)
        {
            inlineInvokePlugins = Arrays.copyOf(inlineInvokePlugins, inlineInvokePlugins.length + 1);
            inlineInvokePlugins[inlineInvokePlugins.length - 1] = __plugin;
        }

        public void prependInlineInvokePlugin(InlineInvokePlugin __plugin)
        {
            InlineInvokePlugin[] __newPlugins = new InlineInvokePlugin[inlineInvokePlugins.length + 1];
            System.arraycopy(inlineInvokePlugins, 0, __newPlugins, 1, inlineInvokePlugins.length);
            __newPlugins[0] = __plugin;
            inlineInvokePlugins = __newPlugins;
        }

        public void clearInlineInvokePlugins()
        {
            inlineInvokePlugins = new InlineInvokePlugin[0];
        }

        public LoopExplosionPlugin getLoopExplosionPlugin()
        {
            return loopExplosionPlugin;
        }

        public void setLoopExplosionPlugin(LoopExplosionPlugin __plugin)
        {
            this.loopExplosionPlugin = __plugin;
        }

        public InvokeDynamicPlugin getInvokeDynamicPlugin()
        {
            return invokeDynamicPlugin;
        }

        public void setInvokeDynamicPlugin(InvokeDynamicPlugin __plugin)
        {
            this.invokeDynamicPlugin = __plugin;
        }

        public StampPair getOverridingStamp(GraphBuilderTool __b, JavaType __type, boolean __nonNull)
        {
            for (TypePlugin __plugin : getTypePlugins())
            {
                StampPair __stamp = __plugin.interceptType(__b, __type, __nonNull);
                if (__stamp != null)
                {
                    return __stamp;
                }
            }
            return null;
        }
    }

    // @def
    private static final ResolvedJavaType[] EMPTY = new ResolvedJavaType[] {};

    // @field
    private final boolean eagerResolving;
    // @field
    private final boolean unresolvedIsError;
    // @field
    private final BytecodeExceptionMode bytecodeExceptionMode;
    // @field
    private final ResolvedJavaType[] skippedExceptionTypes;
    // @field
    private final Plugins plugins;

    // @enum GraphBuilderConfiguration.BytecodeExceptionMode
    public enum BytecodeExceptionMode
    {
        /**
         * This mode always explicitly checks for exceptions.
         */
        CheckAll,
        /**
         * This mode omits all explicit exception edges.
         */
        OmitAll,
        /**
         * This mode omits exception edges at invokes, but not for implicit null checks or bounds checks.
         */
        ExplicitOnly,
        /**
         * This mode uses profiling information to decide whether to use explicit exception edges.
         */
        Profile
    }

    // @cons
    protected GraphBuilderConfiguration(boolean __eagerResolving, boolean __unresolvedIsError, BytecodeExceptionMode __bytecodeExceptionMode, ResolvedJavaType[] __skippedExceptionTypes, Plugins __plugins)
    {
        super();
        this.eagerResolving = __eagerResolving;
        this.unresolvedIsError = __unresolvedIsError;
        this.bytecodeExceptionMode = __bytecodeExceptionMode;
        this.skippedExceptionTypes = __skippedExceptionTypes;
        this.plugins = __plugins;
    }

    /**
     * Creates a copy of this configuration with all its plugins. The {@link InvocationPlugins} in
     * this configuration become the {@linkplain InvocationPlugins#getParent() parent} of the
     * {@link InvocationPlugins} in the copy.
     */
    public GraphBuilderConfiguration copy()
    {
        Plugins __newPlugins = new Plugins(plugins);
        return new GraphBuilderConfiguration(eagerResolving, unresolvedIsError, bytecodeExceptionMode, skippedExceptionTypes, __newPlugins);
    }

    /**
     * Set the {@link #unresolvedIsError} flag. This flag can be set independently from
     * {@link #eagerResolving}, i.e., even if eager resolving fails execution is assumed to be
     * valid. This allows us for example to process unresolved types/methods/fields even when
     * eagerly resolving elements.
     */
    public GraphBuilderConfiguration withUnresolvedIsError(boolean __newUnresolvedIsError)
    {
        return new GraphBuilderConfiguration(eagerResolving, __newUnresolvedIsError, bytecodeExceptionMode, skippedExceptionTypes, plugins);
    }

    public GraphBuilderConfiguration withEagerResolving(boolean __newEagerResolving)
    {
        return new GraphBuilderConfiguration(__newEagerResolving, unresolvedIsError, bytecodeExceptionMode, skippedExceptionTypes, plugins);
    }

    public GraphBuilderConfiguration withSkippedExceptionTypes(ResolvedJavaType[] __newSkippedExceptionTypes)
    {
        return new GraphBuilderConfiguration(eagerResolving, unresolvedIsError, bytecodeExceptionMode, __newSkippedExceptionTypes, plugins);
    }

    public GraphBuilderConfiguration withBytecodeExceptionMode(BytecodeExceptionMode __newBytecodeExceptionMode)
    {
        return new GraphBuilderConfiguration(eagerResolving, unresolvedIsError, __newBytecodeExceptionMode, skippedExceptionTypes, plugins);
    }

    public ResolvedJavaType[] getSkippedExceptionTypes()
    {
        return skippedExceptionTypes;
    }

    public boolean eagerResolving()
    {
        return eagerResolving;
    }

    public BytecodeExceptionMode getBytecodeExceptionMode()
    {
        return bytecodeExceptionMode;
    }

    public static GraphBuilderConfiguration getDefault(Plugins __plugins)
    {
        return new GraphBuilderConfiguration(false, false, BytecodeExceptionMode.Profile, EMPTY, __plugins);
    }

    public static GraphBuilderConfiguration getSnippetDefault(Plugins __plugins)
    {
        return new GraphBuilderConfiguration(true, true, BytecodeExceptionMode.OmitAll, EMPTY, __plugins);
    }

    /**
     * Returns {@code true} if it is an error for a class/field/method resolution to fail.
     */
    public boolean unresolvedIsError()
    {
        return unresolvedIsError;
    }

    public Plugins getPlugins()
    {
        return plugins;
    }
}
