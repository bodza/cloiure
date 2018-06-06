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
        private final InvocationPlugins ___invocationPlugins;
        // @field
        private NodePlugin[] ___nodePlugins;
        // @field
        private ParameterPlugin[] ___parameterPlugins;
        // @field
        private TypePlugin[] ___typePlugins;
        // @field
        private InlineInvokePlugin[] ___inlineInvokePlugins;
        // @field
        private LoopExplosionPlugin ___loopExplosionPlugin;
        // @field
        private InvokeDynamicPlugin ___invokeDynamicPlugin;

        ///
        // Creates a copy of a given set of plugins. The {@link InvocationPlugins} in
        // {@code copyFrom} become the {@linkplain InvocationPlugins#getParent() default}
        // {@linkplain #getInvocationPlugins() invocation plugins} in this object.
        ///
        // @cons GraphBuilderConfiguration.Plugins
        public Plugins(GraphBuilderConfiguration.Plugins __copyFrom)
        {
            super();
            this.___invocationPlugins = new InvocationPlugins(__copyFrom.___invocationPlugins);
            this.___nodePlugins = __copyFrom.___nodePlugins;
            this.___parameterPlugins = __copyFrom.___parameterPlugins;
            this.___typePlugins = __copyFrom.___typePlugins;
            this.___inlineInvokePlugins = __copyFrom.___inlineInvokePlugins;
            this.___loopExplosionPlugin = __copyFrom.___loopExplosionPlugin;
            this.___invokeDynamicPlugin = __copyFrom.___invokeDynamicPlugin;
        }

        ///
        // Creates a new set of plugins.
        //
        // @param invocationPlugins the {@linkplain #getInvocationPlugins() invocation plugins} in
        //            this object
        ///
        // @cons GraphBuilderConfiguration.Plugins
        public Plugins(InvocationPlugins __invocationPlugins)
        {
            super();
            this.___invocationPlugins = __invocationPlugins;
            this.___nodePlugins = new NodePlugin[0];
            this.___parameterPlugins = new ParameterPlugin[0];
            this.___typePlugins = new TypePlugin[0];
            this.___inlineInvokePlugins = new InlineInvokePlugin[0];
        }

        public InvocationPlugins getInvocationPlugins()
        {
            return this.___invocationPlugins;
        }

        public NodePlugin[] getNodePlugins()
        {
            return this.___nodePlugins;
        }

        public void appendNodePlugin(NodePlugin __plugin)
        {
            this.___nodePlugins = Arrays.copyOf(this.___nodePlugins, this.___nodePlugins.length + 1);
            this.___nodePlugins[this.___nodePlugins.length - 1] = __plugin;
        }

        public void prependNodePlugin(NodePlugin __plugin)
        {
            NodePlugin[] __newPlugins = new NodePlugin[this.___nodePlugins.length + 1];
            System.arraycopy(this.___nodePlugins, 0, __newPlugins, 1, this.___nodePlugins.length);
            __newPlugins[0] = __plugin;
            this.___nodePlugins = __newPlugins;
        }

        public void clearNodePlugin()
        {
            this.___nodePlugins = new NodePlugin[0];
        }

        public ParameterPlugin[] getParameterPlugins()
        {
            return this.___parameterPlugins;
        }

        public void appendParameterPlugin(ParameterPlugin __plugin)
        {
            this.___parameterPlugins = Arrays.copyOf(this.___parameterPlugins, this.___parameterPlugins.length + 1);
            this.___parameterPlugins[this.___parameterPlugins.length - 1] = __plugin;
        }

        public void prependParameterPlugin(ParameterPlugin __plugin)
        {
            ParameterPlugin[] __newPlugins = new ParameterPlugin[this.___parameterPlugins.length + 1];
            System.arraycopy(this.___parameterPlugins, 0, __newPlugins, 1, this.___parameterPlugins.length);
            __newPlugins[0] = __plugin;
            this.___parameterPlugins = __newPlugins;
        }

        public TypePlugin[] getTypePlugins()
        {
            return this.___typePlugins;
        }

        public void appendTypePlugin(TypePlugin __plugin)
        {
            this.___typePlugins = Arrays.copyOf(this.___typePlugins, this.___typePlugins.length + 1);
            this.___typePlugins[this.___typePlugins.length - 1] = __plugin;
        }

        public void prependTypePlugin(TypePlugin __plugin)
        {
            TypePlugin[] __newPlugins = new TypePlugin[this.___typePlugins.length + 1];
            System.arraycopy(this.___typePlugins, 0, __newPlugins, 1, this.___typePlugins.length);
            __newPlugins[0] = __plugin;
            this.___typePlugins = __newPlugins;
        }

        public void clearParameterPlugin()
        {
            this.___parameterPlugins = new ParameterPlugin[0];
        }

        public InlineInvokePlugin[] getInlineInvokePlugins()
        {
            return this.___inlineInvokePlugins;
        }

        public void appendInlineInvokePlugin(InlineInvokePlugin __plugin)
        {
            this.___inlineInvokePlugins = Arrays.copyOf(this.___inlineInvokePlugins, this.___inlineInvokePlugins.length + 1);
            this.___inlineInvokePlugins[this.___inlineInvokePlugins.length - 1] = __plugin;
        }

        public void prependInlineInvokePlugin(InlineInvokePlugin __plugin)
        {
            InlineInvokePlugin[] __newPlugins = new InlineInvokePlugin[this.___inlineInvokePlugins.length + 1];
            System.arraycopy(this.___inlineInvokePlugins, 0, __newPlugins, 1, this.___inlineInvokePlugins.length);
            __newPlugins[0] = __plugin;
            this.___inlineInvokePlugins = __newPlugins;
        }

        public void clearInlineInvokePlugins()
        {
            this.___inlineInvokePlugins = new InlineInvokePlugin[0];
        }

        public LoopExplosionPlugin getLoopExplosionPlugin()
        {
            return this.___loopExplosionPlugin;
        }

        public void setLoopExplosionPlugin(LoopExplosionPlugin __plugin)
        {
            this.___loopExplosionPlugin = __plugin;
        }

        public InvokeDynamicPlugin getInvokeDynamicPlugin()
        {
            return this.___invokeDynamicPlugin;
        }

        public void setInvokeDynamicPlugin(InvokeDynamicPlugin __plugin)
        {
            this.___invokeDynamicPlugin = __plugin;
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
    private final boolean ___eagerResolving;
    // @field
    private final boolean ___unresolvedIsError;
    // @field
    private final GraphBuilderConfiguration.BytecodeExceptionMode ___bytecodeExceptionMode;
    // @field
    private final ResolvedJavaType[] ___skippedExceptionTypes;
    // @field
    private final GraphBuilderConfiguration.Plugins ___plugins;

    // @enum GraphBuilderConfiguration.BytecodeExceptionMode
    public enum BytecodeExceptionMode
    {
        ///
        // This mode always explicitly checks for exceptions.
        ///
        CheckAll,
        ///
        // This mode omits all explicit exception edges.
        ///
        OmitAll,
        ///
        // This mode omits exception edges at invokes, but not for implicit null checks or bounds checks.
        ///
        ExplicitOnly,
        ///
        // This mode uses profiling information to decide whether to use explicit exception edges.
        ///
        Profile
    }

    // @cons GraphBuilderConfiguration
    protected GraphBuilderConfiguration(boolean __eagerResolving, boolean __unresolvedIsError, GraphBuilderConfiguration.BytecodeExceptionMode __bytecodeExceptionMode, ResolvedJavaType[] __skippedExceptionTypes, GraphBuilderConfiguration.Plugins __plugins)
    {
        super();
        this.___eagerResolving = __eagerResolving;
        this.___unresolvedIsError = __unresolvedIsError;
        this.___bytecodeExceptionMode = __bytecodeExceptionMode;
        this.___skippedExceptionTypes = __skippedExceptionTypes;
        this.___plugins = __plugins;
    }

    ///
    // Creates a copy of this configuration with all its plugins. The {@link InvocationPlugins} in
    // this configuration become the {@linkplain InvocationPlugins#getParent() parent} of the
    // {@link InvocationPlugins} in the copy.
    ///
    public GraphBuilderConfiguration copy()
    {
        GraphBuilderConfiguration.Plugins __newPlugins = new GraphBuilderConfiguration.Plugins(this.___plugins);
        return new GraphBuilderConfiguration(this.___eagerResolving, this.___unresolvedIsError, this.___bytecodeExceptionMode, this.___skippedExceptionTypes, __newPlugins);
    }

    ///
    // Set the {@link #unresolvedIsError} flag. This flag can be set independently from
    // {@link #eagerResolving}, i.e., even if eager resolving fails execution is assumed to be
    // valid. This allows us for example to process unresolved types/methods/fields even when
    // eagerly resolving elements.
    ///
    public GraphBuilderConfiguration withUnresolvedIsError(boolean __newUnresolvedIsError)
    {
        return new GraphBuilderConfiguration(this.___eagerResolving, __newUnresolvedIsError, this.___bytecodeExceptionMode, this.___skippedExceptionTypes, this.___plugins);
    }

    public GraphBuilderConfiguration withEagerResolving(boolean __newEagerResolving)
    {
        return new GraphBuilderConfiguration(__newEagerResolving, this.___unresolvedIsError, this.___bytecodeExceptionMode, this.___skippedExceptionTypes, this.___plugins);
    }

    public GraphBuilderConfiguration withSkippedExceptionTypes(ResolvedJavaType[] __newSkippedExceptionTypes)
    {
        return new GraphBuilderConfiguration(this.___eagerResolving, this.___unresolvedIsError, this.___bytecodeExceptionMode, __newSkippedExceptionTypes, this.___plugins);
    }

    public GraphBuilderConfiguration withBytecodeExceptionMode(GraphBuilderConfiguration.BytecodeExceptionMode __newBytecodeExceptionMode)
    {
        return new GraphBuilderConfiguration(this.___eagerResolving, this.___unresolvedIsError, __newBytecodeExceptionMode, this.___skippedExceptionTypes, this.___plugins);
    }

    public ResolvedJavaType[] getSkippedExceptionTypes()
    {
        return this.___skippedExceptionTypes;
    }

    public boolean eagerResolving()
    {
        return this.___eagerResolving;
    }

    public GraphBuilderConfiguration.BytecodeExceptionMode getBytecodeExceptionMode()
    {
        return this.___bytecodeExceptionMode;
    }

    public static GraphBuilderConfiguration getDefault(GraphBuilderConfiguration.Plugins __plugins)
    {
        return new GraphBuilderConfiguration(false, false, GraphBuilderConfiguration.BytecodeExceptionMode.Profile, EMPTY, __plugins);
    }

    public static GraphBuilderConfiguration getSnippetDefault(GraphBuilderConfiguration.Plugins __plugins)
    {
        return new GraphBuilderConfiguration(true, true, GraphBuilderConfiguration.BytecodeExceptionMode.OmitAll, EMPTY, __plugins);
    }

    ///
    // Returns {@code true} if it is an error for a class/field/method resolution to fail.
    ///
    public boolean unresolvedIsError()
    {
        return this.___unresolvedIsError;
    }

    public GraphBuilderConfiguration.Plugins getPlugins()
    {
        return this.___plugins;
    }
}
